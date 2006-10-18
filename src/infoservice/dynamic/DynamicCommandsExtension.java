/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice.dynamic;

import infoservice.HttpResponseStructure;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Random;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class provides the functionality needed by the "dynamic cascade
 * extension". It is essentially an extension to
 * <code>InfoServiceCommands</code>
 * 
 * @author LERNGRUPPE
 */
public class DynamicCommandsExtension
{
    /**
     * This method is called, when we receive data from a mixcascade (first mix)
     * or when we receive data from a remote infoservice, which posts data about
     * a mixcascade.
     * 
     * @param a_postData
     *            The data we have received.
     * 
     * @return The HTTP response for the client.
     */
    public HttpResponseStructure cascadePostHelo(byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_OK);
        try
        {
            LogHolder.log(LogLevel.DEBUG, LogType.NET, "MixCascade HELO received: XML: "
                    + (new String(a_postData)));
            Element mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil
                    .toXMLDocument(a_postData), MixCascade.XML_ELEMENT_NAME));
            /* verify the signature */
            /*
             * LERNGRUPPE: Signature is valid if digest can be confirmed and ID
             * equals subjectKeyIdentifier of the certificate
             */
            MixCascade mixCascadeEntry = new MixCascade(mixCascadeNode);
            if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode,
                    SignatureVerifier.DOCUMENT_CLASS_MIX))
            {
                // remove temporary cascades if existig
                MixCascade temporaryCascade = getTemporaryCascade(mixCascadeEntry.getId());
                if (temporaryCascade != null)
                {
                    Database.getInstance(TemporaryCascade.class).remove(temporaryCascade);
                }
                Database.getInstance(MixCascade.class).update(mixCascadeEntry);
            } else
            {
                LogHolder.log(LogLevel.WARNING, LogType.NET,
                        "Signature check failed for MixCascade entry! XML: "
                                + (new String(a_postData)));
                httpResponse = new HttpResponseStructure(
                        HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.NET, e);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
        }
        return httpResponse;
    }

    /**
     * Checks if there exists new cascade information for the mix with the given
     * ID.
     * 
     * @param a_strMixId
     * @return <code>HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR</code>
     *         if no new information is available,
     *         <code>HttpResponseStructure.HTTP_RETURN_OK</code> otherwise
     */
    public HttpResponseStructure isNewCascadeAvailable(String a_strMixId)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_NOT_FOUND);

        if (!haveNewCascadeInformation(a_strMixId))
        {
            return httpResponse;
        }
        httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
        return httpResponse;
    }

    public HttpResponseStructure reconfigureMix(String a_strMixId)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
        MixCascade cascade = getTemporaryCascade(a_strMixId);
        if (cascade != null)
        {
            Element doc = cascade.getXmlStructure();
            SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
                    doc);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
                    HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(doc));
        }
        return httpResponse;
    }

    /**
     * Tests if there exists new cascade information for the mix with the given
     * ID. New information should be available if 1) No current cascade is found
     * and a temporary cascade is available 2) Current cascade is not equal to a
     * temporary cascade
     * 
     * @param a_strMixId
     *            The ID of the mix in question
     * @return <code>true</code> if a new cascade is available,
     *         <code>false</code> otherwise
     */
    private boolean haveNewCascadeInformation(String a_strMixId)
    {
        MixCascade assignedCascade = getCurrentCascade(a_strMixId);
        MixCascade assignedTemporaryCascade = getTemporaryCascade(a_strMixId);
        // No new information
        if (assignedTemporaryCascade == null)
            return false;

        // No new information
        if (assignedTemporaryCascade.compareMixIDs(assignedCascade))
            return false;

        // In all other cases the assignedTemporaryCascade should be newer ->
        // new information
        return true;
    }

    /**
     * Returns a temporary cascade for the mix with the given ID. Not the
     * TemporaryCascade-object, but the real MixCascade is returned
     * 
     * @param a_mixId
     *            The ID of the mix in question
     * @return A temporary <code>MixCascade</code> for the mix or
     *         <code>null</code>
     */
    private MixCascade getTemporaryCascade(String a_mixId)
    {
        Enumeration knownTemporaryMixCascades = Database.getInstance(TemporaryCascade.class)
                .getEntryList().elements();
        MixCascade assignedTemporaryCascade = null;
        while (knownTemporaryMixCascades.hasMoreElements() && (assignedTemporaryCascade == null))
        {
            MixCascade currentCascade = ((TemporaryCascade) (knownTemporaryMixCascades
                    .nextElement())).getRealCascade();
            if (currentCascade.getMixIds().contains(a_mixId))
            {
                /* the mix is assigned to that cascade */
                assignedTemporaryCascade = currentCascade;
                break;
            }
        }
        return assignedTemporaryCascade;
    }

    /**
     * Returns the current cascade for the mix with the given ID.
     * 
     * @param a_mixId
     *            The ID of the mix in question
     * @return The current <code>MixCascade</code> for the mix or
     *         <code>null</code>
     */
    private MixCascade getCurrentCascade(String a_mixId)
    {
        /* check whether the mix is already assigned to a mixcascade */
        Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList()
                .elements();
        MixCascade assignedCascade = null;
        while (knownMixCascades.hasMoreElements() && (assignedCascade == null))
        {
            MixCascade currentCascade = (MixCascade) (knownMixCascades.nextElement());
            if (currentCascade.getMixIds().contains(a_mixId))
            {
                /* the mix is assigned to that cascade */
                assignedCascade = currentCascade;
                break;
            }
        }
        return assignedCascade;
    }

    /**
     * This method gets called when a last mix posts its cascade information to
     * the InfoService Such a cascade is not yet established, so it is a
     * temporary cascade an will be treated as such
     * 
     * @param a_postData
     *            The data of the POST request
     * @return <code>HttpResponseStructure</code> HTTP_RETURN_OK (no payload)
     *         or HTTP_RETURN_INTERNAL_SERVER_ERROR
     */
    public HttpResponseStructure lastMixPostDynaCascade(byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_OK);
        try
        {
            LogHolder.log(LogLevel.DEBUG, LogType.NET, "MixCascade HELO received: XML: "
                    + (new String(a_postData)));
            Element mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil
                    .toXMLDocument(a_postData), MixCascade.XML_ELEMENT_NAME));
            /* verify the signature */
            if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode,
                    SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
            {
                MixCascade mixCascadeEntry = new MixCascade(mixCascadeNode);
                TemporaryCascade tmp = new TemporaryCascade(mixCascadeEntry);
                Database.getInstance(TemporaryCascade.class).update(tmp);
            } else
            {
                LogHolder.log(LogLevel.WARNING, LogType.NET,
                        "Signature check failed for MixCascade entry! XML: "
                                + (new String(a_postData)));
                httpResponse = new HttpResponseStructure(
                        HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.NET, e);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
        }
        return httpResponse;

    }

    /**
     * This method gets called when a mix asks the InfoService to verify its
     * connectivity. Connectivity verification works as follows
     * <ul>
     * <li>Mix sends /connectivity-request as HTTP-Post containing a XML
     * structure with the port to be probed</li>
     * <li>The InfoService opens a socket to the source address of the request
     * to the requested port</li>
     * <li>It then sends a random number as echo request (cp. ICMP echo
     * request) to the mix</li>
     * <li>The mix sends the payload (i.e. random number back over the socket</li>
     * <li>InfoService responds with "OK" if it got the random number back,
     * "Failed" otherwise</li>
     * </ul>
     * 
     * @param a_sourceAddress
     *            The source address of the request
     * @param a_postData
     *            The POST data containing a XML structure with the port
     * @return <code>HttpResponseStructure</code> HTTP_RETURN_OK (containing
     *         the answer XML structure) or HTTP_RETURN_INTERNAL_SERVER_ERROR
     */
    public HttpResponseStructure mixPostConnectivityTest(InetAddress a_sourceAddress,
            byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
        int port = extractPort(a_postData);
        LogHolder.log(LogLevel.EMERG, LogType.MISC, "Port: " + port);
        if (port == -1)
        {
            LogHolder.log(LogLevel.DEBUG, LogType.MISC, "connectivityTest: No Port given");
            return httpResponse;
        }

        Document docConnectivity = null;
        if (isReachable(a_sourceAddress, port))
        {
            docConnectivity = constructAnswer("OK");
        } else
        {
            docConnectivity = constructAnswer("Failed");
        }
        httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
                HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(docConnectivity));
        return httpResponse;
    }

    /**
     * Constructs the answer for a connectivity request.
     * 
     * @param response
     *            Either "True" or "False"
     * @return The XML Document containing the answer
     */
    private Document constructAnswer(String response)
    {
        Document result = XMLUtil.createDocument();
        Node nodeConnectivity = result.createElement("Connectivity");
        Node nodeResult = result.createElement("Result");
        result.appendChild(nodeConnectivity);
        nodeConnectivity.appendChild(nodeResult);
        XMLUtil.setValue(nodeResult, response);
        return result;
    }

    /**
     * Checks if the given address is reachable on the given port
     * 
     * @param a_Address
     *            The address to be tested
     * @param port
     *            The port to be tested
     * @return <code>true</code> if the address is reachable on the given
     *         port, <code>false</code> otherwise
     */
    private boolean isReachable(InetAddress a_Address, int port)
    {
        // Construct the challenge to send
        Random rand = new Random();
        long echoRequest = Math.abs(rand.nextLong());

        LogHolder.log(LogLevel.EMERG, LogType.ALL, "Echo request is: " + echoRequest);
        String result = doPing(a_Address, port, echoRequest);
        if (result == null)
            return false;

        long echoResponse = Long.parseLong(result);
        return (echoResponse == echoRequest);
    }

    /**
     * Actually executes the ping-like connectivity-test. Sends a echoRequest to
     * the querying mix and waits for the same token to come back from the mix
     * 
     * @param a_Address
     *            The target address
     * @param port
     *            The target port
     * @param echoRequest
     *            The echoRequest to send
     * @return The echoResponse from the mix
     */
    private String doPing(InetAddress a_Address, int port, long echoRequest)
    {
        StringBuffer result = new StringBuffer();
        try
        {
            Socket socket = new Socket(a_Address, port);
            socket.setSoTimeout(5000);
            BufferedOutputStream str = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            byte[] content = Long.toString(echoRequest).getBytes();

            // Send the echo request
            str.write(content);
            str.flush();

            // Read the response from the mix
            byte[] buff = new byte[content.length];
            int bytes = -1;
            while ((bytes = in.read(buff)) != -1)
            {
                byte[] real = new byte[bytes];
                System.arraycopy(buff, 0, real, 0, bytes);
                result.append(new String(real));
            }

            in.close();
            socket.close();
        } catch (Exception e)
        {
            return null;
        }
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "Answer from Mix was:  " + result);
        return result.toString();
    }

    /**
     * Extracts the port from the POST data. Creates a XML-Document and returns
     * the value of the <code>Port</code> element.
     * 
     * @param a_postData
     *            The POST data to parse
     * @return The port or -1 if there was an error
     */
    private int extractPort(byte[] a_postData)
    {
        Document doc;
        int port = -1;
        try
        {
            doc = XMLUtil.toXMLDocument(a_postData);
            Node rootNode = doc.getFirstChild();
            Element portNode = (Element) XMLUtil.getFirstChildByName(rootNode, "Port");
            port = XMLUtil.parseValue(portNode, -1);
        } catch (XMLParseException e)
        {
            // we can ignore this
        }
        return port;
    }

}

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
package infoservice;

import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureVerifier;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.JAPMinVersion;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.StatusInfo;
import anon.util.XMLUtil;
import infoservice.japforwarding.JapForwardingTools;
import infoservice.tor.TorDirectoryAgent;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.PaymentInstanceDBEntry;
import infoservice.tor.MixminionDirectoryAgent;

/**
 * This is the implementation of all commands the InfoService supports.
 */
public class InfoServiceCommands implements JWSInternalCommands
{

	/**
	 * This method is called, when we receive data directly from a infoservice or when we receive
	 * data from a remote infoservice, which posts data about another infoservice.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure infoServerPostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Infoserver received: XML: " + (new String(a_postData)));
			Element infoServiceNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				InfoServiceDBEntry.getXmlElementName()));
			/* verify the signature --> if requested */
			if (!Configuration.getInstance().isInfoServiceMessageSignatureCheckEnabled() ||
				SignatureVerifier.getInstance().verifyXml(infoServiceNode,
				SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) == true)
			{
				InfoServiceDBEntry newEntry = new InfoServiceDBEntry(infoServiceNode, false);
				Database.getInstance(InfoServiceDBEntry.class).update(newEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for infoservice entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a payment instance or when we receive
	 * data from a remote infoservice, which posts data about another payment instance.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure paymentInstancePostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Infoserver received: XML: " + (new String(a_postData)));
			Element paymentInstanceNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(
				a_postData),
				PaymentInstanceDBEntry.getXmlElementName()));
			/* verify the signature --> if requested */
			// no signature check at the monent...
			//	if (!Configuration.getInstance().isInfoServiceMessageSignatureCheckEnabled() ||
			//	SignatureVerifier.getInstance().verifyXml(infoServiceNode,
			//SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) == true)
			{
				PaymentInstanceDBEntry newEntry = new PaymentInstanceDBEntry(paymentInstanceNode);
				Database.getInstance(PaymentInstanceDBEntry.class).update(newEntry);
			}
			/*			else
			   {
			 LogHolder.log(LogLevel.WARNING, LogType.NET,
			   "Signature check failed for infoservice entry! XML: " + (new String(a_postData)));
			 httpResponse = new HttpResponseStructure(HttpResponseStructure.
			  HTTP_RETURN_INTERNAL_SERVER_ERROR);
			   }*/
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the complete list of all known infoservices to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchInfoServers()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			Document doc = XMLUtil.createDocument();
			/* create the InfoServices element */
			Element infoServicesNode = doc.createElement("InfoServices");
			/* append the nodes of all infoservices we know */
			Enumeration allInfoServices = Database.getInstance(InfoServiceDBEntry.class).
				getEntrySnapshotAsEnumeration();
			while (allInfoServices.hasMoreElements())
			{
				/* import the infoservice node in this document */
				Node infoServiceNode = ( (InfoServiceDBEntry) (allInfoServices.nextElement())).toXmlElement(
					doc);
				infoServicesNode.appendChild(infoServiceNode);
			}
			doc.appendChild(infoServicesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * Sends info about a special payment instance to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchPaymentInstanceInfo(String a_piID)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			Enumeration en = Database.getInstance(PaymentInstanceDBEntry.class).getEntrySnapshotAsEnumeration();
			while (en.hasMoreElements())
			{
				PaymentInstanceDBEntry entry = (PaymentInstanceDBEntry) en.nextElement();
				if (entry.getId().equals(a_piID))
				{
					Document doc = XMLUtil.createDocument();
					doc.appendChild(entry.toXmlElement(doc));
					httpResponse = new HttpResponseStructure(doc);
				}
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}

		return httpResponse;
	}

	/**
	 * Sends the complete list of all known payment instances to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchPaymentInstances()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			Document doc = XMLUtil.createDocument();
			/* create the InfoServices element */
			Element paymentInstances = doc.createElement("PaymentInstances");
			/* append the nodes of all infoservices we know */
			Enumeration allPaymentInstances = Database.getInstance(PaymentInstanceDBEntry.class).
				getEntrySnapshotAsEnumeration();
			while (allPaymentInstances.hasMoreElements())
			{
				/* import the infoservice node in this document */
				Node paymentInstanceNode = ( (PaymentInstanceDBEntry) (allPaymentInstances.nextElement())).
					toXmlElement(
						doc);
				paymentInstances.appendChild(paymentInstanceNode);
			}
			doc.appendChild(paymentInstances);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mixcascade (first mix) or when we
	 * receive data from a remote infoservice, which posts data about a mixcascade.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure cascadePostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "MixCascade HELO received: XML: " + (new String(a_postData)));
			Element mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				MixCascade.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode,
				SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
			{
				MixCascade mixCascadeEntry = new MixCascade(mixCascadeNode);
				Database.getInstance(MixCascade.class).update(mixCascadeEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for MixCascade entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the complete list of all known mixcascades to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchCascades()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			Document doc = XMLUtil.createDocument();
			/* create the MixCascades element */
			Element mixCascadesNode = doc.createElement("MixCascades");
			/* append the nodes of all mixcascades we know */
			Enumeration knownMixCascades = Database.getInstance(MixCascade.class).
				getEntrySnapshotAsEnumeration();
			while (knownMixCascades.hasMoreElements())
			{
				/* import the MixCascade XML structure in this document */
				Element mixCascadeNode = ( (MixCascade) (knownMixCascades.nextElement())).toXmlNode(doc);
				mixCascadesNode.appendChild(mixCascadeNode);
			}
			doc.appendChild(mixCascadesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			/* should never occur */
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a non-free mix or when we receive data
	 * from a remote infoservice, which posts data about a non-free mix.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure mixPostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Mix HELO received: XML: " + (new String(a_postData)));
			Element mixNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				MixInfo.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(mixNode, SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
			{
				MixInfo mixEntry = new MixInfo(mixNode);
				Database.getInstance(MixInfo.class).update(mixEntry);
				//extract possible last proxy addresses
				VisibleProxyAddresses.addAddresses(mixNode);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for Mix entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mix which wants to get configured
	 * automatically (inserted in a auto-configure-cascade). If the mix is already assigned to a
	 * mixcascade, we will send back the XML structure of the cascade the mix now belongs to. This
	 * makes it possible for the mix to connect his neighbours.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure mixPostConfigure(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Mix Configure received: XML: " + (new String(a_postData)));
			Element mixNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				MixInfo.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(mixNode, SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
			{
				MixInfo mixEntry = new MixInfo(mixNode);
				/* check whether the mix is already assigned to a mixcascade */
				Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList().elements();
				MixCascade assignedCascade = null;
				while (knownMixCascades.hasMoreElements() && (assignedCascade == null))
				{
					MixCascade currentCascade = (MixCascade) (knownMixCascades.nextElement());
					if (currentCascade.getMixIds().contains(mixEntry.getId()))
					{
						/* the mix is assigned to that cascade */
						assignedCascade = currentCascade;
					}
				}
				if (assignedCascade == null)
				{
					/* the mix is not assigned to any cascade and wants to get configured -> it's a free mix
					 */
					mixEntry.setFreeMix(true);
				}
				else
				{
					/* send back the XML structure of the cascade the mix belongs to */
					httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
						XMLUtil.toString(assignedCascade.getXmlStructure()));
				}
				/* update the database in every case */
				Database.getInstance(MixInfo.class).update(mixEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for Mix entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the XML encoded mix entry for the specified ID to the client.
	 *
	 * @param a_mixId The ID of the requested mix.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetMix(String a_mixId)
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			MixInfo mixEntry = (MixInfo) (Database.getInstance(MixInfo.class).getEntryById(a_mixId));
			if (mixEntry == null)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					XMLUtil.toString(mixEntry.getXmlStructure()));
			}
		}
		catch (Exception e)
		{
			/* should never occur */
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mixcascade about the status or when we
	 * receive data from a remote infoservice, which posts data about mixcascade status.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure cascadePostStatus(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Status received: XML: " + (new String(a_postData)));
			Element mixCascadeStatusNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(
				a_postData), StatusInfo.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(mixCascadeStatusNode,
				SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
			{
				/* the currently required minimum JAP version is only needed for compatibility with JAP
				 * < 00.02.016
				 * @todo remove it
				 */
				JAPMinVersion currentMinVersion = (JAPMinVersion) (Database.getInstance(JAPMinVersion.class).
					getEntryById("JAPMinVersion"));
				/* better than nothing, version 00.00.000 means, that every JAP version is up to date */
				String minVersionString = "00.00.000";
				if (currentMinVersion != null)
				{
					minVersionString = currentMinVersion.getJapSoftware().getVersion();
				}
				StatusInfo statusEntry = new StatusInfo(mixCascadeStatusNode, minVersionString);
				Database.getInstance(StatusInfo.class).update(statusEntry);
				/* update the statistics, if they are not enabled or we know the received status message
				 * already, nothing is done by this call
				 */
				StatusStatistics.getInstance().update(statusEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for mixcascade status entry! XML: " +
							  (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the XML encoded status of the mixcascade with the ID given by cascadeId. It uses
	 * the original version (as the infoservice has received it) of the XML structure.
	 *
	 * @param a_cascadeId The ID of the mixcascade.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetCascadeStatus(String a_cascadeId)
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			StatusInfo statusEntry = (StatusInfo) Database.getInstance(StatusInfo.class).getEntryById(
				a_cascadeId);
			if (statusEntry == null)
			{
				/* we don't have a status for the given id */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					statusEntry.getStatusXmlData());
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * Sends a generated HTML file with all status entrys to the client. This function is not used
	 * by the JAP client. It's intended to use with a webbrowser to see the status of all cascades.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure humanGetStatus()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			String htmlData = "<HTML>\n" +
				"  <HEAD>\n" +
				"    <TITLE>JAP - InfoService - Cascade Status</TITLE>\n" +
				"    <STYLE TYPE=\"text/css\">\n" +
				"      <!--\n" +
				"        h1 {color:blue; text-align:center;}\n" +
				"        b,h3,h4,h5 {font-weight:bold; color:maroon;}\n" +
				"        body {margin-top:0px; margin-left:0px; margin-width:0px; margin-height:0px; background-color:white; color:black;}\n" +
				"        h1,h2,h3,h4,h5,p,address,ol,ul,tr,td,th,blockquote,body,.smalltext,.leftcol {font-family:geneva,arial,helvetica,sans-serif;}\n" +
				"        p,address,ol,ul,tr,td,th,blockquote {font-size:11pt;}\n" +
				"        .leftcol,.smalltext {font-size: 10px;}\n" +
				"        h1 {font-size:17px;}\n" +
				"        h2 {font-size:16px;}\n" +
				"        h3 {font-size:15px;}\n" +
				"        h4 {font-size:14px;}\n" +
				"        h5 {font-size:13px;}\n" +
				"        address {font-style:normal;}\n" +
				"        hr {color:#cccccc;}\n" +
				"        h2,.leftcol {font-weight:bold; color:#006699;}\n" +
				"        a:link {color:#006699; font-weight:bold; text-decoration:none;}\n" +
				"        a:visited {color:#666666; font-weight:bold; text-decoration:none;}\n" +
				"        a:active {color:#006699; font-weight:bold; text-decoration:none;}\n" +
				"        a:hover {color:#006699; font-weight:bold; text-decoration:underline;}\n" +
				"        th {color:white; background:#006699; font-weight:bold; text-align:left;}\n" +
				"        td.name {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699; background:#eeeeff;}\n" +
				"        td.status {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699;}\n" +
				"      -->\n" +
				"    </STYLE>\n" +
				"    <META HTTP-EQUIV=\"refresh\" CONTENT=\"25\">\n" +
				"  </HEAD>\n" +
				"  <BODY BGCOLOR=\"#FFFFFF\">\n" +
				"    <P ALIGN=\"right\">" + (new Date()).toString() + "</P>\n" +
				"    <H2>JAP - Cascade Status</H2><BR><BR>\n" +
				"    <TABLE ALIGN=\"center\" BORDER=\"0\">\n" +
				"      <COLGROUP>\n" +
				"        <COL WIDTH=\"20%\">\n" +
				"        <COL WIDTH=\"15%\">\n" +
				"        <COL WIDTH=\"10%\">\n" +
				"        <COL WIDTH=\"7%\">\n" +
				"        <COL WIDTH=\"10%\">\n" +
				"        <COL WIDTH=\"13%\">\n" +
				"        <COL WIDTH=\"25%\">\n" +
				"      </COLGROUP>\n" +
				"      <TR>\n" +
				"        <TH>Cascade Name</TH>\n" +
				"        <TH>Cascade ID</TH>\n" +
				"        <TH>Active Users</TH>\n" +
				"        <TH>Current Risk</TH>\n" +
				"        <TH>Traffic Situation</TH>\n" +
				"        <TH>Mixed Packets</TH>\n" +
				"        <TH>Last Notification</TH>\n" +
				"      </TR>\n";
			/* get all status entrys from database */
			Enumeration enumer = Database.getInstance(StatusInfo.class).getEntrySnapshotAsEnumeration();
			while (enumer.hasMoreElements())
			{
				/* get the HTML table line */
				htmlData = htmlData + "      " + ( (StatusInfo) (enumer.nextElement())).getHtmlTableLine() +
					"\n";
			}
			htmlData = htmlData + "    </TABLE><BR><BR><BR><BR>\n";
			htmlData = htmlData + "    <P>Infoservice [" + Constants.INFOSERVICE_VERSION + "] Startup Time: " +
				Configuration.getInstance().getStartupTime() +
				"</P>\n" +
				"    <HR noShade SIZE=\"1\">\n" +
				"    <ADDRESS>&copy; 2000 - 2005 The JAP Team</ADDRESS>\n" +
				"  </BODY>\n" +
				"</HTML>\n";
			/* send content */
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_HTML, htmlData);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	/**
	 * Sends the complete list of all known mixes to the client. This command is not used by
	 * the JAP client. It's just a comfort function to see all currently working mixes.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure fetchAllMixes()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			/* create xml document */
			Document doc = XMLUtil.createDocument();
			/* create the Mixes element */
			Element mixesNode = doc.createElement("Mixes");
			/* append the nodes of all mixes we know */
			Enumeration knownMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			while (knownMixes.hasMoreElements())
			{
				/* import the mix node in this document */
				Element mixNode = (Element) (XMLUtil.importNode(doc,
					( (MixInfo) (knownMixes.nextElement())).getXmlStructure(), true));
				mixesNode.appendChild(mixNode);
			}
			doc.appendChild(mixesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	/**
	 * Constructs an XML structure containing a list of all free mixes we know. Those mixes were
	 * announced to us via the '/configure' method and are currently not assigned to any cascade.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure fetchAvailableMixes()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			/* create xml document */
			Document doc = XMLUtil.createDocument();
			/* create the Mixes element */
			Element mixesNode = doc.createElement("Mixes");
			/* append the nodes of all free mixes we know */
			Enumeration knownMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			while (knownMixes.hasMoreElements())
			{
				/* import the mix node in this document */
				MixInfo mixEntry = (MixInfo) (knownMixes.nextElement());
				if (mixEntry.isFreeMix())
				{
					Element mixNode = (Element) (XMLUtil.importNode(doc, mixEntry.getXmlStructure(), true));
					mixesNode.appendChild(mixNode);
				}
			}
			doc.appendChild(mixesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	/**
	 * Sends the XML encoded mixcascade entry the ID given by cascadeId to the client.
	 *
	 * @param a_cascadeId The ID of the requested mixcascade.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getCascadeInfo(String a_cascadeId)
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			MixCascade mixCascadeEntry = (MixCascade) (Database.getInstance(MixCascade.class).getEntryById(
				a_cascadeId));
			if (mixCascadeEntry == null)
			{
				/* we don't have a mixcascade with the given id */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				/* send XML-Document */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					XMLUtil.toString(mixCascadeEntry.getXmlStructure()));
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	private HttpResponseStructure echoIP(InetAddress a_sourceAddress)
	{
		HttpResponseStructure httpResponse;
		Document docEchoIP = XMLUtil.createDocument();
		Node nodeEchoIP = docEchoIP.createElement("EchoIP");
		Node nodeIP = docEchoIP.createElement("IP");

		docEchoIP.appendChild(nodeEchoIP);
		nodeEchoIP.appendChild(nodeIP);
		XMLUtil.setValue(nodeIP, a_sourceAddress.getHostAddress());
		httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK,
												 XMLUtil.toString(docEchoIP));

		return httpResponse;
	}

	/**
	 * Sends the complete list of all known tor nodes to the client. This command is used by the
	 * JAP clients with tor integration. If we don't have a current tor nodes list, we return -1
	 * and the client will get an http error. So the client will ask another infoservice.
	 *
	 * @param a_compressed Whether to send the compressed version of the TOR nodes list to the
	 *                     client (true) or the uncompressed version (false).
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getTorNodesList(boolean a_compressed)
	{
		/* this is only the default, if we don't have a TOR list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		Node torNodesList = null;
		if (a_compressed == true)
		{
			torNodesList = TorDirectoryAgent.getInstance().getCompressedTorNodesList();
		}
		else
		{
			torNodesList = TorDirectoryAgent.getInstance().getTorNodesList();
		}
		if (torNodesList != null)
		{
			try
			{
				/* create xml document */
				Document doc = XMLUtil.createDocument();
				doc.appendChild(XMLUtil.importNode(doc, torNodesList, true));
				httpResponse = new HttpResponseStructure(doc);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		return httpResponse;
	}

	/**
	 * Sends the complete list of all known mixminion nodes to the client. This command is used by the
	 * JAP clients with mixminion integration. If we don't have a current mixminion nodes list, we return -1
	 * and the client will get an http error. So the client will ask another infoservice.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getMixminionNodesList()
	{
		/* this is only the default, if we don't have a TOR list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		Node mixminionNodesList = null;
		mixminionNodesList = MixminionDirectoryAgent.getInstance().getMixminionNodesList();
		if (mixminionNodesList != null)
		{
			try
			{
				/* create xml document */
				Document doc = XMLUtil.createDocument();
				doc.appendChild(XMLUtil.importNode(doc, mixminionNodesList, true));
				httpResponse = new HttpResponseStructure(doc);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		return httpResponse;
	}

	/**
	 * Adds a new JAP forwarder to the database of JAP forwarders. But first we verify the
	 * connection, if this is successful we add the entry and send the forwarder entry ID back
	 * to the forwarder. So he knows under which ID he can renew the entry.
	 *
	 * @param a_postData The data we have received.
	 * @param a_sourceAddress The internet address where the request was coming from. We use this
	 *                        for checking the connection to the forwarder.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure addJapForwarder(byte[] a_postData, InetAddress a_sourceAddress)
	{
		/* this is only the default, if we don't have a primary forwarder list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_BAD_REQUEST);
		String answer = JapForwardingTools.addForwarder(a_postData, a_sourceAddress);
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML, answer);
		}
		return httpResponse;
	}

	/**
	 * Renews the entry of a JAP forwarder in the database of JAP forwarders. We write back some
	 * status information, whether it was successful.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure renewJapForwarder(byte[] a_postData)
	{
		/* this is only the default, if we don't have a primary forwarder list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_BAD_REQUEST);
		String answer = JapForwardingTools.renewForwarder(a_postData);
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML, answer);
		}
		return httpResponse;
	}

	/**
	 * Gets a forwarder entry (encoded with a captcha) from the JAP forwarder database. If we have
	 * such a database, but there is no data in it, we send back an answer with the error
	 * description. If this infoservice doesn't have a primary forwarder list, we aks all known
	 * infoservices with such a list for an entry, until we get an entry from one of them or we
	 * asked all and no one has a JAP forwarder entry. In this case we send also an answer with
	 * the error description back to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getJapForwarder()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		String answer = JapForwardingTools.getForwarder();
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML, answer);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from another infoservice with the minimum
	 * required JAP client version.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japPostCurrentJapVersion(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "JAPMinVersion received: XML: " + (new String(a_postData)));
			Element japNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				JAPMinVersion.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(japNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == true)
			{
				JAPMinVersion minVersionEntry = new JAPMinVersion(japNode);
				Database.getInstance(JAPMinVersion.class).update(minVersionEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for JAPMinVersion entry! XML: " +
							  (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the version number of the current minimum required JAP client software as an XML
	 * structure.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetCurrentJapVersion()
	{
		/* this is only the default, if we don't know the current JAP version */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		JAPMinVersion minVersionEntry = (JAPMinVersion) (Database.getInstance(JAPMinVersion.class).
			getEntryById("JAPMinVersion"));
		if (minVersionEntry != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
				XMLUtil.toString(minVersionEntry.getXmlStructure()));
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from another infoservice with the current
	 * japRelease.jnlp or japDevelopment.jnlp Java WebStart files.
	 *
	 * @param a_fileName The filename of the JNLP file (full path starting with / + filename).
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure postJnlpFile(String a_fileName, byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "JNLP file received (" + a_fileName + "): XML: " + (new String(a_postData)));
			Element jnlpNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				JAPVersionInfo.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(jnlpNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == true)
			{
				JAPVersionInfo jnlpEntry = null;
				if (a_fileName.equals("/japRelease.jnlp"))
				{
					jnlpEntry = new JAPVersionInfo(jnlpNode, JAPVersionInfo.JAP_RELEASE_VERSION);
				}
				else if (a_fileName.equals("/japDevelopment.jnlp"))
				{
					jnlpEntry = new JAPVersionInfo(jnlpNode, JAPVersionInfo.JAP_DEVELOPMENT_VERSION);
				}
				else
				{
					throw (new Exception("InfoServiceCommands: postJnlpFile: Invalid filename specified (" +
										 a_fileName + ")."));
				}
				Database.getInstance(JAPVersionInfo.class).update(jnlpEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for JNLP file! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the JNLP files for the JAP development or release version to the JAP client or any
	 * other system which makes a Java WebStart request.
	 *
	 * @param a_fileName The filename of the requested JNLP file (full path starting with / +
	 *                   filename).
	 * @param a_httpMethod Describes the HTTP method (can be GET or HEAD, see constants in
	 *                     Constants.java). Java WebStart requests firstly only the header without
	 *                     the content and the asks a second time for the whole thing (header +
	 *                     content).
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getJnlpFile(String a_fileName, int a_httpMethod)
	{
		/* this is only the default, if we cannot find the requested JNLP file */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		JAPVersionInfo jnlpFile = (JAPVersionInfo) (Database.getInstance(JAPVersionInfo.class).getEntryById(
			a_fileName));
		if (jnlpFile != null)
		{
			if (a_httpMethod == Constants.REQUEST_METHOD_GET)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_APPLICATION_JNLP,
					XMLUtil.toString(jnlpFile.getXmlStructure()));
			}
			else if (a_httpMethod == Constants.REQUEST_METHOD_HEAD)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_APPLICATION_JNLP,
					XMLUtil.toString(jnlpFile.getXmlStructure()), true);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
			}
		}
		return httpResponse;
	}

	/**
	 * This function sends the addresses of
	 * the proxy servers at the end of the cascades as plain text to the client. The info about
	 * the proxies comes from the configuration property file and from the information given
	 * by Last Mixes.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getProxyAddresses()
	{
		/* this is only the default, if we don't know the proxy addresses */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		String strConfiguredProxies = Configuration.getInstance().getProxyAddresses();
		String strReportedProxies = VisibleProxyAddresses.getVisibleAddresses();
		if (strConfiguredProxies == null)
		{
			if (strReportedProxies != null)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
					strReportedProxies);
			}

		}
		else
		{
			if (strReportedProxies != null)
			{
				strConfiguredProxies += " " + strReportedProxies;
			}
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				strConfiguredProxies);
		}
		return httpResponse;
	}

	/**
	 * This is the handler for processing the InfoService commands.
	 *
	 * @param method The HTTP method used within the request from the client. See the REQUEST_METHOD
	 *               constants in anon.infoservice.Constants.
	 * @param command The URL requested from the client within the HTTP request. Normally this
	 *                should be an absolute path with a filename.
	 * @param postData The HTTP content data (maybe of size 0), if the request was an HTTP POST. If
	 *                 the HTTP method was not POST, this value is always null.
	 * @param a_sourceAddress The internet address from where we have received the request. It is
	 *                        the address of the other end of the socket connection, so maybe it is
	 *                        only the address of a proxy.
	 *
	 * @return The response to send back to the client. This value is null, if the request cannot
	 *         be handled by this implementation (maybe because of an invalid command, ...).
	 */
	public HttpResponseStructure processCommand(int method, String command, byte[] postData,
												InetAddress a_sourceAddress)
	{
		HttpResponseStructure httpResponse = null;
		if ( (command.equals("/infoserver")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from another infoservice (can be forwared by an infoservice), which includes
			 * information about that infoservice
			 */
			httpResponse = infoServerPostHelo(postData);
		}
		else if ( (command.equals("/infoservices")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about all infoservices we know */
			httpResponse = japFetchInfoServers();
		}
		else if ( (command.equals("/cascade")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from the first mix of a cascade (can be forwarded by an infoservice), which
			 * includes information about the cascade
			 */
			httpResponse = cascadePostHelo(postData);
		}
		else if ( (command.equals("/cascades")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about all cascades we know */
			httpResponse = japFetchCascades();
		}
		else if ( (command.equals("/helo")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a mix (can be forwarded by an infoservice), which includes information
			 * about that mix
			 */
			httpResponse = mixPostHelo(postData);
		}
		else if ( (command.equals("/configure")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a mix requesting configuration */
			httpResponse = mixPostConfigure(postData);
		}
		else if ( (command.startsWith("/mixinfo/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about the mix with the given ID
			 * Full command: GET /mixinfo/mixid
			 */
			String mixId = command.substring(9);
			httpResponse = japGetMix(mixId);
		}
		else if ( (command.equals("/feedback")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from the first mix of a cascade (can be forwarded by an infoservice), which
			 * includes status information (traffic) of that cascade
			 */
			httpResponse = cascadePostStatus(postData);
		}
		else if ( (command.startsWith("/mixcascadestatus/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about the status of the cascade with the
			 * given ID
			 * Full command: GET /mixcascadestatus/cascadeid
			 */
			String cascadeId = command.substring(18);
			httpResponse = japGetCascadeStatus(cascadeId);
		}
		else if ( (command.equals("/status")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the status (traffic) information about all cascades for human view as html file */
			httpResponse = humanGetStatus();
		}
		else if ( (command.equals("/mixes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about all mixes (mixes of all cascades) */
			httpResponse = fetchAllMixes();
		}
		else if ( (command.equals("/availablemixes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about all mixes (mixes of all cascades) */
			httpResponse = fetchAvailableMixes();
		}
		else if ( (command.startsWith("/cascadeinfo/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about the cascade with the given ID (it's the same information as
			 * /cascades but there you get information about all known cascades)
			 * Full command: GET /cascadeinfo/cascadeid
			 */
			String cascadeId = command.substring(13);
			httpResponse = getCascadeInfo(cascadeId);
		}
		else if ( (command.equals("/tornodes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the list with all known tor nodes in an XML structure */
			httpResponse = getTorNodesList(false);
		}
		else if ( (command.equals("/compressedtornodes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the list with all known tor nodes in an XML structure */
			httpResponse = getTorNodesList(true);
		}
		else if ( (command.equals("/compressedmixminionnodes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the list with all known mixminion nodes in an XML structure */
			httpResponse = getMixminionNodesList();
		}
		else if ( (command.equals("/addforwarder")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* adds a new JAP forwarder to the database of known forwarders */
			httpResponse = addJapForwarder(postData, a_sourceAddress);
		}
		else if ( (command.equals("/renewforwarder")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* renews a JAP forwarder in the database of known forwarders */
			httpResponse = renewJapForwarder(postData);
		}
		else if ( (command.equals("/getforwarder")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get a captcha with information about a JAP forwarder */
			httpResponse = getJapForwarder();
		}
		else if ( (command.equals("/currentjapversion")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from another infoservice about the minimal needed JAP version */
			httpResponse = japPostCurrentJapVersion(postData);
		}
		else if ( (command.equals("/currentjapversion")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the current version of the client software */
			httpResponse = japGetCurrentJapVersion();
		}
		else if ( ( (command.equals("/japRelease.jnlp")) || (command.equals("/japDevelopment.jnlp"))) &&
				 (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from another infoservice with information about new JAP software */
			httpResponse = postJnlpFile(command, postData);
		}
		else if ( ( (command.equals("/japRelease.jnlp")) || (command.equals("/japDevelopment.jnlp"))) &&
				 ( (method == Constants.REQUEST_METHOD_GET) || (method == Constants.REQUEST_METHOD_HEAD)))
		{
			// request for JNLP File (WebStart or Update Request
			httpResponse = getJnlpFile(command, method);
		}
		else if ( (command.equals("/proxyAddresses")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* returns the addresses from the proxies at the end of the cascades, only
			 * for compatibility with some old scripts (written before world war II)
			 */
			/** @todo remove it */
			httpResponse = getProxyAddresses();
		}
		else if (command.equals("/echoip") && (method == Constants.REQUEST_METHOD_GET ||
											   method == Constants.REQUEST_METHOD_HEAD))
		{
			// just echo the clients ip adresse - for mix autoconfig resons
			httpResponse = echoIP(a_sourceAddress);
		}
		else if ( (command.equals("/paymentinstance")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a payment instance or another infoservice (can be forwared by an infoservice), which includes
			 * information about that payment instance
			 */
			httpResponse = paymentInstancePostHelo(postData);
		}
		else if ( (command.equals("/paymentinstances")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about all payment instacnes we know */
			httpResponse = japFetchPaymentInstances();
		}
		else if ( (command.startsWith("/paymentinstance/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about a special payment instance */
			String piID = command.substring(17);
			httpResponse = japFetchPaymentInstanceInfo(piID);
		}

		return httpResponse;
	}

}

/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.crypto;

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.Base64;
import anon.util.XMLUtil;

final public class JAPSignature
{

	private IMySignature signatureAlgorithm;

	public JAPSignature()
	{
		try
		{
			signatureAlgorithm = null;
		}
		catch (Exception e)
		{
		}
	}

	synchronized public void initVerify(IMyPublicKey k) throws InvalidKeyException
	{
		if (k == null)
		{
			throw new InvalidKeyException("Key is NULL");
		}
		if (k instanceof DSAPublicKey)
		{
			signatureAlgorithm = new MyDSASignature();
		}
		else if (k instanceof MyRSAPublicKey)
		{
			signatureAlgorithm = new MyRSASignature();
		}
		else
		{
			throw new InvalidKeyException("Unknown algorithm for key");
		}
		signatureAlgorithm.initVerify(k);
	}

	/**
	 * Initializes a JAPSignature instance for signing messages. This method needs only to be
	 * called once (before the first use of the signing methods). If you have called initVerify()
	 * before calling this method, the JAPSignature instance is switched from verifying to
	 * signing. Verifying will not work until you call initVerify() on this instance.
	 *
	 * @param ownPrivateKey The private key of a asymmetric algorithm (DSA at the moment) for
	 * signing the messages.
	 */
	synchronized public void initSign(IMyPrivateKey ownPrivateKey) throws InvalidKeyException
	{
		if (ownPrivateKey == null)
		{
			throw new InvalidKeyException("Key is NULL");
		}
		if (ownPrivateKey instanceof DSAPrivateKey)
		{
			signatureAlgorithm = new MyDSASignature();
		}
		else if (ownPrivateKey instanceof MyRSAPrivateKey)
		{
			signatureAlgorithm = new MyRSASignature();
		}
		else
		{
			throw new InvalidKeyException("Unknown algorithm for key");
		}
		signatureAlgorithm.initSign(ownPrivateKey);
	}

	public boolean verifyXML(InputStream xmlDoc)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document d = db.parse(xmlDoc);
			return verifyXML(d.getDocumentElement());
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Verifies the XML (signature) structure.
	 * @param n Root node of the XML structure. If it is a Document then the Root element of that document is
	 *  taken.
	 * @return true if it could be verified
	 * @return false otherwise
	 */
	public boolean verifyXML(Node n)
	{
		try
		{
			if (n == null)
			{
				return false;
			}
			Element root = null;
			if (n instanceof Document)
			{
				root = ( (Document) n).getDocumentElement();
			}
			else if (n instanceof Element)
			{
				root = (Element) n;
			}
			else
			{
				return false;
			}
			Element signature = (Element) XMLUtil.getFirstChildByName(root, "Signature");
			NodeList nl = signature.getElementsByTagName("SignedInfo");
			if (nl.getLength() < 1)
			{
				return false;
			}
			Element siginfo = (Element) nl.item(0);

			//make SigInfo Canonical....
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (makeCanonical(siginfo, out, false, null) == -1)
			{
				return false;
			}
			out.flush();
			//System.out.println(new String(out.toByteArray())+"   Size:"+Integer.toString(out.size()));
			nl = signature.getElementsByTagName("SignatureValue");
			if (nl.getLength() < 1)
			{
				return false;
			}
			Element signaturevalue = (Element) nl.item(0);
			String strSigValue = signaturevalue.getFirstChild().getNodeValue();
			//System.out.println("SigValue: "+strSigValue);

			//get r and s...
			//BASE64Decoder dec=new BASE64Decoder();
			byte[] rsbuff = Base64.decode(strSigValue);
			//dec.decodeBuffer(strSigValue);
			//System.out.println("Size of rsbuff: "+Integer.toString(rsbuff.length));
			if (rsbuff.length != 40)
			{
				return false;
			}

			//now rsBuff contains r (20 bytes) and s (20 bytes)

			byte[] tmpBuff=transcodeRStoDER(rsbuff);
			//testing Signature....
			byte[] buff = out.toByteArray();
			if (!verify(buff, tmpBuff))
			{
				return false;
			}
			//making Reference hash....
			out.reset();
			if (makeCanonical(root, out, false, signature) == -1)
			{
				return false;
			}
			out.flush();
			//System.out.println(new String(out.toByteArray())+"   Size:"+Integer.toString(out.size()));
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			byte[] hk = out.toByteArray();
			byte[] digest = sha1.digest(hk);
			//System.out.println("Messgaediegst-Size: "+digest.length);

			//Decoding <DigestValue>
			nl = siginfo.getElementsByTagName("DigestValue");
			if (nl.getLength() < 1)
			{
				return false;
			}
			String strDigest = nl.item(0).getFirstChild().getNodeValue();
			tmpBuff = Base64.decode(strDigest);
			return MessageDigest.isEqual(tmpBuff, digest);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	synchronized public boolean verify(byte[] message, byte[] sig,boolean bIsRSEncoded)
	{
		if(bIsRSEncoded)
			sig=transcodeRStoDER(sig);
		return verify(message,sig);
	}

	synchronized public boolean verify(byte[] message, byte[] sig)
		{
		return signatureAlgorithm.verify(message, sig);
	}

	/**
	 * Signs an array of bytes.
	 *
	 * @param bytesToSign byte[] Bytes to sign
	 * @return byte[] the signature, or null if something goes wrong
	 */
	synchronized public byte[] signBytes(byte[] bytesToSign)
	{
		try
		{
			return signatureAlgorithm.sign(bytesToSign);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	/** Returns all Certificates which are included in this Signatures <KeyInfo> element
	 * @return certifcates emmbeded in the Signature Node
	 * @return null if no certificates could be found or something else goes wrong
	 * @todo At the moment only returns the first Certifcate found!
	 */
	public static JAPCertificate[] getAppendedCertificates(Node nSig)
	{
		if (! (nSig instanceof Element))
		{
			return null;
		}
		Element nodeSig = (Element) nSig;
		Element elemKeyInfo = (Element) XMLUtil.getFirstChildByName(nodeSig, "KeyInfo");
		if (elemKeyInfo == null)
		{
			return null;
		}
		/* there is a certificate appended */
		Element elemX509Data = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "X509Data");
		Element elemX509Certificate = (Element) XMLUtil.getFirstChildByName(elemX509Data,
			"X509Certificate");
		JAPCertificate appendedCertificate = null;
		appendedCertificate = JAPCertificate.getInstance(elemX509Certificate);
		if (appendedCertificate != null)
		{
			JAPCertificate[] t = new JAPCertificate[1];
			t[0] = appendedCertificate;
			return t;
		}
		return null;
	}

	/**
	 * Signs an XML document with the own private key. The signature is directly inserted as
	 * a child of the root node of the document, so there is nothing returned. If there is
	 * already a signature in the document, it is removed.
	 *
	 * @param toSign The document you want to sign.
	 */
	public void signXmlDoc(Document toSign) throws Exception
	{
		if (toSign == null)
		{
			throw new Exception("JAPSignature: signXmlDoc: Nothing to sign!");
		}
		Element rootNode = toSign.getDocumentElement();
		if (rootNode == null)
		{
			throw new Exception("JAPSignature: signXmlDoc: No document root!");
		}
		signXmlNode(rootNode);
	}

	/**
	 * Signs an XML node with the own private key. The signature is directly inserted as
	 * a child of the node, so there is nothing returned. If there is already a signature in
	 * the tree under the node, it is removed.
	 *
	 * @param toSign The document you want to sign.
	 */
	public void signXmlNode(Element toSign) throws Exception
	{
		/* if there are any Signature nodes, remove them --> we create a new one */
		Node oldSig = XMLUtil.getFirstChildByName(toSign, "Signature");
		//NodeList signatureNodes = toSign.getElementsByTagName("Signature");
		//for (int i = signatureNodes.getLength(); i > 0; i--)
		//{
		/* if there are any Signature nodes, remove them --> we create a new one */
		//	toSign.removeChild(signatureNodes.item(i - 1));
		//}
		if (oldSig != null)
		{
			toSign.removeChild(oldSig);
		}
		ByteArrayOutputStream bytesToSign = nodeToCanonical(toSign);
		/* now we have a XML bytestream of our toSign node (incl. name + attributes + child tree),
		 * now use a message digest algorithm with it
		 */
		byte[] digest = MessageDigest.getInstance("SHA-1").digest(bytesToSign.toByteArray());
		/* now build the SignedInfo node tree */
		Document doc = toSign.getOwnerDocument();
		Element signedInfoNode = doc.createElement("SignedInfo");
		Element referenceNode = doc.createElement("Reference");
		referenceNode.setAttribute("URI", "");
		Element digestValueNode = doc.createElement("DigestValue");
		digestValueNode.appendChild(doc.createTextNode(new String(Base64.encode(digest, false))));
		referenceNode.appendChild(digestValueNode);
		signedInfoNode.appendChild(referenceNode);
		/* now we sign the SignedInfo node tree, first we need the XML bytestream */
		bytesToSign = nodeToCanonical(signedInfoNode);
		/* now we use an asymmetric crypto algorithm with our private key on the bytestream of the
		 * SignedInfo tree to create the signature (SHA1 hash is included in the Java implementation
		 * of DSA)
		 */
		byte[] signatureAsn1 = signBytes(bytesToSign.toByteArray());
		/* now extract the ASN.1 encoded values for r and s from the signature */
		/* ASN.1 Notation:
		 *   sequence {
		 *     integer r
		 *     integer s
		 *   }
		 * --> Der-Encoding
		 * byte   0x30    // Sequence
		 * byte   44 + x  // len in bytes (x = {0|1|2} depending on r and s
		 * byte   0x02    // integer
		 * byte   <= 21   // len of r (21: if first bit of r set, we need a leading 0 --> 20 + 1 bytes)
		 * byte[] ...     // value of r (with leading zero if necessary)
		 * byte   0x02    // integer
		 * byte   <= 21   // len of s (21: if first bit of s set, we need a leading 0 --> 20 + 1 bytes)
		 * byte[] ...     // value of s (with leading zero if necessary)
		 */
		byte rLength = signatureAsn1[3];
		byte sLength = signatureAsn1[3 + rLength + 2];
		byte[] signature = new byte[40];
		for (int i = 0; i < 40; i++)
		{
			/* be sure that it is zero */
			signature[i] = 0;
		}
		byte rOverLength = 0;
		if (rLength == 21)
		{
			rOverLength = 1;
			rLength = 20;
		}
		System.arraycopy(signatureAsn1, 4 + rOverLength, signature, 20 - rLength, rLength);
		rLength = (byte) (rLength + rOverLength);
		byte sOverLength = 0;
		if (sLength == 21)
		{
			sOverLength = 1;
			sLength = 20;
		}
		System.arraycopy(signatureAsn1, 4 + rLength + 2 + sOverLength, signature, 40 - sLength, sLength);
		/* create the SignatureValue node and build the Signature tree */
		Element signatureValueNode = doc.createElement("SignatureValue");
		signatureValueNode.appendChild(doc.createTextNode(new String(Base64.encode(signature, false))));
		Element signatureNode = doc.createElement("Signature");
		signatureNode.appendChild(signedInfoNode);
		signatureNode.appendChild(signatureValueNode);
		/* now add the Signature node as a child to our toSign node */
		toSign.appendChild(signatureNode);
	}

	/**
	 * Signs an XML node with the own private key. The signature is directly inserted as
	 * a child of the node, so there is nothing returned. If there is already a signature in
	 * the tree under the node, it is removed. Also the given certifcates are included to make
	 * it easier for the recipient to check the signature.
	 *
	 * @param toSign The document you want to sign.
	 */
	public void signXmlNode(Element toSign, JAPCertificate certsToAdd[]) throws Exception
	{
		signXmlNode(toSign);
		if (certsToAdd != null)
		{
			/* now add X509 certificates */
			Element signatureNode = (Element) XMLUtil.getFirstChildByName(toSign, "Signature");
			if (signatureNode == null)
			{
				throw (new Exception("Error in XML structure."));
			}
			Document doc = toSign.getOwnerDocument();
			Element elemKeyInfo = doc.createElement("KeyInfo");
			Element elemX509Data = doc.createElement("X509Data");
			signatureNode.appendChild(elemKeyInfo);
			elemKeyInfo.appendChild(elemX509Data);
			for (int i = 0; i < certsToAdd.length; i++)
			{
				elemX509Data.appendChild(certsToAdd[i].toXmlElement(doc));

			}
		}
	}

	/**
	 * Signs an XML node with the own private key. The signature is directly inserted as
	 * a child of the node, so there is nothing returned. If there is already a signature in
	 * the tree under the node, it is removed. Also the given certifcate is included to make
	 * it easier for the recipient to check the signature.
	 *
	 * @param toSign The document you want to sign.
	 */
	public void signXmlNode(Element toSign, JAPCertificate certToAdd) throws Exception
	{
		JAPCertificate tmp[] = new JAPCertificate[1];
		tmp[0] = certToAdd;
		signXmlNode(toSign, tmp);
	}

	/**
	 * Creates a bytestream out of the abstract tree of the node.
	 *
	 * @param inputNode The node (incl. the whole tree) which is flattened to a bytestream.
	 *
	 * @return The bytestream of the node (incl. the whole tree).
	 */
	private ByteArrayOutputStream nodeToCanonical(Node inputNode) throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		/* TODO: find a better way to get the data of the node as a bytestream, for
		 * compatibility reasons we use this now
		 */
		if (makeCanonical(inputNode, out, true, null) == -1)
		{
			throw new Exception("JAPSignature: nodeToCanonical: Could not make the node canonical!");
		}
		out.flush();
		return out;
	}

//Thread safe ?
	private int makeCanonical(Node node, OutputStream o, boolean bSiblings, Node excludeNode)
	{
		try
		{
			if (node == null)
			{
				return 0;
			}
			if (node.equals(excludeNode))
			{
				return 0;
			}
			if (node.getNodeType() == node.ELEMENT_NODE)
			{
				Element elem = (Element) node;
				o.write('<');
				o.write(elem.getNodeName().getBytes());
				NamedNodeMap attr = elem.getAttributes();
				if (attr.getLength() > 0)
				{
					for (int i = 0; i < attr.getLength(); i++)
					{
						o.write(' ');
						o.write(attr.item(i).getNodeName().getBytes());
						o.write('=');
						o.write('\"');
						o.write(attr.item(i).getNodeValue().getBytes());
						o.write('\"');
					}
				}
				o.write('>');
				if (elem.hasChildNodes())
				{
					if (makeCanonical(elem.getFirstChild(), o, true, excludeNode) == -1)
					{
						return -1;
					}
				}
				o.write('<');
				o.write('/');
				o.write(elem.getNodeName().getBytes());
				o.write('>');
				if (bSiblings && makeCanonical(elem.getNextSibling(), o, true, excludeNode) == -1)
				{
					return -1;
				}
			}
			else if (node.getNodeType() == node.TEXT_NODE)
			{
				o.write(node.getNodeValue().trim().getBytes());
				if (makeCanonical(node.getNextSibling(), o, true, excludeNode) == -1)
				{
					return -1;
				}
				return 0;
			}
			else if (node.getNodeType() == node.COMMENT_NODE)
			{
				if (makeCanonical(node.getNextSibling(), o, true, excludeNode) == -1)
				{
					return -1;
				}
				return 0;
			}
			else
			{
				return -1;
			}
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	/**Making DER-Encoding of r and s.....
// ASN.1 Notation:
//  sequence
//    {
//          integer r
//          integer s
//    }
// HINT: Sun JDK 1.4.x needs a leading '0' in the binary representation
// of r (and s) if r[0]>0x7F or s[0]>0x7F
//--> Der-Encoding
// 0x30 //Sequence
// 44 + x // len in bytes (x = {0|1|2} depending on r and s (see above)
// 0x02 // integer
// 20 | 21 // len in bytes of r
// ....   //value of r (with leading zero if necessary)
// 0x02 //integer
// 20 | 21  //len of s
// ... value of s (with leading zero if necessary)
	 **/
	private byte[] transcodeRStoDER(byte[] rsbuff)
	{

		int index = 46;
		if (rsbuff[0] < 0)
		{
			index++;
		}
		if (rsbuff[20] < 0)
		{
			index++;
		}
		byte tmpBuff[] = new byte[index];
		tmpBuff[0] = 0x30;
		tmpBuff[1] = (byte) (index - 2);
		tmpBuff[2] = 0x02;
		if (rsbuff[0] < 0)
		{
			index = 5;
			tmpBuff[3] = 21;
			tmpBuff[4] = 0;
		}
		else
		{
			tmpBuff[3] = 20;
			index = 4;
		}
		System.arraycopy(rsbuff, 0, tmpBuff, index, 20);
		index += 20;
		tmpBuff[index++] = 0x02;
		if (rsbuff[20] < 0)
		{
			tmpBuff[index++] = 21;
			tmpBuff[index++] = 0;
		}
		else
		{
			tmpBuff[index++] = 20;
		}
		System.arraycopy(rsbuff, 20, tmpBuff, index, 20);
		return tmpBuff;
	}
}

/*
   Copyright (c) 2000, The JAP-Team All rights reserved.
   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of the University of Technology Dresden, Germany
   nor the names of its contributors may be used to endorse or promote
   products derived from this software without specific prior written
   permission.


   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
   REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
   POSSIBILITY OF SUCH DAMAGE
*/
package payxml;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import org.w3c.dom.*;

import payxml.util.Base64;
import payxml.util.Signer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.math.BigInteger;

import java.security.*;


/**
 * This class contains the functionality for parsing and verifying XML
 * signatures, and for signing XML documents using the W3C's enveloped
 * signature format. The class XMLSigner is obsolete, its functionality is now
 * included in this class. This class can be used for two main purposes:
 * 
 * <ol>
 * <li>
 * XML Signature checking:
 * <pre>
 *   org.bouncycastle.crypto.params.RSAKeyParameters signersPublicKey;
 *   String mySignedXMLString;
 *   XMLSignature sig = new XMLSignature( mySignedXMLString );
 *   sig.initVerify();
 *   if( sig.verifyXML() )
 *      println("Signature OK");
 *   else
 *      println("Signature invalid");
 * </pre>
 * </li>
 * <li>
 * Signing of arbitrary XML data (example obsolete, needs update!!!):
 * <pre>
 *   org.bouncycastle.crypto.params.RSAKeyParameters myPrivateKey;
 *   String myXMLString = "&lt;?xml version=\"1.0\"?&gt;&lt;body&gt;&lt;any&gt;xml&lt;/any&gt;"+
 *                        "&lt;example&gt;document&lt;/example&gt;&lt;/body&gt;&lt;/xml&gt;";
 *   XMLSignature sig = new XMLSignature(myXMLString.getBytes());
 *   sig.initSign( myPrivateKey );
 *   String signedXML = sig.signXML();
 * </pre>
 * </li>
 * </ol>
 *
 * For usage of this class see the file XMLSignatureTest.java included in the 
 * package payxml.test
 */
final public class XMLSignature extends XMLDocument
{
	//~ Static fields/initializers *********************************************

	static private final String templateSignature1 = 
		"<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">";
	static private final String templateSignature2 = "<SignatureValue>";
	static private final String templateSignature3 = "</SignatureValue></Signature>";
	static final String templateSignedInfo1 =
		"<SignedInfo><CanonicalizationMethod Algorithm=\"" +
		"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\">" +
		"</CanonicalizationMethod><SignatureMethod Algorithm=\"" +
		"http://www.w3.org/2000/09/xmldsig#rsa-sha1\">" +
		"</SignatureMethod><Reference URI=\"\"><Transforms><Transform Algorithm=\"" +
		"http://www.w3.org/2000/09/xmldsig#enveloped-signature\">" +
		"</Transform></Transforms><DigestMethod Algorithm=\"" +
		"http://www.w3.org/2000/09/xmldsig#sha1\">" +
		"</DigestMethod><DigestValue>";
	static final String templateSignedInfo2 = "</DigestValue></Reference></SignedInfo>";

	//~ Instance fields ********************************************************

	private RSAKeyParameters m_privateKey;
	private RSAKeyParameters m_publicKey;
	private payxml.util.Signer signer = new Signer();

	//~ Constructors ***********************************************************

	public XMLSignature(byte[] data) throws Exception
	{
		setDocument(data);
		try {
			m_publicKey = null;

			//sig=Signature.getInstance("SHA1withRSA");
		} catch (Exception e) {
			//sig=null;
		}
	}

	//~ Methods ****************************************************************

	/**
	 * Initialize this instance for signing
	 *
	 * @param privKey private key for signing
	 */
	public void initSign(RSAKeyParameters privKey)
	{
		m_privateKey = privKey;
		signer.init(true, privKey);
	}


	/**
	 * Initizalize this instance for checking signatures.
	 *
	 * @param pubKey public key for checking the signatures
	 */
	public void initVerify(RSAKeyParameters pubKey) throws InvalidKeyException
	{
		signer.init(false, pubKey);
		m_publicKey = pubKey;
	}


	/**
	 * Signs the XML document after making it canonical and returns a String
	 * representation of the signed document
	 *
	 * @return Signed XML structure
	 */
	public String signXML() throws SignatureException
	{
		// Algorithm:
		// 1. make document canonical
		// 2. make message digest of the canonical xml
		// 3. put the <SignedInfo> envelope around the digest
		// 4. sign the <SignedInfo> node
		// 5. put the other templates around the whole thing
		try {
			// first, make our whole document canonical
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (makeCanonical(domDocument.getDocumentElement(), out, false) == -1
			) {
				throw new SignatureException("could not make canonical");
			}
			out.flush();

			byte[] hk = out.toByteArray();


			// signing implementation using bouncycastle
			// OK with JDK1.1.8 + Bouncycastle
			org.bouncycastle.crypto.digests.SHA1Digest sha1 = new SHA1Digest();
			byte[] digest = new byte[sha1.getDigestSize()];
			sha1.update(hk, 0, hk.length);
			sha1.doFinal(digest, 0);

			byte[] sigvalue = null;
			byte[] signedInfo = getSignedInfo(digest).getBytes();
			try {
				signer.update(signedInfo);
				sigvalue = signer.generateSignature();
			} catch (DataLengthException e) {
				e.printStackTrace();
			}

			// Only possible in JDK>= 1.3
			//	    MessageDigest sha1=MessageDigest.getInstance("SHA-1");
			//	    byte[] digest=sha1.digest(hk);
			//sig.update(getSignedInfo(digest).getBytes());
			//byte[] sigvalue = sig.sign();

			int index = xmlDocument.lastIndexOf("</");
			StringBuffer signedBuffer = new StringBuffer();
			signedBuffer.append(xmlDocument.substring(0, index));
			signedBuffer.append(templateSignature1);
			signedBuffer.append(getSignedInfo(digest));
			signedBuffer.append(templateSignature2);
			signedBuffer.append(new String(Base64.encode(sigvalue)));
			signedBuffer.append(templateSignature3);
			signedBuffer.append(xmlDocument.substring(index));

			return signedBuffer.toString();
		} catch (Exception e) {
			throw new SignatureException(e.getMessage());
		}
	}


	/**
	 * verifies the Signature contained in this document
	 */
	public boolean verifyXML() throws Exception
	{
		try {
			return verifyXML(domDocument.getDocumentElement());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Verifies a XML signature
	 */
	public boolean verifyXML(Node n) throws SignatureException
	{
		//FIXME: Thread Safe ???
		//
		// Algorithm: 
		// 1. Make SignedInfo canonical 
		// 2. Check Signature 
		// 3. Make reference message digest 
		// 4. check wether reference digest is ok

		try {
			if (n == null) {
				throw new SignatureException("Root Node is null");
			}

			Element root = (Element) n;
			NodeList nl = root.getElementsByTagName("Signature");
			if (nl.getLength() < 1) {
				throw new SignatureException("No <Signature> Tag");
			}

			Element signature = (Element) nl.item(0);
			root.removeChild(signature);
			nl = signature.getElementsByTagName("SignedInfo");
			if (nl.getLength() < 1) {
				throw new SignatureException("No <SignedInfo> Tag");
			}

			Element siginfo = (Element) nl.item(0);


			// 1. make SignedInfo Canonical....
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (makeCanonical(siginfo, out, false) == -1) {
				throw new SignatureException(
					"Could not make <SignedInfo> canonical"
				);
			}
			out.flush();


			// 2. Check Signature
			nl = signature.getElementsByTagName("SignatureValue");
			if (nl.getLength() < 1) {
				throw new SignatureException("No <SignatureValue> Tag");
			}

			Element signaturevalue = (Element) nl.item(0);
			String strSigValue = signaturevalue.getFirstChild().getNodeValue();
			byte[] rsbuff = Base64.decode(strSigValue.toCharArray());

			// check Signature value, using JDK1.1.8+bouncycastle
			synchronized (signer) {
				signer.update(out.toByteArray());
				if (!signer.verify(rsbuff)) {
					return false;
				}
			}

			// Only with JDK>=1.3
			//            synchronized(sig)
			// 		{
			// 		    sig.update(out.toByteArray());
			// 		    if(!sig.verify(rsbuff))
			// 			return false;
			// 		}

			// 3. Signature value ok, now make Reference msg digest....
			out.reset();
			if (makeCanonical(root, out, true) == -1) {
				throw new SignatureException("Could not make ROOT canonical");
			}
			out.flush();

			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			byte[] hk = out.toByteArray();
			byte[] digest = sha1.digest(hk);

			nl = siginfo.getElementsByTagName("DigestValue");
			if (nl.getLength() < 1) {
				throw new SignatureException("No <DigestValue> Tag");
			}

			String strDigest = nl.item(0).getFirstChild().getNodeValue();
			rsbuff = Base64.decode(strDigest.toCharArray());

			return MessageDigest.isEqual(rsbuff, digest);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SignatureException(e.getMessage());
		}
	}


	/**
	 * add the SignedInfo envelope around the digest
	 *
	 * @param digest The message digest binary value
	 * @return SignedInfo xml envelope with the Base64-encoded digest 
	 */
	private String getSignedInfo(byte[] digest)
	{
		String digestString = new String(Base64.encode(digest));
		return templateSignedInfo1 + digestString + templateSignedInfo2;
	}
}

/*
 Copyright (c) 2000, The JAP-Team
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.modes.CTSBlockCipher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.Base64;
import anon.util.XMLUtil;

final public class XMLEncryption
{
	private static final int SALT_SIZE = 20;
	private static final int MIN_ITERATIONS = 1000;
	private XMLEncryption()
	{
	}

	/**
	 * Encrypts an element and all its children using PKCS#12 and the given password.
	 * The encrypted Element replaces the plaintext Element inside its
	 * OwnerDocument.
	 *
	 * Note: The document Element cannot be encrypted using this function.
	 *
	 * For information on the XML encryption standard
	 * see also http://www.w3.org/TR/xmlenc-core/
	 *
	 * @author Bastian Voigt
	 * @param elemPlain Element to be encrypted (not the DocumentElement!)
	 * @param password String a password
	 * @return Element a pointer to the encrypted element
	 */
	public static Element encryptElement(Element elemPlain, String password) throws Exception
	{
		// generate random salt
		SecureRandom random = new SecureRandom();
		byte[] kSalt = new byte[SALT_SIZE];
		random.nextBytes(kSalt);

		// encrypt data
		byte[] barInput = null;
		byte[] barOutput = null;
		int len = 0;
		try
		{
			barInput = XMLUtil.XMLNodeToString(elemPlain).getBytes();
			len = barInput.length;
			barOutput = codeData(true, barInput, password, kSalt);
		}
		catch (Exception ex1)
		{
			throw new IOException("Exception while encrypting: " + ex1.toString());
		}

		// initialize XML-Encryption-Standard structure
		Document doc = elemPlain.getOwnerDocument();
		Node nodeParent = elemPlain.getParentNode();
		Element elemCrypt = doc.createElement("EncryptedData");
		elemCrypt.setAttribute("Type", "http://www.w3.org/2001/04/xmlenc#Element");
		elemCrypt.setAttribute("xmlns", "http://www.w3.org/2001/04/xmlenc#");
		Element elemAlgo = doc.createElement("EncryptionMethod");
		elemAlgo.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#aes-cts");
		elemCrypt.appendChild(elemAlgo);
		Element elemKeyInfo = doc.createElement("ds:KeyInfo");
		elemKeyInfo.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
		Element elemSalt = doc.createElement("ds:Salt");
		XMLUtil.setNodeValue(elemSalt, Base64.encodeBytes(kSalt));
		elemKeyInfo.appendChild(elemSalt);
		//Element elemLen = doc.createElement("ds:Length");
		//elemKeyInfo.appendChild(elemLen);
		//XMLUtil.setNodeValue(elemLen, Integer.toString(len));
		elemCrypt.appendChild(elemKeyInfo);

		Element elemCipher = doc.createElement("CipherData");
		elemCrypt.appendChild(elemCipher);
		Element elemValue = doc.createElement("CipherValue");
		elemCipher.appendChild(elemValue);

		// add ciphertext to dom document and remove plaintext
		XMLUtil.setNodeValue(elemValue, Base64.encodeBytes(barOutput));
		nodeParent.removeChild(elemPlain);
		nodeParent.appendChild(elemCrypt);

		return elemCrypt;
	}

	/**
	 * The part that is the same for encryption and decryption..
	 *
	 * @param encrypt boolean true=encrypt, false=decrypt
	 * @param barInput byte[] input plain or ciphertext
	 * @param password String password
	 * @param kSalt byte[] random salt
	 * @param length int length of the plaintext (to cut off padding)
	 * @throws Exception
	 * @return byte[]
	 * @author Bastian Voigt
	 */
	private static byte[] codeData(boolean encrypt,
								   byte[] barInput,
								   String password,
								   byte[] kSalt) throws Exception
	{
		PKCS12PBEParams kParams = new PKCS12PBEParams(kSalt, MIN_ITERATIONS);
		PKCS12ParametersGenerator paramGen =
			new PKCS12ParametersGenerator(new SHA1Digest());
		paramGen.init(paramGen.PKCS12PasswordToBytes(password.toCharArray()), // muss das hier auch wieder nachher genullt werden?
					  kParams.getIV(), // warum hier IV=Salt? ist in PKCS12.java auch so gemacht, kopiere das einfach ohne es zu verstehen :-)
					  kParams.getIterations().intValue());
		CipherParameters params = paramGen.generateDerivedParameters(128); // sind diese laengen so richtig und sinnvoll?

		BufferedBlockCipher cipher = /*new PaddedBufferedBlockCipher(*/
			new CTSBlockCipher(
			new AESFastEngine()
			); //);
		cipher.init(encrypt, params);

		// encrypt the data
		byte[] barOutput = new byte[cipher.getOutputSize(barInput.length)];
		int len = 0;
		if (barInput.length != 0)
		{
			len = cipher.processBytes(barInput, 0, barInput.length,
									  barOutput, 0);
		}
		len += cipher.doFinal(barOutput, len);

		return barOutput;
	}

	/**
	 * Decrypts an XML element
	 *
	 * @todo implement
	 * @param elemCrypt Element
	 * @param password String
	 * @return Element
	 * @author Bastian Voigt
	 */
	public static Element decryptElement(Element elemCrypt, String password) throws Exception
	{
		Document doc = elemCrypt.getOwnerDocument();
		Node nodeParent = elemCrypt.getParentNode();

		// get actual ciphertext from xml structure
		String strType = elemCrypt.getAttribute("Type");
		if (strType == null || !strType.equals("http://www.w3.org/2001/04/xmlenc#Element"))
		{
			throw new IOException("Wrong XML Format");
		}
		Element elemValue = (Element) XMLUtil.getFirstChildByName(elemCrypt, "CipherData");
		elemValue = (Element) XMLUtil.getFirstChildByName(elemValue, "CipherValue");
		byte[] barInput = Base64.decode(XMLUtil.parseNodeString(elemValue, null));

		// get salt from xml KeyInfo structure
		Element elemKeyInfo = (Element) XMLUtil.getFirstChildByName(elemCrypt, "ds:KeyInfo");
		Element elemSalt = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "ds:Salt");
		byte[] barSalt = Base64.decode(XMLUtil.parseNodeString(elemSalt, null));

		// get plaintext length
		//elemSalt = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "ds:Length");
		//int len = XMLUtil.parseNodeInt(elemSalt, 0);

		byte[] barOutput = null;
		Document doc2 = null;
		Element elemPlain = null;
		try
		{
			// decrypt
			barOutput = codeData(false, barInput, password, barSalt);

			// parse decrypted XML
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc2 = parser.parse(new ByteArrayInputStream(barOutput));

			elemPlain = (Element) XMLUtil.importNode(doc, doc2.getDocumentElement(), true);
		}
		catch (Exception ex)
		{
			throw new IOException("Exception while decrypting (maybe password wrong): " + ex.toString());
		}

		// remove ciphertext from document and add plaintext at the right position
		nodeParent.removeChild(elemCrypt);
		nodeParent.appendChild(elemPlain);

		return elemPlain;
	}

}

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
package payxml;

import java.math.BigInteger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.XMLUtil;

/** This class handels RSA Public Key represented in XML.
 * The corresponding XML struct is as follows:
 * <JapPublicKey version="1.0">
 * 	 <RSAKeyValue>
 *     <Modulus> Base64 encoded Modulus </Modulus>
 * 		<Exponent> Base 64 enocded Exponent </Exponent>
 * 	 </RSAKEyValue>
 * </JapPublicKey>
 */

public class XMLJapPublicKey extends XMLDocument
{
	//~ Instance fields ********************************************************

	//    private RSAPublicKey pubkey;
	private MyRSAPublicKey pubkey;

	//~ Constructors ***********************************************************

	public XMLJapPublicKey(MyRSAPublicKey key) throws Exception
	{
		pubkey = key;
		createXmlDocument();
	}

	public XMLJapPublicKey(byte[] data) throws Exception
	{
		setDocument(data);
		setPubKey();
	}

	public XMLJapPublicKey(Element elemKey) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,elemKey,true);
		m_theDocument.appendChild(n);
		setPubKey();
	}

	//~ Methods ****************************************************************

	public MyRSAPublicKey getRSAPublicKey()
	{
		return pubkey;
	}

	private void setPubKey() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("JapPublicKey"))
		{
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("RSAKeyValue");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);

		nl = element.getElementsByTagName("Exponent");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}

		Element exponent = (Element) nl.item(0);
		String chdata = ( (CharacterData) exponent.getFirstChild()).getData();
		BigInteger ExpBigInt = new BigInteger(
			Base64.decode(chdata)
			);

		nl = element.getElementsByTagName("Modulus");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}

		Element modulus = (Element) nl.item(0);
		chdata = ( (CharacterData) modulus.getFirstChild()).getData();

		BigInteger ModulusBigInt = new BigInteger(
			Base64.decode(chdata)
			);

		//KeyFactory keyfactory= KeyFactory.getInstance("RSA");
		//            pubkey = (RSAPublicKey)keyfactory.generatePublic(
		//                    new RSAPublicKeySpec(ModulusBigInt, ExpBigInt));
		pubkey = new MyRSAPublicKey(ModulusBigInt, ExpBigInt);
	}

	private void createXmlDocument() throws Exception
	{
		m_theDocument = null;
		Document doc = getDocumentBuilder().newDocument();
		Element elemRoot = doc.createElement("JapPublicKey");
		doc.appendChild(elemRoot);
		elemRoot.setAttribute("version", "1.0");
		Element elemKey = doc.createElement("RSAKeyValue");
		elemRoot.appendChild(elemKey);
		Element elemModulus = doc.createElement("Modulus");
		elemKey.appendChild(elemModulus);
		byte[] b = pubkey.getModulus().toByteArray();
		XMLUtil.setNodeValue(elemModulus, Base64.encodeBytes(b));
		Element elemExponent = doc.createElement("Exponent");
		elemKey.appendChild(elemExponent);
		b = pubkey.getPublicExponent().toByteArray();
		XMLUtil.setNodeValue(elemExponent, Base64.encodeBytes(b));
		m_theDocument=doc;
	}
}

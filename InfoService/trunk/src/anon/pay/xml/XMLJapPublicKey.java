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
package anon.pay.xml;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.IMyPublicKey;
import anon.crypto.MyDSAPublicKey;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/** This class handles RSA and DSA Public Keys represented in XML.
 * It is used mainly for formatting and parsing xml keys
 * The corresponding XML struct is as follows (for RSA):
 *
 * <JapPublicKey version="1.0">
 * 	 <RSAKeyValue>
 *     <Modulus> Base64 encoded Modulus </Modulus>
 * 		<Exponent> Base 64 enocded Exponent </Exponent>
 * 	 </RSAKeyValue>
 * </JapPublicKey>
 *
 */

public class XMLJapPublicKey implements IXMLEncodable //extends XMLDocument
{
	//~ Instance fields ********************************************************

	private IMyPublicKey m_publicKey;
	private static String ms_elemName = "JapPublicKey";

	public static String getXMLElementName()
	{
		return ms_elemName;
	}

	//~ Constructors ***********************************************************

	public XMLJapPublicKey(IMyPublicKey key) throws Exception
	{
		m_publicKey = key;
	}

	public XMLJapPublicKey(byte[] data) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new
			ByteArrayInputStream(data));
		setPubKey(doc.getDocumentElement());
	}

	public XMLJapPublicKey(String data) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setPubKey(doc.getDocumentElement());
	}

	public XMLJapPublicKey(Element elemKey) throws Exception
	{
		setPubKey(elemKey);
	}

	//~ Methods ****************************************************************

	public IMyPublicKey getPublicKey()
	{
		return m_publicKey;
	}

	/**
	 * Parses an XML JapPublicKey structure. Can handle RSA and DSA keys.
	 * @param elemKey Element the "JapPublicKey" tag
	 * @throws Exception
	 */
	private void setPubKey(Element elemKey) throws Exception
	{
		if (!elemKey.getTagName().equals(ms_elemName))
		{
			throw new Exception("XMLJapPublicKey wrong xml structure. Tagname is" + elemKey.getTagName());
		}
		Element elemRsa = (Element) XMLUtil.getFirstChildByName(elemKey, "RSAKeyValue");
		if (elemRsa != null)
		{
			Element elemMod = (Element) XMLUtil.getFirstChildByName(elemRsa, "Modulus");
			Element elemExp = (Element) XMLUtil.getFirstChildByName(elemRsa, "Exponent");
			BigInteger modulus = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elemMod, "")));
			BigInteger exponent = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elemExp, "")));
			m_publicKey = new MyRSAPublicKey(modulus, exponent);
			return;
		}
		Element elemDsa = (Element) XMLUtil.getFirstChildByName(elemKey, "DSAKeyValue");
		if (elemDsa != null)
		{
			Element elem = (Element) XMLUtil.getFirstChildByName(elemDsa, "P");
			BigInteger p = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elem, "")));
			elem = (Element) XMLUtil.getFirstChildByName(elemDsa, "Y");
			BigInteger y = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elem, "")));
			elem = (Element) XMLUtil.getFirstChildByName(elemDsa, "Q");
			BigInteger q = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elem, "")));
			elem = (Element) XMLUtil.getFirstChildByName(elemDsa, "G");
			BigInteger g = new BigInteger(Base64.decode(XMLUtil.parseNodeString(elem, "")));

			// is this really OK ????
			DSAPublicKeyParameters param = new DSAPublicKeyParameters(
				y,
				new DSAParameters(p, q, g)
				);
			m_publicKey = new MyDSAPublicKey(param);
			return;
		}
		throw new Exception("Wrong key format: Neither RSAKeyValue nor DSAKeyValue found!");
	}

	public Element toXmlElement(Document a_doc) //throws Exception
	{
		//Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elemRoot = a_doc.createElement(ms_elemName);
//		a_doc.appendChild(elemRoot);
		elemRoot.setAttribute("version", "1.0");
		Element elem = m_publicKey.toXmlElement(a_doc);

		elemRoot.appendChild(elem);

		return elemRoot;
	}

}

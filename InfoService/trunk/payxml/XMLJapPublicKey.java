package payxml;

import org.bouncycastle.crypto.params.*;

import org.w3c.dom.*;

import org.xml.sax.*;

import payxml.util.Base64;

import java.io.*;

import java.math.*;

import java.security.*;
import java.security.interfaces.*;


public class XMLJapPublicKey extends XMLDocument
{
	//~ Instance fields ********************************************************

	//    private RSAPublicKey pubkey;
	private RSAKeyParameters pubkey;

	//~ Constructors ***********************************************************

	public XMLJapPublicKey(RSAKeyParameters key)
	{
		pubkey = key;
		createXmlDocument();
	}


	public XMLJapPublicKey(byte[] data) throws Exception
	{
		setDocument(data);
		setPubKey();
	}

	//~ Methods ****************************************************************

	public RSAKeyParameters getRSAPublicKey()
	{
		return pubkey;
	}


	private void setPubKey() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("JapPublicKey")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("RSAKeyValue");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		nl = element.getElementsByTagName("Exponent");
		if (nl.getLength() < 1) {
			throw new Exception();
		}

		Element exponent = (Element) nl.item(0);
		CharacterData chdata = (CharacterData) exponent.getFirstChild();
		BigInteger ExpBigInt = new BigInteger(Base64.decode(
					chdata.getData().toCharArray()
				)
			);

		nl = element.getElementsByTagName("Modulus");
		if (nl.getLength() < 1) {
			throw new Exception();
		}

		Element modulus = (Element) nl.item(0);
		chdata = (CharacterData) modulus.getFirstChild();

		BigInteger ModulusBigInt = new BigInteger(Base64.decode(
					chdata.getData().toCharArray()
				)
			);

		//KeyFactory keyfactory= KeyFactory.getInstance("RSA");
		//            pubkey = (RSAPublicKey)keyfactory.generatePublic(
		//                    new RSAPublicKeySpec(ModulusBigInt, ExpBigInt));
		pubkey = new RSAKeyParameters(false, ModulusBigInt, ExpBigInt);
	}


	private void createXmlDocument()
	{
		if (pubkey == null) {
			return;
		}

		StringBuffer buffer = new StringBuffer(512);
		buffer.append("<JapPublicKey version=\"1.0\"><RSAKeyValue><Modulus>");
		buffer.append(Base64.encode(pubkey.getModulus().toByteArray()));
		buffer.append("</Modulus><Exponent>");
		buffer.append(Base64.encode(pubkey.getExponent().toByteArray()));
		buffer.append("</Exponent></RSAKeyValue></JapPublicKey>");
		xmlDocument = buffer.toString();
	}
}

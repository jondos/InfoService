package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;

import payxml.util.Base64;

import java.io.*;

import java.math.*;


public class XMLCostConfirmation extends XMLDocument
{
	//~ Instance fields ********************************************************

	private XMLCC cc;
	private byte[] digest;

	//~ Constructors ***********************************************************

	public XMLCostConfirmation(String data) throws Exception
	{
		this(data.getBytes());
	}


	public XMLCostConfirmation(byte[] data) throws Exception
	{
		setDocument(data);

		String ccString = xmlDocument.substring(xmlDocument.indexOf("<CC>"),
				xmlDocument.indexOf("</CC>") + 5
			);
		cc = new XMLCC(ccString.getBytes());

		String digestString = xmlDocument.substring(xmlDocument.indexOf(
					"<Digest>"
				) + 9, xmlDocument.indexOf("</Digest>")
			);
		digest = Base64.decode(digestString.toCharArray());
	}

	//~ Methods ****************************************************************

	public String getAIName()
	{
		return cc.getAIName();
	}


	public long getAccountNumber()
	{
		return cc.getAccountNumber();
	}


	public int getCosts()
	{
		return cc.getCosts();
	}


	public byte[] getDigest()
	{
		return getDigest();
	}


	public byte[] getHash()
	{
		return cc.getHash();
	}


	public int getTickPrice()
	{
		return cc.getTickPrice();
	}


	public String getXMLString(boolean withHead)
	{
		if (withHead) {
			return XML_HEAD + xmlDocument;
		} else {
			return xmlDocument;
		}
	}
}

package payxml;

import org.w3c.dom.*;
import org.xml.sax.*;

import payxml.util.Base64;

import java.io.*;
import java.math.*;
import java.security.*;
import java.security.interfaces.*;


/**
 * XML structure for a cost confirmation which is sent to the AI by the Jap
 */
public class XMLCC extends XMLDocument
{
	//~ Instance fields ********************************************************

	private String aiName;
	private byte[] hash;
	private int costs;
	private int tickPrice;
	private long accountNumber;

	//~ Constructors ***********************************************************

	public XMLCC(byte[] data) throws Exception
	{
		setDocument(data);

		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("CC")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("ID");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		aiName = chdata.getData();

		nl = element.getElementsByTagName("AN");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		accountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("C");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		costs = Integer.parseInt(chdata.getData());

		nl = element.getElementsByTagName("D");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		hash = Base64.decode(chdata.getData().toCharArray());

		nl = element.getElementsByTagName("w");
		if (nl.getLength() < 1) {
			throw new Exception();
		}

		Element exponent = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		tickPrice = Integer.parseInt(chdata.getData());
	}

	//~ Methods ****************************************************************

	public String getAIName()
	{
		return aiName;
	}


	public long getAccountNumber()
	{
		return accountNumber;
	}


	public int getCosts()
	{
		return costs;
	}


	public byte[] getHash()
	{
		return hash;
	}


	public int getTickPrice()
	{
		return tickPrice;
	}
}

package payxml;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;


/**
 * XML structure for a easy cost confirmation (without mircopayment function) which is sent to the AI by the Jap
 * @author Grischan Gl&auml;nzel
 */
public class XMLEasyCC extends XMLDocument
{
	//~ Public Fields  ******************************************************
		public static final String docStartTag = "<CC version=\"1.0\">";
		public static final String docEndTag = "</CC>";


	//~ Instance fields ********************************************************

	private String aiName;
	//private byte[] hash;
	private long transferredBytes;
	private long accountNumber;

	//~ Constructors ***********************************************************
	public XMLEasyCC(String aiName,long accountNumber,long transferred) throws Exception{

		this.aiName = aiName;
		this.transferredBytes = transferred;
		this.accountNumber = accountNumber;

		xmlDocument = docStartTag+"<AIName>"+aiName+"</AIName>"+
		"<Bytes>"+transferred+"</Bytes>"+
		"<Number>"+accountNumber+"</Number>"+docEndTag;
		setDocument(xmlDocument);

	}


	public XMLEasyCC(byte[] data) throws Exception
	{
		setDocument(data);

		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("CC")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("AIName");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		aiName = chdata.getData();

		nl = element.getElementsByTagName("Number");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		accountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("Bytes");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		transferredBytes = Integer.parseInt(chdata.getData());

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


	public long getTransferredBytes()
	{
		return transferredBytes;
	}

	public void addTransferredBytes(long plusBytes)
	{
			transferredBytes += plusBytes;
	}

}

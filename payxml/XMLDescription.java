package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;


public class XMLDescription extends XMLDocument
{
	//~ Constructors ***********************************************************

	public XMLDescription(byte[] data) throws Exception
	{
		xmlDocument = new String(data);
		setDocument(xmlDocument);
	}


	public XMLDescription(String data) throws Exception
	{
		xmlDocument = "<Description>" + data + "</Description>";
		setDocument(xmlDocument);
	}

	//~ Methods ****************************************************************

	public String getDescription() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("Description")) {
			throw new Exception();
		}

		CharacterData chdata = (CharacterData) element.getFirstChild();

		return chdata.getData();
	}
}

package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;

import payxml.util.Base64;


public class XMLChallenge extends XMLDocument
{
	//~ Constructors ***********************************************************

	public XMLChallenge(String xml) throws Exception
	{
		xmlDocument = xml;
		setDocument(xml);
	}


	public XMLChallenge(byte[] data) throws Exception
	{
		xmlDocument = "<Challenge><DontPanic>" +
			new String(Base64.encode(data)) + "</DontPanic></Challenge>";
		setDocument(xmlDocument);
	}

	//~ Methods ****************************************************************

	public byte[] getChallenge() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("Challenge")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("DontPanic");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		String challenge = "<" + element.getTagName() + ">" + chdata.getData() +
			"</" + element.getTagName() + ">";

		return challenge.getBytes();
	}
}

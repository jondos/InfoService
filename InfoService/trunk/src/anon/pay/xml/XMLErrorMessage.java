package anon.pay.xml;

import anon.util.IXMLEncodable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.XMLUtil;

public class XMLErrorMessage implements IXMLEncodable
{
	private String m_strErrMsg;
	public XMLErrorMessage(String message)
	{
		m_strErrMsg = message;
	}
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("ErrorMessage");
		XMLUtil.setNodeValue(elemRoot, m_strErrMsg);
		return elemRoot;
	}

}

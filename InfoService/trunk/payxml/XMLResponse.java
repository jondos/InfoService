package payxml;

import payxml.util.Base64;

import org.w3c.dom.*;
import org.xml.sax.*;


public class XMLResponse extends XMLDocument
{
    byte[] response;

    public XMLResponse(String xml) throws Exception
    {
        setDocument(xml);
        setResponse();
    }

    public XMLResponse(byte[] data)
    {
        response=data;
        createXmlDocument();
    }

    private void setResponse() throws Exception
    {
        Element element = domDocument.getDocumentElement();
        if (!element.getTagName().equals("Response"))
            throw new Exception();
        CharacterData chdata = (CharacterData)element.getFirstChild();
        response=Base64.decode(chdata.getData().toCharArray());
    }

    public byte[] getResponse()
    {
        return response;
    }

    private void createXmlDocument()
    {
        StringBuffer buffer = new StringBuffer(512);
        buffer.append("<Response>");
        buffer.append(Base64.encode(response));
        buffer.append("</Response>");
        xmlDocument=buffer.toString();
    }
}

package anon.pay.xml;

public class XMLCloseAck
{
	final static private byte[] XML_CLOSE_ACK="<?xml version=\"1.0\" ?>\n<CloseAck/>".getBytes();

	public static byte[] getXMLByteArray()
		{
			return XML_CLOSE_ACK;
		}
}

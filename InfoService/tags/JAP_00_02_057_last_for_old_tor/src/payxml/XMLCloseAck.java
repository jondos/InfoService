package payxml;

public class XMLCloseAck
{
	final static private byte[] XML_CLOSE_ACK="<?xml version=\"1.0\" ?><CloseAck/>".getBytes();

	public static byte[] getXMLByteArray()
		{
			return XML_CLOSE_ACK;
		}
}

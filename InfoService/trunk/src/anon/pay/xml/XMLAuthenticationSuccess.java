package anon.pay.xml;

public class XMLAuthenticationSuccess
{
	final static private byte[] XML_AUTH_SUCCESS=("<?xml version=\"1.0\" ?>"+
		"<Authentication>Success</Authentication>").getBytes();

	public static byte[] getXMLByteArray()
		{
			return XML_AUTH_SUCCESS;
		}
}

package anon.tor.tinytls;

/**
 * <p>†berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Organisation: </p>
 * @author not attributable
 * @version 1.0
 */

public class TLSRecord
{
	public int m_Type;
	public int m_dataLen;
	public byte[] m_Data;
	public byte[] m_Header;

	public TLSRecord()
    {
		m_Header=new byte[5];
		m_Header[1]=TinyTLS.PROTOCOLVERSION[0];
		m_Header[2]=TinyTLS.PROTOCOLVERSION[1];
		m_Data=new byte[0xFFFF];
		m_dataLen=0;
    }

	public void setType(int type)
	{
		m_Type=type;
		m_Header[0]=(byte)(type&0x00FF);
	}

	public void setLength(int len)
	{
		m_dataLen=len;
		m_Header[3]=(byte)((len>>8)&0x00FF);
		m_Header[4]=(byte)((len)&0x00FF);
	}


}

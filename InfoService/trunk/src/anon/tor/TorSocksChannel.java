/*
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;
import anon.tor.util.helper;

/**
 * @author stefan
 * @todo redesign this very ugly design!!!
 */
public class TorSocksChannel extends TorChannel
{

//	private boolean socksReplyGenerated = false;

	private final static int SOCKS_WAIT_FOR_METHODS = 1;
	private final static int SOCKS_WAIT_FOR_REQUEST = 2;
	private final static int SOCKS_WAIT_FOR_CONNECTED = 3;
	private final static int DATA_MODE = 4;
	private int m_status;
	private byte[] m_data; //buffer for socks protocol headers
	private Tor m_Tor;

	public TorSocksChannel(Tor tor) throws IOException
	{
		m_status = SOCKS_WAIT_FOR_METHODS;
		m_data = null;
		m_Tor = tor;
	}

	/** Called if some bytes should be send over this Sock channel
	 *
	 */
	//Depending on the current stat of the protocol we have to proccess protocols headers. After
	//this we can transparently forward the data
	protected void send(byte[] arg0, int len) throws IOException
	{
		switch (m_status)
		{
			case SOCKS_WAIT_FOR_METHODS:
			{ //initial state
				if (arg0 != null && len > 0)
				{
					m_data = helper.conc(m_data, arg0, len);
				}
				if (m_data.length > 2)
				{
					//m_data[0]=Version (0x05)
					//m_data[1]=Number of Methods
					//m_data[2-x]=Methods
					if (m_data[0] != 5)
					{
						close();
						throw new IOException("Wrong Sock Protocol number");
					}
					int nrOfMethods = (m_data[1] & 0xFF);
					int length = nrOfMethods + 2;
					if (m_data.length >= length)
					{
						boolean methodFound = false;
						byte[] socksAnswer = null;
						for (int i = 0; i < nrOfMethods; i++)
						{
							if (m_data[i + 2] == 0)
							{
								methodFound = true;
								socksAnswer = new byte[]{0x05, 0x00};
								m_status = SOCKS_WAIT_FOR_REQUEST;
								break;
							}
						}
						if (!methodFound)
						{
							socksAnswer = new byte[]
								{
								0x05, (byte) 0xFF};
						}
						super.recv(socksAnswer, 0, socksAnswer.length);
						if (!methodFound)
						{
							//todo close this channel
							break;
						}
						m_data = helper.copybytes(m_data, length, m_data.length - length);
						if (m_data.length > 0)
						{
							send(null, 0);
						}
					}

				}
				break;
			}
			case SOCKS_WAIT_FOR_REQUEST:
			{ //waiting for a request....
				if (arg0 != null && len > 0)
				{
					m_data = helper.conc(m_data, arg0, len);
				}
				if (m_data.length > 6)
				{
					byte[] socksAnswer = null;
					int port = 0;
					String addr = null;
					int requestType = m_data[1];
					int addrType = m_data[3];
					int consumedBytes = 0;
					if (requestType != 1) //connect request type==1
					{
						//todo: close etc.
						//command not supported
						socksAnswer = helper.conc(new byte[]
												  {0x05, 0x07, 0x00}
												  , helper.copybytes(this.m_data, 3, this.m_data.length - 3));
						m_data = null;
						super.recv(socksAnswer, 0, socksAnswer.length);
						break;
					}
					switch (addrType)
					{
						case 1: //IP V4
						{
							if (m_data.length > 9)
							{
								addr = Integer.toString(m_data[4] & 0xFF) + "." +
									Integer.toString(m_data[5] & 0xFF) + "." +
									Integer.toString(m_data[6] & 0xFF) + "." +
									Integer.toString(m_data[7] & 0xFF);
								port = ( (m_data[8] & 0xFF) << 8) | (m_data[9] & 0xFF);
								consumedBytes = 10;
							}
							break;
						}
						case 3: //Domain Name
						{
							int length = m_data[4] & 0xFF;
							if (m_data.length >= (7 + length))
							{
								addr = new String(m_data, 5, length);
								port = ( (m_data[5 + length] & 0xFF) << 8) | (m_data[6 + length] & 0xFF);
								consumedBytes = length + 7;
							}
							break;
						}
						default:
						{
							//addresstype not supportet
							socksAnswer = helper.conc(new byte[]
								{0x05, 0x08, 0x00}
								, helper.copybytes(m_data, 3, m_data.length - 3));
	 						 super.recv(socksAnswer, 0, socksAnswer.length);
							m_data = null;
							//todo close
						}
					}

					if (addr != null) //we found an address
					{
						//	connect
						Circuit circ = m_Tor.createNewActiveCircuit(addr, port);
						circ.connectChannel(this, addr, port);
						socksAnswer = helper.conc(new byte[]
								{0x05, 0x00, 0x00}
								, helper.copybytes(m_data, 3, consumedBytes-3));
	 	 				super.recv(socksAnswer, 0, socksAnswer.length);
						m_data = helper.copybytes(m_data, consumedBytes, m_data.length - consumedBytes);
						m_status=SOCKS_WAIT_FOR_CONNECTED;
						if (m_data.length > 0)
						{
							send(m_data, m_data.length);
							m_data=null;
						}
						break;
					}
				}
				break;
			}
			case DATA_MODE:
			case SOCKS_WAIT_FOR_CONNECTED:
			{
				super.send(arg0, len);
				break;
			}
			default:
			{
				throw new IOException("illegal status");
			}
		}

	}

/*	protected void recv(byte[] buff, int pos, int len) throws IOException
	{

		System.out.println("hier");
	}
*/
}

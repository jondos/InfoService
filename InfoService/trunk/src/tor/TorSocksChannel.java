/*
 * Created on May 9, 2004
 */
package tor;

import java.io.IOException;
import java.net.InetAddress;

import tor.cells.RelayCell;
import tor.util.helper;


/**
 * @author stefan
 */
public class TorSocksChannel extends TorChannel
{

	// used for exchange data between is and os, when a socks connection is established
	private byte[] socksAnswer = null;
	private boolean socksReplyGenerated = false;

	private final static int SOCKS_WAIT_FOR_METHODS = 1;
	private final static int SOCKS_WAIT_FOR_REQUEST = 2;
	private final static int DATA_MODE = 3;
	private int status;
	private byte[] data; //buffer for socks protocol headers

	/**
	 * constructor
	 * @param streamID
	 * streamID for this stream
	 * @param circuit
	 * circuit where this stream belongs to
	 * @throws IOException
	 */
	public TorSocksChannel(int streamID, Circuit circuit) throws IOException
	{
		super(streamID, circuit);
		status = SOCKS_WAIT_FOR_METHODS;
		data = new byte[0];
	}

	protected void send(byte[] arg0, int len) throws IOException
	{
		switch (status)
		{
			case SOCKS_WAIT_FOR_METHODS:
			{
				this.data = helper.conc(this.data, arg0,len);
				if (this.data.length > 2)
				{
					int length = (this.data[1] & 0xFF) + 2;
					if (this.data.length >= length)
					{

						if (this.data[0] != 5)
						{
							close();
							throw new IOException("Wrong Sock Protocol number");
						}

						boolean methodFound = false;
						byte[] socksAnswer = null;
						for (int i = 0; i < (this.data[1] & 0xFF); i++)
						{
							if ( (this.data[i + 2] == 0) && (!methodFound))
							{
								methodFound = true;
								socksAnswer = new byte[]
									{
									0x05, 0x00};
								this.status = SOCKS_WAIT_FOR_REQUEST;
							}
						}
						if (!methodFound)
						{
							socksAnswer = new byte[]
								{
								0x05, (byte) 0xFF};
						}
						recv(socksAnswer, 0, socksAnswer.length);
						this.data = helper.copybytes(this.data, length, this.data.length - length);
						if(data.length>0)
							send(data,0);
					}

				}
				break;
			}
			case SOCKS_WAIT_FOR_REQUEST:
			{
				this.data = helper.conc(this.data, arg0,len);
				if (this.data.length > 6)
				{
					InetAddress addr = null;
					int port = 0;
					switch (this.data[3])
					{
						case 1:
						{
							if (this.data.length > 9)
							{
								addr = InetAddress.getByName("" + (this.data[4] & 0xFF) + "." +
									(this.data[5] & 0xFF) + "." + (this.data[6] & 0xFF) + "." +
									(this.data[7] & 0xFF));
								port = ( (this.data[8] & 0xFF) << 8) | (this.data[9] & 0xFF);
							}
							break;
						}
						case 3:
						{
							int length = this.data[4] & 0xFF;
							if (this.data.length >= (7 + length))
							{
								String s = "";
								for (int i = 0; i < length; i++)
								{
									s = s + (char)this.data[5+i];
								}
								addr = InetAddress.getByName(s);
								port = ( (this.data[5 + length] & 0xFF) << 8) | (this.data[6 + length] & 0xFF);
							}
							break;
						}
						default:
						{
							//addresstype not supportet
							socksAnswer = helper.conc(new byte[]
								{0x05, 0x08, 0x00}
								, helper.copybytes(this.data, 3, this.data.length - 3));
							this.data = new byte[0];
						}
					}

					if (addr != null)
					{
						if (this.data[0] != 5)
						{
							socksAnswer = helper.conc(new byte[]
								{0x05, 0x01, 0x00}
								, helper.copybytes(this.data, 3, this.data.length - 3));
							this.data = new byte[0];
						}
						else
						{
							//what command has been send
							switch (this.data[1])
							{
								//	connect
								case 1:
								{
									this.connect(addr,port);

									break;
								}
								//other commands are not allowed
								default:
								{
									//command not supported
									socksAnswer = helper.conc(new byte[]
										{0x05, 0x07, 0x00}
										, helper.copybytes(this.data, 3, this.data.length - 3));
									this.data = new byte[0];
								}
							}
						}

					}
				}
				recv(socksAnswer, 0, socksAnswer.length);
				break;
			}
			case DATA_MODE:
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
	
	public void dispatchCell(RelayCell cell)
	{
		if(this.status == SOCKS_WAIT_FOR_REQUEST)
		{
			if(cell!=null)
			{
				if(cell.getRelayCommand()==RelayCell.RELAY_CONNECTED)
				{
					socksAnswer = helper.conc(new byte[]
						{0x05, 0x00, 0x00}
						, helper.copybytes(this.data, 3, this.data.length - 3));
					try
					{
						recv(socksAnswer,0,socksAnswer.length);
					} catch (Exception ex)
					{
						this.error = true;
						//TODO : Handle Exception
					}
					this.data = new byte[0];
					this.status = DATA_MODE;
					this.opened = true;
					return;
				}
			}
			socksAnswer = helper.conc(new byte[]
				{0x05, 0x04, 0x00}
				, helper.copybytes(this.data, 3, this.data.length - 3));
			this.data = new byte[0];
			this.close();

			
		}
		super.dispatchCell(cell);
	}

}

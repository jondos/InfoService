/*
 * Created on May 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import tor.cells.Cell;
import tor.cells.RelayCell;
import tor.util.helper;

import anon.AnonChannel;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
								port = ( (this.data[7] & 0xFF) << 8) | (this.data[8] & 0xFF);
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
									byte[] payload = ("" + addr.getHostAddress() + ":" + port).getBytes();
									payload = helper.conc(payload, new byte[1]);
									RelayCell cell = new RelayCell(circuit.getCircID(), RelayCell.RELAY_BEGIN,
										streamID, payload);
									Cell c;
									try
									{
										circuit.send(cell);
										c = circuit.read(streamID);
									}
									catch (Exception ex)
									{
										throw new IOException(ex.getMessage());
									}

									if (c instanceof RelayCell)
									{
										cell = (RelayCell) c;
										if (cell.getRelayCommand() == RelayCell.RELAY_CONNECTED)
										{
											socksAnswer = helper.conc(new byte[]
												{0x05, 0x00, 0x00}
												, helper.copybytes(this.data, 3, this.data.length - 3));
											recv(socksAnswer,0,socksAnswer.length);
											this.data = new byte[0];
											this.status = DATA_MODE;
											start();
											return;
										}
									}
									socksAnswer = helper.conc(new byte[]
										{0x05, 0x04, 0x00}
										, helper.copybytes(this.data, 3, this.data.length - 3));
									this.data = new byte[0];
									this.close();

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
}

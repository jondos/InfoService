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
public class TorChannel implements AnonChannel {

	private final static int MAX_CELL_DATA = 498;
	
	// used for exchange data between is and os, when a socks connection is established
	private byte[] socksAnswer = null;
	private boolean socksReplyGenerated = false;

	private Circuit circuit;
	private int streamID;
	private TORInputStream is;
	private TOROutputStream os;

	class TOROutputStream extends OutputStream
	{
		private final static int SOCKS_WAIT_FOR_METHODS = 1;
		private final static int SOCKS_WAIT_FOR_REQUEST = 2;
		private final static int DATA_MODE = 3;
		
		private byte[] data;
		private int status;
		
		public TOROutputStream()
		{
			super();
			this.status =  DATA_MODE;
			this.data = new byte[0];
		}
		
		public TOROutputStream(int type)
		{
			this();
			switch(type)
			{
				case SOCKS :
				{
					this.status = SOCKS_WAIT_FOR_METHODS;
				}
			}
		}

		public void write(int arg0) throws IOException 
		{
			this.write(helper.inttobyte(arg0,1));
		}

		public void write(byte[] arg0) throws IOException
		{
			switch(status)
			{
				case SOCKS_WAIT_FOR_METHODS :
				{
					this.data = helper.conc(this.data,arg0);
					if(this.data.length>2)
					{
						int length = (this.data[1] & 0xFF) + 2;
						if(this.data.length >= length)
						{
						
							if(this.data[0]!=5)
							{
								this.close();
							} else
							{
								boolean methodFound = false;
								for(int i=0;i<(this.data[1] & 0xFF);i++)
								{
									if((this.data[i+2]==0) &&(!methodFound))
									{
										methodFound = true;
										socksAnswer = new byte[]{0x05,0x00};
										this.status = SOCKS_WAIT_FOR_REQUEST;
									}
								}
								if(!methodFound)
								{
									socksAnswer = new byte[]{0x05,(byte)0xFF};
								}
								this.data = helper.copybytes(this.data,length,this.data.length-length);
							}
						
						}
						
					}
					break;
				}
				case SOCKS_WAIT_FOR_REQUEST :
				{
					this.data = helper.conc(this.data,arg0);
					if(this.data.length>6)
					{
						InetAddress addr = null;
						int port = 0;
						switch(this.data[3])
						{
							case 1 :
							{
								if(this.data.length>9)
								{
									addr = InetAddress.getByName(""+(this.data[3] & 0xFF)+"."+(this.data[4] & 0xFF)+"."+(this.data[5] & 0xFF)+"."+(this.data[6] & 0xFF));
									port = ((this.data[7] & 0xFF) << 8) |(this.data[8] & 0xFF);
								}
								break;
							}
							case 3:
							{
								int length = this.data[3] & 0xFF;
								if(this.data.length>(7+length))
								{
									String s ="";
									for(int i=0;i<length;i++)
									{
										s=s+(char)this.data[4];
									}
									addr = InetAddress.getByName(s);
									port = ((this.data[4+length] & 0xFF) << 8) |(this.data[5+length] & 0xFF);
								}
								break;
							}
							default :
							{
								//addresstype not supportet
								socksAnswer = helper.conc(new byte[]{0x05,0x08,0x00},helper.copybytes(this.data,3,this.data.length-3));
								this.data = new byte[0];
							}
						}
						
						if(addr!=null)
						{
							if(this.data[0]!=5)
							{
								socksAnswer = helper.conc(new byte[]{0x05,0x01,0x00},helper.copybytes(this.data,3,this.data.length-3));
								this.data = new byte[0];
							} else
							{
								//what command has been send
								switch(this.data[1])	
								{
									//	connect
									case 1 : 
									{
										byte[] payload = (""+addr.getHostAddress()+":"+port).getBytes();
										payload = helper.conc(data,new byte[1]);
										RelayCell cell = new RelayCell(circuit.getCircID(),RelayCell.RELAY_BEGIN,streamID,payload);
										Cell c;
										try
										{
											circuit.send(cell);
											c= circuit.read(streamID);
										} catch (Exception ex)
										{
											throw new IOException(ex.getMessage());
										}

										if(c instanceof RelayCell)
										{
											cell = (RelayCell)c;
											if(cell.getRelayCommand()==RelayCell.RELAY_CONNECTED)
											{
												socksAnswer = helper.conc(new byte[]{0x05,0x00,0x00},helper.copybytes(this.data,3,this.data.length-3));
												this.data = new byte[0];
												this.status = DATA_MODE;
												return;
											}
										} 
										socksAnswer = helper.conc(new byte[]{0x05,0x04,0x00},helper.copybytes(this.data,3,this.data.length-3));
										this.data = new byte[0];
										this.close();
																				
										break;
									}
									//other commands are not allowed
									default :
									{
										//command not supported
										socksAnswer = helper.conc(new byte[]{0x05,0x07,0x00},helper.copybytes(this.data,3,this.data.length-3));
										this.data = new byte[0];
									}
								}
							}

						}
					}
					break;
				}
				case DATA_MODE :
				{
					this.sendData(arg0);
					break;
				}
				default :
				{
					throw new IOException("illegal status");
				}
			}
		}
		
		private void sendData(byte[] arg0) throws IOException
		{
			byte[] b = arg0;
			RelayCell cell;
			while(b.length!=0)
			{
				if(b.length>MAX_CELL_DATA)
				{
					cell = new RelayCell(circuit.getCircID(),RelayCell.RELAY_DATA,streamID,helper.copybytes(b,0,MAX_CELL_DATA));
					b = helper.copybytes(b,MAX_CELL_DATA,b.length-MAX_CELL_DATA);
				} else
				{
					cell = new RelayCell(circuit.getCircID(),RelayCell.RELAY_DATA,streamID,b);
					b = new byte[0];
				}
				try
				{
					circuit.send(cell);
				} catch(Exception ex)
				{
					throw new IOException(ex.getMessage());
				}
			}
		}

	}

	class TORInputStream extends InputStream
	{
		private final static int SOCKS_MODE = 1;
		private final static int DATA_MODE = 2;
		
		private byte[] buffer = new byte[0];
		private int status;
		
		public TORInputStream()
		{
			super();
			this.status = DATA_MODE;
		}
		
		public TORInputStream(int type)
		{
			this();
			switch(type)
			{
				case SOCKS :
				{
					this.status = SOCKS_MODE;
					break;
				}
			}
		}
		
		public int read() throws IOException 
		{
			switch(status)
			{
				case SOCKS_MODE :
				{
					if(socksAnswer == null)
					{
						return -1;
					} 
					if(socksAnswer.length==0)
					{
						return 0;
					}
					int i = socksAnswer[0] & 0xFF;
					socksAnswer = helper.copybytes(socksAnswer,1,socksAnswer.length-1);
					if((socksAnswer.length==0)&&(socksReplyGenerated))
					{
						this.status = DATA_MODE;
					}
					return i;
				}
				case DATA_MODE :
				{
					if(buffer.length==0)
					{
						try
						{
							Cell cell = circuit.read(streamID);
							buffer = helper.copybytes(cell.getPayload(),9,cell.getPayload().length-9);
						} catch(Exception ex)
						{
							throw new IOException(ex.getMessage());
						}
					}
					int i = buffer[0] & 0xFF;
					buffer = helper.copybytes(buffer,1,buffer.length-1);
					return i;
				}
				default :
				{
					throw new IOException("illegal status");
				}
			}
		}
		
	}
	
	public TorChannel(int streamID,Circuit circuit)
	{
		this.circuit = circuit;
		this.streamID = streamID;
		this.is = new TORInputStream();
		this.os = new TOROutputStream();
	}

	public TorChannel(int streamID,Circuit circuit,int type)
	{
		this.circuit = circuit;
		this.streamID = streamID;
		switch(type)
		{
			case SOCKS :
			{
				this.is = new TORInputStream(SOCKS);
				this.os = new TOROutputStream(SOCKS);
			}
			default :
			{
				this.is = new TORInputStream();
				this.os = new TOROutputStream();
			}
		}
	}

	public InputStream getInputStream() {
		return this.is;
	}

	public OutputStream getOutputStream() {
		return this.os;
	}

	public void close() {

	}

}

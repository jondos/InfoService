/*
 Copyright (c) 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
/*
 * based on tinySSL
 * (http://www.ritlabs.com/en/products/tinyweb/tinyssl.php)
 *
 */
package anon.tor.tinytls;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;
import anon.crypto.JAPCertificate;
import anon.tor.util.helper;
import anon.util.Base64;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.EOFException;
import anon.crypto.*;
/**
 * @author stefan
 *
 *TinyTLS
 */
public class TinyTLSServerSocket extends Socket
{

	/**
	 * SSL VERSION :
	 *
	 * 3.1 for TLS
	 */
	public static byte[] PROTOCOLVERSION = new byte[]
		{
		0x03, 0x01};

	private static int PROTOCOLVERSION_SHORT = 0x0301;
	private Vector m_supportedciphersuites;
	private CipherSuite m_selectedciphersuite = null;

	private Socket socket;
	private TLSInputStream m_istream;
	private TLSOutputStream m_ostream;

	private boolean m_handshakecompleted;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;

	private JAPCertificate m_servercertificate;
	private IMyPrivateKey m_privatekey;
	private MyDSAPrivateKey m_DSSKey;
	private MyRSAPrivateKey m_RSAKey;
	private JAPCertificate m_DSSCertificate;
	private JAPCertificate m_RSACertificate;

	private byte[] m_handshakemessages;
	private boolean m_encrypt;

	/**
	 *
	 * @author stefan
	 *
	 * TLSInputStream
	 */
	class TLSInputStream extends InputStream
	{

		private DataInputStream m_stream;
		private int m_aktPendOffset; //offest of next data to deliver
		private int m_aktPendLen; // number of bytes we could deliver imedialy
		private TLSRecord m_aktTLSRecord;
		private int m_ReadRecordState;
		final private static int STATE_START = 0;
		final private static int STATE_VERSION = 1;
		final private static int STATE_LENGTH = 2;
		final private static int STATE_PAYLOAD = 3;

		/**
		 * Constructor
		 * @param istream inputstream
		 */
		public TLSInputStream(InputStream istream)
		{
			m_aktTLSRecord = new TLSRecord();
			this.m_stream = new DataInputStream(istream);
			this.m_aktPendOffset = 0;
			this.m_aktPendLen = 0;
			m_ReadRecordState = STATE_START;
		}

		/**
		 * Reads one record if we need more data...
		 * Block until data is available...
		 * @return
		 */
		private synchronized void readRecord() throws IOException
		{
			if (m_ReadRecordState == STATE_START)
			{
				int contenttype;
				try
				{
					contenttype = m_stream.readByte();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}


				if (contenttype < 20 || contenttype > 23)
				{
					throw new TLSException("SSL Content typeProtocoll not supportet" + contenttype,2,10);
				}
				m_aktTLSRecord.setType(contenttype);
				m_ReadRecordState = STATE_VERSION;
			}
			if (m_ReadRecordState == STATE_VERSION)
			{
				int version;
				try
				{
					version = m_stream.readShort();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}
				if (version != PROTOCOLVERSION_SHORT)
				{
					throw new TLSException("Protocollversion not supportet" + version,2,70);
				}
				m_ReadRecordState = STATE_LENGTH;
			}
			if (m_ReadRecordState == STATE_LENGTH)
			{
				int length;
				try
				{
					length = m_stream.readShort();
				}
				catch (InterruptedIOException ioe)
				{
					ioe.bytesTransferred = 0;
					throw ioe;
				}
				m_aktTLSRecord.setLength(length);
				m_ReadRecordState = STATE_PAYLOAD;
				m_aktPendOffset = 0;
			}
			if (m_ReadRecordState == STATE_PAYLOAD)
			{
				int len = m_aktTLSRecord.m_dataLen - m_aktPendOffset;
				while (len > 0)
				{
					try
					{
						int ret = m_stream.read(m_aktTLSRecord.m_Data, m_aktPendOffset, len);
						if (ret < 0)
						{
							throw new EOFException();
						}
						len -= ret;
						m_aktPendOffset += ret;
					}
					catch (InterruptedIOException ioe)
					{
						m_aktPendOffset += ioe.bytesTransferred;
						ioe.bytesTransferred = 0;
						throw ioe;
					}
				}
				m_ReadRecordState = STATE_START;
				m_aktPendOffset = 0;
			}
		}

		public int read() throws IOException
		{
			byte[] b = new byte[1];
			if (read(b, 0, 1) < 1)
			{
				return -1;
			}
			return (b[0] & 0x00FF);
		}

		public int read(byte[] b) throws IOException
		{
			return read(b, 0, b.length);
		}

		public int read(byte[] b, int off, int len) throws IOException
		{
			while (m_aktPendLen < 1)
			{
				readRecord();
				try
				{
					switch (m_aktTLSRecord.m_Type)
					{
						case 21:
						{
							handleAlert();
						}
						case 23:
						{
							m_selectedciphersuite.decode(m_aktTLSRecord);
							m_aktPendOffset = 0;
							m_aktPendLen = m_aktTLSRecord.m_dataLen;
							break;
						}
						default:
						{
							throw new TLSException("Error while decoding application data",2,10);
						}
					}

				}
				catch (Throwable t)
				{
					t.printStackTrace();
					throw new TLSException("Exception by reading next TSL record: " + t.getMessage(),2,80);
				}
			}
			int l = Math.min(m_aktPendLen, len);
			System.arraycopy(m_aktTLSRecord.m_Data, m_aktPendOffset, b, off, l);
			m_aktPendOffset += l;
			m_aktPendLen -= l;
			return l;
		}

		public int available()
		{
			return m_aktPendLen;
		}

		/**
		 * handle alert message
		 * @throws IOException
		 */
		private void handleAlert() throws IOException
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TLS] ALERT!");
			if (m_handshakecompleted)
			{
				m_selectedciphersuite.decode(m_aktTLSRecord);
			}
			byte[] payload = m_aktTLSRecord.m_Data;
			switch (payload[0])
			{
				// warning
				case 1:
				{
					switch (payload[1])
					{
						case 0:
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "[RECIEVED-ALERT] TYPE=WARNING ; MESSAGE=CLOSE NOTIFY");
							break;
						}
						default:
						{
							throw new TLSException("TLSAlert detected!! Level : Warning - Description :" +
								payload[1]);
						}
					}
					break;
				}
				// fatal
				case 2:
				{
					throw new TLSException("TLSAlert detected!! Level : Fatal - Description :" + payload[1]);
				}
				default:
				{
					throw new TLSException("Unknown TLSAlert detected!!");
				}
			}
		}

		public void readClientHello() throws IOException
		{
			readRecord();
			//handshake message???
			if(m_aktTLSRecord.m_Type==22)
			{
				//client hello???
				if(m_aktTLSRecord.m_Data[0]==1)
				{
					//version = 3.1 (TLS) ???
					if(((m_aktTLSRecord.m_Data[4]<<8) | (m_aktTLSRecord.m_Data[5]))==PROTOCOLVERSION_SHORT)
					{
						//read client random
						m_clientrandom = new byte[32];
						System.arraycopy(m_aktTLSRecord.m_Data,6,m_clientrandom,0,32);
						//session id is not implemented
						if(m_aktTLSRecord.m_Data[38] != 0)
						{
							//maybe we implement it later
							throw new TLSException("Client wants to reuse another session, but this is not supportet yet",2,40);
						}
						try
						{
							//read ciphersuites
							int cslength = ((m_aktTLSRecord.m_Data[39] & 0xFF)<<8) | (m_aktTLSRecord.m_Data[40] & 0xFF);
							int aktpos = 41;
							while(((cslength+41)>aktpos) && (m_selectedciphersuite ==null))
							{
								for(int i=0;i<m_supportedciphersuites.size();i++)
								{
									CipherSuite cs = (CipherSuite)m_supportedciphersuites.elementAt(i);
									byte[] b = cs.getCipherSuiteCode();
									if(m_aktTLSRecord.m_Data[aktpos]==b[0]&&m_aktTLSRecord.m_Data[aktpos+1]==b[1])
									{
										m_selectedciphersuite = cs;
										if(cs.getKeyExchangeAlgorithm() instanceof DHE_DSS_Key_Exchange)
										{
											m_servercertificate = m_DSSCertificate;
											m_privatekey = m_DSSKey;
										} else if(cs.getKeyExchangeAlgorithm() instanceof DHE_DSS_Key_Exchange)
										{
											m_servercertificate = m_RSACertificate;
											m_privatekey = m_RSAKey;
										} else
										{
											LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[ERROR!!!] : KeyExchangeAlgorithm not supported yet.(should never happen)");
										}
										break;
									}
								}
								aktpos+=2;
							}
							if(m_selectedciphersuite==null)
							{
								throw new TLSException("no supported ciphersuite found",2,40);
							}
							//read compression
							aktpos = cslength+41;
							int complength = m_aktTLSRecord.m_Data[aktpos];
							if(complength==0)
							{
								throw new TLSException("no compressionalgorithm defined. you need at least one (for example no_compression)",2,50);
							}
							while(complength!=0)
							{
								aktpos++;
								//compression method : NO_COMPRESSION found
								if(m_aktTLSRecord.m_Data[aktpos]==0)
								{
									m_handshakemessages = helper.conc(m_handshakemessages,m_aktTLSRecord.m_Data,m_aktTLSRecord.m_dataLen);
									LogHolder.log(LogLevel.DEBUG,LogType.TOR,"[CLIENT HELLO RECIEVED]");
									return;
								}
								complength--;
							}
							throw new TLSException("no supportet compressionalgorithm found",2,40);
						} catch (ArrayIndexOutOfBoundsException ex)
						{
							throw new TLSException("client hello is not long enough",2,50);
						}
					} else
					{
						throw new TLSException("this Protocol is not supported",2,70);
					}
				}
			}
			throw new TLSException("Client hello expected but another message was recieved",2,10);
		}
		public void readClientKeyExchange() throws IOException
		{
			readRecord();
			try
			{
				if(m_aktTLSRecord.m_Data[0]==16)
				{
					int length = ((m_aktTLSRecord.m_Data[4] & 0xFF) << 8) |(m_aktTLSRecord.m_Data[5]);
					byte[] publickey = helper.copybytes(m_aktTLSRecord.m_Data,6,m_aktTLSRecord.m_dataLen-6);
					publickey = helper.conc(new byte[]{0},publickey);
					BigInteger dh_y = new BigInteger(publickey);
					m_selectedciphersuite.processClientKeyExchange(dh_y);
					m_handshakemessages = helper.conc(m_handshakemessages,m_aktTLSRecord.m_Data,m_aktTLSRecord.m_dataLen);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_KEY_EXCHANGE]");
					return;
				}
			} catch (ArrayIndexOutOfBoundsException ex)
			{
				throw new TLSException(ex.getLocalizedMessage(),2,50);
			}
			throw new TLSException("Client Key Exchange expected, but another messagetype was recieved",2,10);
		}

		public void readClientFinished() throws IOException
		{

			readRecord();
			if (m_aktTLSRecord.m_Type==20 && m_aktTLSRecord.m_dataLen == 1 && m_aktTLSRecord.m_Data[0] == 1)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CHANGE_CIPHER_SPEC]");
			} else
			{
				throw new TLSException("Change Cipher Spec expected",2,10);
			}
			m_encrypt = true;
			readRecord();
			m_selectedciphersuite.decode(m_aktTLSRecord);
			try
			{
				if(m_aktTLSRecord.m_Data[0]==20)
				{
					byte[] verify_data = helper.copybytes(m_aktTLSRecord.m_Data,4,12);
					m_selectedciphersuite.getKeyExchangeAlgorithm().processClientFinished(verify_data,m_handshakemessages);
					m_handshakemessages = helper.conc(m_handshakemessages,m_aktTLSRecord.m_Data,m_aktTLSRecord.m_dataLen);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_FINISHED]");
					return;
				}
			} catch (ArrayIndexOutOfBoundsException ex)
			{
				throw new TLSException(ex.getLocalizedMessage(),2,50);
			}
			throw new TLSException("Client Finish message expected, but another message was recieved",2,10);
		}

	}

	/**
	 *
	 * @author stefan
	 *
	 *TLSOutputStream
	 */
	class TLSOutputStream extends OutputStream
	{

		private DataOutputStream m_stream;
		private TLSRecord m_aktTLSRecord;

		/**
		 * Constructor
		 * @param ostream outputstream
		 */
		public TLSOutputStream(OutputStream ostream)
		{
			m_aktTLSRecord = new TLSRecord();
			m_stream = new DataOutputStream(ostream);
		}

		/**
		 *
		 */
		public void write(byte[] message) throws IOException
		{
			this.send(23, message, 0, message.length);
		}

		/**
		 *
		 */
		public void write(byte[] message, int offset, int len) throws IOException
		{
			this.send(23, message, offset, len);
		}

		/**
		 *
		 */
		public void write(int i) throws IOException
		{
			this.write(new byte[]
					   { (byte) i});
		}

		/**
		 *
		 */
		public void flush() throws IOException
		{
			this.m_stream.flush();
		}

		/**
		 * send a message to the server
		 * @param type type of the tls message
		 * @param message message
		 * @throws IOException
		 */
		private synchronized void send(int type, byte[] message, int offset, int len) throws IOException
		{
			System.arraycopy(message, offset, m_aktTLSRecord.m_Data, 0, len);
			m_aktTLSRecord.setLength(len);
			m_aktTLSRecord.setType(type);
			if (m_encrypt)
			{
				m_selectedciphersuite.encode(m_aktTLSRecord);
			}
			m_stream.write(m_aktTLSRecord.m_Header);
			m_stream.write(m_aktTLSRecord.m_Data, 0, m_aktTLSRecord.m_dataLen);
			m_stream.flush();
		}

		public void sendHandshake(int type, byte[] message) throws IOException
		{
			byte[] senddata = helper.conc(new byte[]{ (byte) type} ,
												helper.inttobyte(message.length, 3), message);
			send(22, senddata, 0, senddata.length);
			m_handshakemessages = helper.conc(m_handshakemessages, senddata);
		}


		public void sendServerHello() throws IOException
		{
			byte[] gmt_unix_time;
			byte[] random = new byte[28];
			byte[] sessionid = new byte[]{0x00};
			byte[] ciphersuite = m_selectedciphersuite.getCipherSuiteCode();
			byte[] compression = new byte[]{0x00};

			gmt_unix_time = helper.inttobyte( (System.currentTimeMillis() / (long) 1000), 4);
			Random rand = new Random(System.currentTimeMillis());
			rand.nextBytes(random);
			m_serverrandom = helper.conc(gmt_unix_time,random);

			byte[] message = helper.conc(PROTOCOLVERSION, m_serverrandom, sessionid, ciphersuite, compression);

			sendHandshake(2, message);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO]");
		}

		public void sendServerCertificate() throws IOException
		{
			byte[] cert = m_servercertificate.toByteArray();
			byte[] length = helper.inttobyte(cert.length,3);
			byte[] message = helper.conc(length,cert);
			length = helper.inttobyte(message.length,3);
			message = helper.conc(length,message);
			sendHandshake(11,message);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CERTIFICATE]");
		}

		public void sendServerKeyExchange() throws IOException
		{
			sendHandshake(12,m_selectedciphersuite.getKeyExchangeAlgorithm().generateServerKeyExchange(m_privatekey,m_clientrandom,m_serverrandom));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE]");
		}

		public void sendServerHelloDone() throws IOException
		{
			sendHandshake(14,new byte[]{});
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO_DONE]");
		}

		public void sendServerHandshakes() throws IOException
		{
			sendServerHello();
			sendServerCertificate();
			sendServerKeyExchange();
			sendServerHelloDone();
		}

		/**
		 * send a change cipher spec message
		 * now all client data will be encrypted
		 * @throws IOException
		 */
		public void sendChangeCipherSpec() throws IOException
		{
			m_encrypt = false;
			send(20, new byte[]{1}, 0, 1);
			m_encrypt = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CHANGE_CIPHER_SPEC]");
		}

		public void sendServerFinished() throws IOException
		{
			sendHandshake(20,m_selectedciphersuite.getKeyExchangeAlgorithm().calculateServerFinished(m_handshakemessages));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_FINISHED]");
		}
	}

	/**
	 *
	 * TinyTLS creates a TLS Connection to a server
	 *
	 * @param addr
	 * Server Address
	 * @param port
	 * Server's TLS Port
	 */
	public TinyTLSServerSocket(Socket socket) throws IOException
	{
		this.socket = socket;
		this.m_handshakecompleted = false;
		this.m_encrypt = false;
		this.m_supportedciphersuites = new Vector();
		m_istream = new TLSInputStream(socket.getInputStream());
		m_ostream = new TLSOutputStream(socket.getOutputStream());
		m_DSSCertificate = null;
		m_DSSKey = null;
		m_RSACertificate = null;
		m_RSAKey = null;
	}

	/**
	 * add a ciphersuites to TinyTLS
	 * @param cs ciphersuite you want to add
	 */
	public void addCipherSuite(CipherSuite cs)
	{
		if (!this.m_supportedciphersuites.contains(cs))
		{
			if(((cs.getKeyExchangeAlgorithm() instanceof DHE_DSS_Key_Exchange)&&m_DSSKey!=null&&m_DSSCertificate!=null)||
				((cs.getKeyExchangeAlgorithm() instanceof DHE_RSA_Key_Exchange)&&m_RSAKey!=null&&m_RSACertificate!=null))
			{
				this.m_supportedciphersuites.addElement(cs);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CIPHERSUITE ADDED] : "+cs.toString());
			} else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CIPHERSUITE NOT ADDED] : Please check if you've set the Certificate and the Private Key");
			}
		}
	}

	/**
	 * start the handshake
	 * @throws IOException
	 * @throws CertificateException
	 * @throws Exception
	 */
	public void startHandshake() throws IOException
	{
		if(m_supportedciphersuites.isEmpty())
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[NO_CIPHERSUITE_DEFINED] : using predefined");
			if(m_DSSKey!=null&&m_DSSCertificate!=null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[DSS FOUND] : using DSS Ciphersuites");
				this.addCipherSuite(new DHE_DSS_WITH_3DES_CBC_SHA());
				this.addCipherSuite(new DHE_DSS_WITH_AES_128_CBC_SHA());
				this.addCipherSuite(new DHE_DSS_WITH_DES_CBC_SHA());
			}
			if(m_RSAKey!=null&&m_RSACertificate!=null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[RSA FOUND] : using RSA Ciphersuites");
				this.addCipherSuite(new DHE_RSA_WITH_3DES_CBC_SHA());
				this.addCipherSuite(new DHE_RSA_WITH_AES_128_CBC_SHA());
				this.addCipherSuite(new DHE_RSA_WITH_DES_CBC_SHA());
			}
		}
		this.m_handshakemessages = new byte[]{};
		try
		{
			this.m_istream.readClientHello();
			this.m_ostream.sendServerHandshakes();
			this.m_istream.readClientKeyExchange();
			this.m_istream.readClientFinished();
			this.m_ostream.sendChangeCipherSpec();
			this.m_ostream.sendServerFinished();
		} catch (TLSException ex)
		{
			if(ex.Alert())
			{
				this.m_ostream.send(21,new byte[]{ex.getAlertLevel(),ex.getAlertDescription()},0,2);
			}
			throw ex;
		}
		this.m_handshakecompleted = true;
	}

	public void setDSSParameters(JAPCertificate cert,MyDSAPrivateKey key)
	{
		m_DSSCertificate = cert;
		m_DSSKey = key;
	}

	public void setRSAParameters(JAPCertificate cert,MyRSAPrivateKey key)
	{
		m_RSACertificate = cert;
		m_RSAKey = key;
	}

	public InputStream getInputStream()
	{
		return this.m_istream;
	}

	public OutputStream getOutputStream()
	{
		return this.m_ostream;
	}

	public void close() throws IOException
	{
		this.m_ostream.send(21,new byte[]{1,0},0,2);
	}

}

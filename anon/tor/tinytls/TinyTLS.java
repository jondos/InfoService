/*
 * Created on Mar 16, 2004
 *
 */
package anon.tor.tinytls;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Vector;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import anon.crypto.JAPCertificate;
import anon.tor.util.helper;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author stefan
 *
 *TinyTLS
 */
public class TinyTLS extends Socket
{

//TODO : DEBUGString entfernen
	String debugstring;
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

	//private Socket m_socket;
	private TLSInputStream m_istream;
	private TLSOutputStream m_ostream;

	private boolean m_handshakecompleted;
	private boolean m_serverhellodone;
	private boolean m_certificaterequested;

	private JAPCertificate m_servercertificate;
	private DHParameters m_dhparams;
	private DHPublicKeyParameters m_dhserverpub;
	private byte[] m_serverparams;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;
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
		private byte[] m_aktInput; //data that we have read but not delivered yet
		private int m_aktPendOffset; //offest of next data to deliver
		private int m_aktPendLen; // number of bytes we could deliver imedialy
		private TLSRecord m_aktTLSRecord;
		/**
		 * Constructor
		 * @param istream inputstream
		 */
		public TLSInputStream(InputStream istream)
		{
			m_aktTLSRecord = new TLSRecord();
			this.m_stream = new DataInputStream(istream);
			this.m_aktInput = null;
			this.m_aktPendOffset = 0;
			this.m_aktPendLen = 0;
		}

		/**
		 * Reads one record if we need more data...
		 * Block until data is available...
		 * @return
		 */
		private synchronized void readRecord() throws TLSException,IOException
		{
			int contenttype = m_stream.readByte();
			if (contenttype <20 || contenttype > 23)
			{
				throw new TLSException("SSL Content typeProtocoll not supportet" + contenttype);
			}
			int version = m_stream.readShort();
			if (version != PROTOCOLVERSION_SHORT)
			{
				throw new TLSException("Protocollversion not supportet" + version);
			}
			m_aktTLSRecord.setType(contenttype);
			int length = m_stream.readShort();
			m_aktTLSRecord.setLength(length);
			m_stream.readFully(m_aktTLSRecord.m_Data, 0, length);
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
			while (this.m_aktPendLen < 1)
			{
				try
				{
					readRecord();
					switch (m_aktTLSRecord.m_Type)
					{
						case 23:
						{
							m_selectedciphersuite.decode(m_aktTLSRecord);
							m_aktPendOffset = 0;
							m_aktPendLen = m_aktTLSRecord.m_dataLen;
							break;
						}
						case 21:
						{
							handleAlert();
							break;
						}
						default:
						{
							throw new TLSException("Error while decoding application data");
						}
					}

				}
				catch (Throwable t)
				{
					t.printStackTrace();
					throw new IOException("Exception by reading next TSL record: " + t.getMessage());
				}
			}
			int l = Math.min(this.m_aktPendLen, len);
			System.arraycopy(m_aktTLSRecord.m_Data, m_aktPendOffset, b, off, l);
			this.m_aktPendOffset += l;
			this.m_aktPendLen -= l;
			return l;
		}

		public int available()
		{
			return this.m_aktPendLen;
		}

		/**
		 * process server hello
		 * @param bytes server hello message
		 * @throws IOException
		 */
		private void gotServerHello(TLSRecord msg) throws IOException
		{
			byte[] b;
			//byte[] sslversion = helper.copybytes(bytes, 0, 2);
			int aktIndex=4;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] SSLVERSION :" + msg.m_Data[aktIndex] + "." +msg.m_Data[aktIndex+1]);
			if ( (msg.m_Data[aktIndex] != PROTOCOLVERSION[0]) || (msg.m_Data[aktIndex+1] != PROTOCOLVERSION[1]))
			{
				throw new TLSException("Server replies with wrong protocoll");
			}
			m_serverrandom = helper.copybytes(msg.m_Data, aktIndex+2, 32);
			//debugstring = "";
			//for (int i = 0; i < 32; i++)
			//{
			//	debugstring += "" + i + ":" + (random[i] & 0xFF) + " ";
			//}
			//LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO] RANDOMBYTES :" + debugstring);
			byte[] sessionid = new byte[0];
			int sessionidlength = msg.m_Data[aktIndex+34];
			if (sessionidlength > 0)
			{
				sessionid = helper.copybytes(msg.m_Data, aktIndex+35, sessionidlength);
			}
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] Laenge der SessionID : " + sessionidlength);
			byte[] ciphersuite = helper.copybytes(msg.m_Data, aktIndex+35 + sessionidlength, 2);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_HELLO] Ciphersuite : " + ciphersuite[0] + " " + ciphersuite[1]);
			byte[] compression = helper.copybytes(msg.m_Data, aktIndex+37 + sessionidlength, 1);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO] Kompression : " + compression[0]);
			//Iterator i = supportedciphersuites.iterator();
			CipherSuite cs = null;
			for (int i = 0; i < m_supportedciphersuites.size(); i++)
			{
				cs = (CipherSuite) m_supportedciphersuites.elementAt(i);
				b = cs.getCipherSuiteCode();
				if ( (b[0] == ciphersuite[0]) && (b[1] == ciphersuite[1]))
				{
					break;
				}
				cs = null;
			}
			if (cs == null)
			{
				throw new TLSException("Unsupported Ciphersuite selected");
			}
			m_selectedciphersuite = cs;
			m_supportedciphersuites = null;
		}

		/**
		 * process server certificate
		 * @param bytes server certificate message
		 * @throws IOException
		 * @throws CertificateException
		 */
		private void gotCertificate(byte[] bytes,int offset,int len) throws IOException
		{
//TODO: alle m?glichen zertifikate abfragen und nicht nur eins
			Vector certificates = new Vector();
			byte[] b = helper.copybytes(bytes, 0+offset, 3);
			int certificateslength = ( (b[0] & 0xFF) << 16) | ( (b[1] & 0xFF) << 8) | (b[2] & 0xFF);
			b = helper.copybytes(bytes,3+offset, 3);
			int certificatelength = ( (b[0] & 0xFF) << 16) | ( (b[1] & 0xFF) << 8) | (b[2] & 0xFF);
			b = helper.copybytes(bytes, 6+offset, certificatelength);
			JAPCertificate japcert = JAPCertificate.getInstance(b);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_CERTIFICATE] " + japcert.getIssuer().toString());
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[SERVER_CERTIFICATE] " + japcert.getSubject().toString());
			m_servercertificate = japcert;
			m_selectedciphersuite.setServerCertificate(japcert);
		}

		/**
		 * process server key exchange message
		 * @param bytes server key exchange message
		 * @throws IOException
		 * @throws Exception
		 */
		private void gotServerKeyExchange(byte[] bytes,int offset,int len) throws IOException, Exception
		{
			m_selectedciphersuite.serverKeyExchange(bytes,offset,len, m_clientrandom, m_serverrandom);
		}

		/**
		 * handle certificate request
		 * @param bytes certificate request message
		 */
		private void gotCertificateRequest(byte[] bytes,int offest,int len)
		{
			m_certificaterequested = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CERTIFICATE_REQUEST]");
		}

		/**
		 * handle server hello done message
		 * @param bytes server hello done message
		 */
		private void gotServerHelloDone(byte[] bytes,int offset,int len)
		{
			m_serverhellodone = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_HELLO_DONE]");
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
			byte[] payload=m_aktTLSRecord.m_Data;
			switch (payload[0])
			{
				// warning
				case 1:
				{
					switch (payload[1])
					{
						case 0:
						{
							//TODO : close stream
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

		/**
		 * read the server handshakes and handle alerts
		 * @throws IOException
		 * @throws CertificateException
		 * @throws Exception
		 */
		protected void readServerHandshakes() throws IOException, Exception
		{
			while (!m_serverhellodone)
			{
				readRecord();
//				int contenttype = m_stream.readByte();
//				int version = m_stream.readShort();
//				int length = m_stream.readShort();
//	if (m_aktTLSRecord.version != PROTOCOLVERSION_SHORT)
//				{
//					throw new TLSException("Protocollversion not supportet" + version);
//				}

				switch (m_aktTLSRecord.m_Type)
				{
					case 21:
					{
						handleAlert();
						break;
					}
					case 22:
					{
						break;
					}
					default:
					{
						throw new TLSException("Error while shaking hands");
					}
				}
				//byte[] b = new byte[4];
				//this.m_stream.readFully(b);
				//int fragmentlength = length;
				int type = m_aktTLSRecord.m_Data[0];
				int length = ( (m_aktTLSRecord.m_Data[1] & 0xFF) << 16) | ( (m_aktTLSRecord.m_Data[2] & 0xFF) << 8) | (m_aktTLSRecord.m_Data[3] & 0xFF);
				//b = new byte[length];
				//this.m_stream.readFully(b);
				//byte[] recieveddata = helper.conc(helper.inttobyte(type, 1),
				//								  helper.conc(helper.inttobyte(length, 3), b));
				m_handshakemessages = helper.conc(m_handshakemessages, m_aktTLSRecord.m_Data,m_aktTLSRecord.m_dataLen);
				switch (type)
				{
					//Server hello
					case 2:
					{
						this.gotServerHello(m_aktTLSRecord);
						break;
					}
					//certificate
					case 11:
					{
						this.gotCertificate(m_aktTLSRecord.m_Data,4,m_aktTLSRecord.m_dataLen-4);
						break;
					}
					//server key exchange
					case 12:
					{
						this.gotServerKeyExchange(m_aktTLSRecord.m_Data,4,m_aktTLSRecord.m_dataLen-4);
						break;
					}
					//certificate request
					case 13:
					{
						int aktIndex=4;
						gotCertificateRequest(m_aktTLSRecord.m_Data,aktIndex,
							m_aktTLSRecord.m_dataLen-aktIndex);
						aktIndex+=length;
//						byte []b = new byte[4];
//						this.m_stream.readFully(b);
						type = m_aktTLSRecord.m_Data[aktIndex];
						length = ( (m_aktTLSRecord.m_Data[aktIndex+1] & 0xFF) << 16) |
							( (m_aktTLSRecord.m_Data[aktIndex+2] & 0xFF) << 8) |
							(m_aktTLSRecord.m_Data[aktIndex+3] & 0xFF);
//						b = new byte[length];
//						this.m_stream.readFully(b);
						if (type != 14)
						{
							throw new TLSException(
								"ServerHelloDone expected but Server has returned something else");
						}
						//byte[] recieveddata = helper.conc(helper.inttobyte(type, 1),
						//	helper.conc(helper.inttobyte(length, 3), b));
						this.gotServerHelloDone(m_aktTLSRecord.m_Data,aktIndex+4,m_aktTLSRecord.m_dataLen-aktIndex-4);
						//m_handshakemessages = helper.conc(m_handshakemessages, recieveddata);

						break;
					}
					//server hello done
					case 14:
					{
						this.gotServerHelloDone(m_aktTLSRecord.m_Data,4,m_aktTLSRecord.m_dataLen-4);
						break;
					}
					default:
					{
						throw new TLSException("Unexpected Handshake type" + type);
					}
				}
			}

		}

		/**
		 * wait for server finished message
		 *
		 */
		protected void readServerFinished() throws TLSException, IOException
		{
			readRecord();

			switch (m_aktTLSRecord.m_Type)
			{
				case 20:
				{
					if (m_aktTLSRecord.m_dataLen == 1 && m_aktTLSRecord.m_Data[0] == 1)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_CHANGE_CIPHER_SPEC]");
					}
					break;
				}
				case 21:
				{
					handleAlert();
					break;
				}
				default:
				{
					throw new TLSException("Error while shaking hands");
				}
			}
			readRecord();

			switch (m_aktTLSRecord.m_Type)
			{
				case 22:
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_FINISHED]");
					m_selectedciphersuite.serverFinished(m_aktTLSRecord);
					break;
				}
				case 21:
				{
					this.handleAlert();
					break;
				}
				default:
				{
					throw new TLSException("Error while shaking hands");
				}
			}
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
			m_aktTLSRecord=new TLSRecord();
			m_stream = new DataOutputStream(ostream);
		}

		/**
		 *
		 */
		public void write(byte[] message) throws IOException
		{
			this.send(23, message);
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
		private synchronized void send(int type, byte[] message) throws IOException
		{
			System.arraycopy(message,0,m_aktTLSRecord.m_Data,0,message.length);
			m_aktTLSRecord.setLength(message.length);
			m_aktTLSRecord.setType(type);
			if (m_encrypt)
			{
				m_selectedciphersuite.encode(m_aktTLSRecord);
			}
			m_stream.write(m_aktTLSRecord.m_Header);
			m_stream.write(m_aktTLSRecord.m_Data,0,m_aktTLSRecord.m_dataLen);
			m_stream.flush();
		}

		/**
		 * send a handshake message to the server
		 * @param type handshake type
		 * @param message message
		 * @throws IOException
		 */
		public void sendHandshake(int type, byte[] message) throws IOException
		{
			byte[] senddata = helper.conc(new byte[]
										  { (byte) type}
										  , helper.conc(helper.inttobyte(message.length, 3), message));
			send(22, senddata);
			m_handshakemessages = helper.conc(m_handshakemessages, senddata);
		}

		/**
		 * send a client hello message
		 * @throws IOException
		 */
		public void sendClientHello() throws IOException
		{
			byte[] message = PROTOCOLVERSION;
			byte[] gmt_unix_time;
			byte[] random = new byte[28];
			byte[] sessionid = new byte[]
				{
				0x00};
			byte[] ciphers = new byte[m_supportedciphersuites.size() * 2];
			//Iterator i = supportedciphersuites.iterator();
			int counter = 0;
			for (int i = 0; i < m_supportedciphersuites.size(); i++)
			{
				CipherSuite cs = (CipherSuite) m_supportedciphersuites.elementAt(i);
				ciphers[counter] = cs.getCipherSuiteCode()[0];
				counter++;
				ciphers[counter] = cs.getCipherSuiteCode()[1];
				counter++;

			}
			byte[] ciphersuites = helper.conc(helper.inttobyte(m_supportedciphersuites.size() * 2, 2),
											  ciphers);
			byte[] compression = new byte[]
				{
				0x01, 0x00};

			gmt_unix_time = helper.inttobyte( (System.currentTimeMillis() / (long) 1000), 4);
			Random rand = new Random(System.currentTimeMillis());
			rand.nextBytes(random);

			message = helper.conc(message, gmt_unix_time);
			message = helper.conc(message, random);
			message = helper.conc(message, sessionid);
			message = helper.conc(message, ciphersuites);
			message = helper.conc(message, compression);

			sendHandshake(1, message);
			m_clientrandom = helper.conc(gmt_unix_time, random);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_HELLO]");
		}

		/**
		 * send a client certificate message
		 * because we don't have one we send no certificate back
		 * @throws IOException
		 */
		public void sendClientCertificate() throws IOException
		{
			if (m_certificaterequested)
			{
				//no certificate available
				this.sendHandshake(11, new byte[]
								   {0, 0, 0});
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CERTIFICATE]");
			}
		}

		/**
		 * send a client key exchange message
		 * @throws IOException
		 */
		public void sendClientKeyExchange() throws IOException
		{
			byte[] message = m_selectedciphersuite.clientKeyExchange();
			sendHandshake(16, helper.conc(helper.inttobyte(message.length, 2), message));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_KEY_EXCHANGE]");
		}

		/**
		 * send a change cipher spec message
		 * now all client data will be encrypted
		 * @throws IOException
		 */
		public void sendChangeCipherSpec() throws IOException
		{
			send(20, new byte[]
					  {1});
			m_encrypt = true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_CHANGE_CIPHER_SPEC]");
		}

		/**
		 * send a client finished message
		 * @throws IOException
		 */
		public void sendClientFinished() throws IOException
		{
			sendHandshake(20, m_selectedciphersuite.clientFinished(m_handshakemessages));
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[CLIENT_FINISHED]");
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
	public TinyTLS(String addr, int port) throws UnknownHostException, IOException, Exception
	{
		super(addr, port);
		this.m_handshakecompleted = false;
		this.m_serverhellodone = false;
		this.m_encrypt = false;
		this.m_certificaterequested = false;
		this.m_supportedciphersuites = new Vector();
//		this.m_addCipherSuite(new DHE_RSA_WITH_AES_128_CBC_SHA());
		this.addCipherSuite(new DHE_RSA_WITH_3DES_CBC_SHA());
		//this.m_socket = new Socket(addr, port);
		m_istream = new TLSInputStream(super.getInputStream());
		m_ostream = new TLSOutputStream(super.getOutputStream());
	}

	/**
	 * add more ciphersuites to TinyTLS
	 * @param cs ciphersuite you want to add
	 */
	public void addCipherSuite(CipherSuite cs)
	{
		if (!this.m_supportedciphersuites.contains(cs))
		{
			this.m_supportedciphersuites.addElement(cs);
		}
	}

	/**
	 * close the connection to the server
	 */
	/*	public void close() throws IOException
	 {
	  this.m_socket.close();
	 }
	 */
	/**
	 * start the handshake
	 * @throws IOException
	 * @throws CertificateException
	 * @throws Exception
	 */
	public void startHandshake() throws IOException, Exception
	{
		this.m_handshakemessages = new byte[]
			{};
		this.m_ostream.sendClientHello();
		this.m_istream.readServerHandshakes();
		this.m_ostream.sendClientCertificate();
		this.m_ostream.sendClientKeyExchange();
		this.m_ostream.sendChangeCipherSpec();
		this.m_ostream.sendClientFinished();
		this.m_istream.readServerFinished();
		this.m_handshakecompleted = true;
	}

	public InputStream getInputStream()
	{
		return this.m_istream;
	}

	public OutputStream getOutputStream()
	{
		return this.m_ostream;
	}

}

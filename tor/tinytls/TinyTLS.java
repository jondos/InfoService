/*
 * Created on Mar 16, 2004
 *
 */
package tor.tinytls;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

//import java.util.ArrayList;
//import java.util.Iterator;
import java.util.*;

import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;

import tor.util.helper;
import anon.crypto.JAPCertificate;
import logging.*;
/**
 * @author stefan
 *
 *TinyTLS
  */
public class TinyTLS extends Socket {


//TODO : DEBUGString entfernen
	String debugstring;
	/**
	 * SSL VERSION :
	 *
	 * 3.1 for TLS
	 */
	private static byte[] PROTOCOLVERSION = new byte[]{0x03,0x01};

	private Vector supportedciphersuites;
	private CipherSuite selectedciphersuite = null;

	private Socket socket;
	private TLSInputStream istream;
	private TLSOutputStream ostream;

	private boolean handshakecompleted;
	private boolean serverhellodone;
	private boolean certificaterequested;

	private JAPCertificate servercertificate;
	private DHParameters dhparams;
	private DHPublicKeyParameters dhserverpub;
	private byte[] serverparams;
	private byte[] clientrandom;
	private byte[] serverrandom;
	private byte[] handshakemessages;
	private boolean encrypt;

	/**
	 *
	 * @author stefan
	 *
	* TLSInputStream
	 */
	class TLSInputStream extends InputStream
	{

		private DataInputStream _stream;
		private byte[] input;

		/**
		 * Constructor
		 * @param istream inputstream
		 */
		public TLSInputStream(InputStream istream)
		{
			this._stream = new DataInputStream(istream);
			this.input = new byte[0];
		}

		/**
		 *
		 * @return
		 */
		public int available() throws IOException
		{
			byte[] contenttype = new byte[1];
			byte[] version = new byte[2];
			while(this._stream.available()>0)
			{
				while(this._stream.available()<3);
				this._stream.read(contenttype);
				this._stream.read(version);
				if((version[0]!=PROTOCOLVERSION[0]) || (version[1]!=PROTOCOLVERSION[1])) { throw new TLSException("Protocollversion not supportet"+version[0]+"."+version[1]);}

				switch(contenttype[0])
				{
					case 23:
					{
						while(this._stream.available()<2);
						byte[] b = new byte[2];
						this._stream.read(b);
						int length =  (( b[0] & 0xFF)  << 8) | (b[1]  & 0xFF);

						while(this._stream.available()<length);
						byte[] m = new byte[]{};
						byte[] n;
						while(length>0)
						{
							if(length>2000)
							{
								n=new byte[2000];
								length-=2000;
							} else
							{
								n=new byte[length];
								length=0;
							}
							this._stream.read(n);
							m = helper.conc(m,n);
						}
						this.input = helper.conc(this.input,selectedciphersuite.decode(helper.conc(contenttype,helper.conc(version,b)),m));
						break;
					}
					case 21 :
					{
						this.handleAlert();
						break;
					}
					default :
					{
						throw new TLSException("Error while decoding application data");
					}
				}
			}
			return this.input.length;
		}

		public int read() throws IOException
		{
			if(this.available()>0)
			{
				byte b = input[0];
				input = helper.copybytes(input,1,input.length-1);
				return (b & 0xFF);
			} else
			{
				return -1;
			}
		}

		/**
		 * process server hello
		 * @param bytes server hello message
		 * @throws IOException
		 */
		private void gotServerHello(byte[] bytes) throws IOException
		{
			byte[] b;
			byte[] sslversion = helper.copybytes(bytes,0,2);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO] SSLVERSION :"+sslversion[0]+"."+sslversion[1]);
			byte[] random = helper.copybytes(bytes,2,32);
			debugstring="";
			for(int i=0;i<32;i++)
			{
				debugstring +=""+i+":"+(random[i] & 0xFF)+" ";
			}
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO] RANDOMBYTES :"+debugstring);
			byte[] sessionid = new byte[0];
			int sessionidlength = bytes[34];
			if(sessionidlength>0)
			{
				sessionid = helper.copybytes(bytes,35,sessionidlength);
			}
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO] Laenge der SessionID : "+sessionidlength);
			byte[] ciphersuite = helper.copybytes(bytes,35+sessionidlength,2);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO] Ciphersuite : "+ciphersuite[0]+" "+ciphersuite[1]);
			byte[] compression = helper.copybytes(bytes,37+sessionidlength,1);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO] Kompression : "+compression[0]);
			if((sslversion[0]!=PROTOCOLVERSION[0]) || (sslversion[1]!=PROTOCOLVERSION[1])) {throw new TLSException("Server replies with wrong protocoll");}
			//Iterator i = supportedciphersuites.iterator();
			CipherSuite cs = null;
			for(int i=0;i<supportedciphersuites.size();i++)
			{
				cs =(CipherSuite)supportedciphersuites.elementAt(i);
				b=cs.getCipherSuiteCode();
				if((b[0]==ciphersuite[0])&&(b[1]==ciphersuite[1]))
				{
					break;
				}
				cs = null;
			}
			if(cs==null)
			{
				throw new TLSException("Unsupported Ciphersuite selected");
			}
			serverrandom = random;
			selectedciphersuite = cs;
			supportedciphersuites = null;
		}

		/**
		 * process server certificate
		 * @param bytes server certificate message
		 * @throws IOException
		 * @throws CertificateException
		 */
		private void gotCertificate(byte[] bytes) throws IOException
		{
//TODO: alle m?glichen zertifikate abfragen und nicht nur eins
			Vector certificates = new Vector();
			byte[] b = helper.copybytes(bytes,0,3);
			int certificateslength = ((b[0] &0xFF)<<16)|((b[1] & 0xFF) <<8)|(b[2] & 0xFF);
			b = helper.copybytes(bytes,3,3);
			int certificatelength = ((b[0] &0xFF)<<16)|((b[1] & 0xFF) <<8)|(b[2] & 0xFF);
			b= helper.copybytes(bytes,6,certificatelength);
			JAPCertificate japcert = JAPCertificate.getInstance(b);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_CERTIFICATE] "+japcert.getIssuer().toString());
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_CERTIFICATE] "+japcert.getSubject().toString());
			servercertificate = japcert;
			selectedciphersuite.setServerCertificate(japcert);
		}

		/**
		 * process server key exchange message
		 * @param bytes server key exchange message
		 * @throws IOException
		 * @throws Exception
		 */
		private void gotServerKeyExchange(byte[] bytes) throws IOException,Exception
		{
			selectedciphersuite.serverKeyExchange(bytes,clientrandom,serverrandom);
		}

		/**
		 * handle certificate request
		 * @param bytes certificate request message
		 */
		private void gotCertificateRequest(byte[] bytes)
		{
			certificaterequested = true;
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_CERTIFICATE_REQUEST]");
		}

		/**
		 * handle server hello done message
		 * @param bytes server hello done message
		 */
		private void gotServerHelloDone(byte[] bytes)
		{
			serverhellodone = true;
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_HELLO_DONE]");
		}

		/**
		 * handle alert message
		 * @throws IOException
		 */
		private void handleAlert() throws IOException
		{
			System.out.println("ALERT");
			byte[] b = new byte[2];
			while(this._stream.available()<2);
			this._stream.read(b);
			int length = ((b[0] & 0xFF) << 8 ) | (b[1] & 0xFF);
			while(this._stream.available()<length);
			b= new byte[length];
			this._stream.read(b);
			if(handshakecompleted)
			{
				b = selectedciphersuite.decode(helper.conc(new byte[]{21},helper.conc(PROTOCOLVERSION,helper.inttobyte(length,2))),b);
			}
			switch(b[0])
			{
				// warning
				case 1 :
				{
					switch(b[1])
					{
						case 0 :
						{
							//TODO : close stream
							LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[RECIEVED-ALERT] TYPE=WARNING ; MESSAGE=CLOSE NOTIFY");
							break;
						}
						default :
						{
							throw new TLSException("TLSAlert detected!! Level : Warning - Description :"+b[1]);
						}
					}
					break;
				}
				// fatal
				case 2 :
				{
					throw new TLSException("TLSAlert detected!! Level : Fatal - Description :"+b[1]);
				}
				default :
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
		public void readServerHandshakes() throws IOException, Exception
		{
			while(!serverhellodone)
			{
				while(this._stream.available()<3);
				byte[] b;
				byte[] contenttype = new byte[1];
				this._stream.read(contenttype);
				byte[] version = new byte[2];
				this._stream.read(version);

				if((version[0]!=PROTOCOLVERSION[0]) || (version[1]!=PROTOCOLVERSION[1])) { throw new TLSException("Protocollversion not supportet"+version[0]+"."+version[1]);}

				switch(contenttype[0])
				{
					case 21 :
					{
						this.handleAlert();
						break;
					}
					case 22:
					{
						break;
					}
					default :
					{
						throw new TLSException("Error while shaking hands");
					}
				}
				while(this._stream.available()<6);
				b = new byte[6];
				this._stream.read(b);
				int fragmentlength = ((b[0] & 0xFF) << 8 ) | (b[1] & 0xFF);
				int type = b[2];
				int length = ( (b[3] & 0xFF) << 16 ) | ( (b[4] & 0xFF)  << 8) | (b[5] & 0xFF);
				while(this._stream.available()<length);
				b = new byte[length];
				this._stream.read(b);
				byte[] recieveddata = helper.conc(helper.inttobyte(type,1),helper.conc(helper.inttobyte(length,3),b));
				handshakemessages = helper.conc(handshakemessages,recieveddata);
				switch(type)
				{
					//Server hello
					case 2 :
					{
						this.gotServerHello(b);
						break;
					}
					//certificate
					case 11 :
					{
						this.gotCertificate(b);
						break;
					}
					//server key exchange
					case 12 :
					{
						this.gotServerKeyExchange(b);
						break;
					}
					//certificate request
					case 13 :
					{
						this.gotCertificateRequest(b);

						while(this._stream.available()<4);
						b = new byte[4];
						this._stream.read(b);
						type = b[0];
						length = ( (b[1] & 0xFF) << 16 ) | ( (b[2] & 0xFF)  << 8) | (b[3]  & 0xFF);
						b = new byte[length];
						while(this._stream.available()<length);
						this._stream.read(b);
						if(type!=14)
						{
							throw new TLSException("ServerHelloDone expected but Server has returned something else");
						}
						recieveddata = helper.conc(helper.inttobyte(type,1),helper.conc(helper.inttobyte(length,3),b));
						this.gotServerHelloDone(b);
						handshakemessages = helper.conc(handshakemessages,recieveddata);

						break;
					}
					//server hello done
					case 14 :
					{
						this.gotServerHelloDone(b);
						break;
					}
					default :
					{
						throw new TLSException("Unexpected Handshake type"+type);
					}
				}
			}

		}

		/**
		 * wait for server finished message
		 *
		 */
		public void readServerFinished() throws TLSException,IOException
		{
			while(this._stream.available()<3);
			byte[] b;
			byte[] contenttype = new byte[1];
			this._stream.read(contenttype);
			byte[] version = new byte[2];
			this._stream.read(version);
			if((version[0]!=PROTOCOLVERSION[0]) || (version[1]!=PROTOCOLVERSION[1])) { throw new TLSException("Protocollversion not supportet"+version[0]+"."+version[1]);}

			switch(contenttype[0])
			{
				case 20:
				{
					while(this._stream.available()<3);
					b = new byte[3];
					this._stream.read(b);
					if(b[0]==0&&b[1]==1&&b[2]==1)
					{
						LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_CHANGE_CIPHER_SPEC]");
					}
					break;
				}
				case 21 :
				{
					this.handleAlert();
					break;
				}
				default :
				{
					throw new TLSException("Error while shaking hands");
				}
			}
			while(this._stream.available()<3);
			this._stream.read(contenttype);
			this._stream.read(version);
			if((version[0]!=PROTOCOLVERSION[0]) || (version[1]!=PROTOCOLVERSION[1])) { throw new TLSException("Protocollversion not supportet"+version[0]+"."+version[1]);}

			switch(contenttype[0])
			{
				case 22:
				{
					LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_FINISHED]");
					while(this._stream.available()<2);
					b = new byte[2];
					this._stream.read(b);
					int length =  (( b[0] & 0xFF)  << 8) | (b[1]  & 0xFF);
					byte[] message = new byte[length];
					while(this._stream.available()<length);
					this._stream.read(message);
					selectedciphersuite.serverFinished(helper.conc(contenttype,helper.conc(version,b)),message);
					break;
				}
				case 21 :
				{
					this.handleAlert();
					break;
				}
				default :
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

		private DataOutputStream _stream;

		/**
		 * Constructor
		 * @param ostream outputstream
		 */
		public TLSOutputStream(OutputStream ostream)
		{
			this._stream = new DataOutputStream(ostream);
		}

		/**
		 *
		 */
		public void write(byte[] message) throws IOException
		{
			this.send(23,message);
		}

		/**
		 *
		 */
		public void write(int i) throws IOException
		{
			this.write(new byte[]{(byte) i});
		}

		/**
		 *
		 */
		public void flush() throws IOException
		{
			this._stream.flush();
		}

		/**
		 * send a message to the server
		 * @param type type of the tls message
		 * @param message message
		 * @throws IOException
		 */
		private void send(int type,byte[] message) throws IOException
		{
			byte[] encryptedmessage = message;
			if(encrypt)
			{
				encryptedmessage= selectedciphersuite.encode(helper.conc(new byte[]{(byte)(type & 0xFF)},helper.conc(PROTOCOLVERSION,helper.inttobyte(encryptedmessage.length,2))) ,encryptedmessage);
			}
			this._stream.write(helper.conc(new byte[]{(byte)(type & 0xFF)},helper.conc(PROTOCOLVERSION,helper.conc(helper.inttobyte(encryptedmessage.length,2) ,encryptedmessage))));
		}

		/**
		 * send a handshake message to the server
		 * @param type handshake type
		 * @param message message
		 * @throws IOException
		 */
		public void sendHandshake(int type,byte[] message) throws IOException
		{
			byte[] senddata = helper.conc(new byte[]{(byte)type},helper.conc(helper.inttobyte(message.length,3),message));
			this.send(22,senddata);
			handshakemessages = helper.conc(handshakemessages,senddata);
		}

		/**
		 * send a client hello message
		 * @throws IOException
		 */
		public void sendClientHello() throws IOException
		{
			byte[] message= PROTOCOLVERSION;
			byte[] gmt_unix_time;
			byte[] random = new byte[28];
			byte[] sessionid = new byte[]{0x00};
			byte[] ciphers = new byte[supportedciphersuites.size()*2];
			//Iterator i = supportedciphersuites.iterator();
			int counter = 0;
			for(int i=0;i<supportedciphersuites.size();i++)
			{
				CipherSuite cs = (CipherSuite)supportedciphersuites.elementAt(i);
				ciphers[counter] = cs.getCipherSuiteCode()[0];
				counter++;
				ciphers[counter] = cs.getCipherSuiteCode()[1];
				counter++;

			}
			byte[] ciphersuites = helper.conc(helper.inttobyte(supportedciphersuites.size()*2,2),ciphers);
			byte[] compression = new byte[]{0x01,0x00};

			gmt_unix_time = helper.inttobyte((System.currentTimeMillis() / (long) 1000),4);
			Random rand = new Random(System.currentTimeMillis());
			rand.nextBytes(random);

			message = helper.conc(message,gmt_unix_time);
			message = helper.conc(message,random);
			message = helper.conc(message,sessionid);
			message = helper.conc(message,ciphersuites);
			message = helper.conc(message,compression);

			this.sendHandshake(1,message);
			clientrandom =helper. conc(gmt_unix_time,random);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[CLIENT_HELLO]");
		}

		/**
		 * send a client certificate message
		 * because we don't have one we send no certificate back
		 * @throws IOException
		 */
		public void sendClientCertificate() throws IOException
		{
			if(certificaterequested)
			{
				//no certificate available
				this.sendHandshake(11,new byte[]{0,0,0});
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[CLIENT_CERTIFICATE]");
			}
		}

		/**
		 * send a client key exchange message
		 * @throws IOException
		 */
		public void sendClientKeyExchange() throws IOException
		{
			byte[] message = selectedciphersuite.clientKeyExchange();
			this.sendHandshake(16,helper.conc(helper.inttobyte(message.length,2),message));
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[CLIENT_KEY_EXCHANGE]");
		}

		/**
		 * send a change cipher spec message
		 * now all client data will be encrypted
		 * @throws IOException
		 */
		public void sendChangeCipherSpec() throws IOException
		{
			this.send(20,new byte[]{1});
			encrypt = true;
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[CLIENT_CHANGE_CIPHER_SPEC]");
		}

		/**
		 * send a client finished message
		 * @throws IOException
		 */
		public void sendClientFinished() throws IOException
		{
			sendHandshake(20,selectedciphersuite.clientFinished(handshakemessages));
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[CLIENT_FINISHED]");
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
	public TinyTLS(String addr,int port) throws UnknownHostException , IOException, Exception
	{
		this.handshakecompleted = false;
		this.serverhellodone = false;
		this.encrypt = false;
		this.certificaterequested = false;
		this.supportedciphersuites = new Vector();
//		this.addCipherSuite(new DHE_RSA_WITH_AES_128_CBC_SHA());
		this.addCipherSuite(new DHE_RSA_WITH_3DES_CBC_SHA());
		this.socket = new Socket(addr,port);
		this.istream = new TLSInputStream(this.socket.getInputStream());
		this.ostream = new TLSOutputStream(this.socket.getOutputStream());
	}

	/**
	 * add more ciphersuites to TinyTLS
	 * @param cs ciphersuite you want to add
	 */
	public void addCipherSuite(CipherSuite cs)
	{
		if(!this.supportedciphersuites.contains(cs))
		{
			this.supportedciphersuites.addElement(cs);
		}
	}

	/**
	 * close the connection to the server
	 */
	public void close() throws IOException
	{
		this.socket.close();
	}

	/**
	 * start the handshake
	 * @throws IOException
	 * @throws CertificateException
	 * @throws Exception
	 */
	public void startHandshake() throws IOException,Exception
	{
		this.handshakemessages = new byte[]{};
		this.ostream.sendClientHello();
		this.istream.readServerHandshakes();
		this.ostream.sendClientCertificate();
		this.ostream.sendClientKeyExchange();
		this.ostream.sendChangeCipherSpec();
		this.ostream.sendClientFinished();
		this.istream.readServerFinished();
		handshakecompleted = true;
	}

	public InputStream getInputStream()
	{
		return this.istream;
	}

	public OutputStream getOutputStream()
	{
		return this.ostream;
	}


}

// Copyright (C) 2002 Adam Megacz <adam@xwt.org> all rights reserved.
//
// You may modify, copy, and redistribute this code under the terms of
// the GNU Library Public License version 2.1, with the exception of
// the portion of clause 6a after the semicolon (aka the "obnoxious
// relink clause")

//package org.xwt;

package pay.crypto.tinyssl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
//import org.xwt.util.Log;
/**
 TinySSL: a tiny SSL implementation in Java, built on the
 bouncycastle.org lightweight crypto library.

 This class implements an SSLv3 client-side socket, with the
 SSL_RSA_EXPORT_WITH_RC4_40_MD5 and SSL_RSA_WITH_RC4_128_MD5 cipher
 suites, as well as certificate chain verification against a
 collection of 93 built-in Trusted Root CA public keys (the same 93
 included with Microsoft Internet Explorer 5.5 SP2).

 As of 07-Dec-01, the zipped bytecode for this class is 43k, and the
 subset of bouncycastle it requires is 82k.

 This class should work correctly on any Java 1.1 compliant
 platform. The java.security.* classes are not used.

 The main design goal for this class was the smallest possible body
 of code capable of connecting to 99% of all active HTTPS
 servers. Although this class is useful in many other situations
 (IMAPS, Secure SMTP, etc), the author will refuse all feature
 requests and submitted patches which go beyond this scope.

 Because of the limited goals of this class, certain abstractions
 have been avoided, and certain parameters have been
 hard-coded. "Magic numbers" are often used instead of "static final
 int"'s, although they are usually accompanied by a descriptive
 comment. Numeric offsets into byte arrays are also favored over
 DataInputStream(ByteArrayInputStream(foo))'s.

 Much thanks and credit go to the BouncyCastle team for producing
 such a first-class library, and for helping me out on the
 dev-crypto mailing list while I was writing this.

 Revision History:

 1.0  07-Dec-01  Initial Release

 1.01 15-Mar-02  Added PKCS1 class to avoid dependancy on java.security.SecureRandom

 1.02 27-Mar-02  Fixed a bug which would hang the connection when more than one
 Handshake message appeared in the same TLS Record

 1.03 10-Aug-02  Fixed a vulnerability outlined at
 http://online.securityfocus.com/archive/1/286290

 */
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class TinySSL extends Socket
{

	// Simple Test //////////////////////////////////////////////

	public static void main(String[] args)
	{
		try
		{
			Socket s = new TinySSL("www.dresdner-privat.de", 443);
			PrintWriter pw = new PrintWriter(s.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw.println("GET / HTTP/1.0");
			pw.println("");
			pw.flush();

			while (true)
			{
				String s2 = br.readLine();
				if (s2 == null)
				{
					return;
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, s2);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Static Data //////////////////////////////////////////////

	public static class SSLException extends IOException
	{
		public SSLException(String s)
		{
			super(s);
		}
	}

	static SubjectPublicKeyInfo[] trusted_CA_public_keys;
	static String[] trusted_CA_public_key_identifiers;
	public static byte[] pad1 = new byte[48];
	public static byte[] pad2 = new byte[48];
	public static byte[] pad1_sha = new byte[40];
	public static byte[] pad2_sha = new byte[40];
	static byte[] randpool;
	static long randcnt = 0;

	// Cipher State //////////////////////////////////////////////

	public byte[] server_random = new byte[32];
	public byte[] client_random = new byte[32];
	public byte[] client_write_MAC_secret = new byte[16];
	public byte[] server_write_MAC_secret = new byte[16];
	public byte[] client_write_key = null;
	public byte[] server_write_key = null;
	public byte[] master_secret = null;

	/** the bytes of the ServerKeyExchangeMessage, null if none recieved */
	public byte[] serverKeyExchange = null;

	/** true iff the server asked for a certificate */
	public boolean cert_requested = false;

	public X509CertificateStructure server_cert = null;

	public SSLOutputStream os = null;
	public SSLInputStream is = null;

	String hostname;

	/** if true, we don't mind if the server's cert isn't signed by a CA. USE WITH CAUTION! */
	boolean ignoreUntrustedCert = false;

	/** the concatenation of all the bytes of all handshake messages sent or recieved */
	public byte[] handshakes = new byte[]
		{};

	/** true iff we're using SSL_RSA_EXPORT_WITH_RC4_40_MD5 */
	boolean export = false;

	public InputStream getInputStream() throws IOException
	{
		return is != null ? is : super.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException
	{
		return os != null ? os : super.getOutputStream();
	}

	public TinySSL(String host, int port) throws IOException
	{
		this(host, port, true, false);
	}

	public TinySSL(String host, int port, boolean negotiateImmediately) throws IOException
	{
		this(host, port, negotiateImmediately, false);
	}

	public TinySSL(String host, int port, boolean negotiateImmediately, boolean ignoreUntrustedCert) throws
		IOException
	{
		super(host, port);
		hostname = host;
		this.ignoreUntrustedCert = ignoreUntrustedCert;
		if (negotiateImmediately)
		{
			negotiate();
		}
	}

	/** negotiates the SSL connection */
	public void negotiate() throws IOException
	{
		os = new SSLOutputStream(super.getOutputStream());
		is = new SSLInputStream(super.getInputStream());
		os.writeClientHello();
		is.readServerHandshakes();
		os.sendClientHandshakes();
		is.readServerFinished();
	}

	class SSLInputStream extends InputStream
	{

		/** the underlying inputstream */
		DataInputStream raw;

		/** the server's sequence number */
		public int seq_num = 0;

		/** the decryption engine */
		public RC4Engine rc4 = null;

		/** pending bytes -- decrypted, but not yet fed to consumer */
		byte[] pend = null;
		int pendstart = 0;
		int pendlen = 0;

		public void mark()
		{}

		public void reset()
		{}

		public boolean markSupported()
		{
			return false;
		}

		public long skip(long l) throws IOException
		{
			for (long i = 0; i < l; i++)
			{
				read();
			}
			return l;
		}

		public SSLInputStream(InputStream raw)
		{
			this.raw = new DataInputStream(raw);
		}

		public int available() throws IOException
		{
			return pendlen;
		}

		public int read() throws IOException
		{
			byte[] singlebyte = new byte[1];
			int numread = read(singlebyte);
			if (numread != 1)
			{
				return -1;
			}
			return (int) singlebyte[0];
		}

		public int read(byte[] b, int off, int len) throws IOException
		{
			if (pendlen == 0)
			{
				pend = readRecord();
				if (pend == null)
				{
					return -1;
				}
				pendstart = 0;
				pendlen = pend.length;
			}
			int ret = Math.min(len, pendlen);
			System.arraycopy(pend, pendstart, b, off, ret);
			pendlen -= ret;
			pendstart += ret;
			return ret;
		}

		/** reads and decrypts exactly one record; blocks if unavailable */
		public byte[] readRecord() throws IOException
		{

			// we only catch EOFException here, because anywhere else
			// would be "unusual", and we *want* and EOFException in
			// those cases
			byte type;
			try
			{
				type = raw.readByte();
			}
			catch (EOFException e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got EOFException reading packet type");
				return null;
			}

			byte ver_major = raw.readByte();
			byte ver_minor = raw.readByte();
			short len = raw.readShort();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "got record of type " + type + ", SSLv" + ver_major + "." + ver_minor + ", length=" +
						  len);

			byte[] ret = new byte[len];
			raw.readFully(ret);

			// simply ignore ChangeCipherSpec messages -- we change as soon as we send ours
			if (type == 20)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got ChangeCipherSpec; ignoring");
				seq_num = 0;
				return readRecord();
			}

			byte[] decrypted_payload;

			// if crypto hasn't been enabled yet; skip crypt and hash
			if (rc4 == null)
			{
				decrypted_payload = ret;
			}
			else
			{
				// decrypt the payload
				decrypted_payload = new byte[len - 16];
				rc4.processBytes(ret, 0, len - 16, decrypted_payload, 0);

				// check the MAC
				byte[] MAC = new byte[16];
				rc4.processBytes(ret, len - 16, 16, MAC, 0);
				byte[] ourMAC = computeMAC(type, decrypted_payload, 0, decrypted_payload.length,
										   server_write_MAC_secret, seq_num++);
				for (int i = 0; i < MAC.length; i++)
				{
					if (MAC[i] != ourMAC[i])
					{
						throw new SSLException("MAC mismatch on byte " + i + ": got " + MAC[i] +
											   ", expecting " + ourMAC[i]);
					}
				}
			}

			if (type == 21)
			{
				if (decrypted_payload[1] > 1)
				{
					throw new SSLException("got SSL ALERT message, level=" + decrypted_payload[0] + " code=" +
										   decrypted_payload[1]);
				}
				else if (decrypted_payload[1] == 0)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "server requested connection closure; returning null");
					return null;
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "got SSL ALERT message, level=" + decrypted_payload[0] + " code=" +
								  decrypted_payload[1]);
					return readRecord();
				}

			}
			else if (type == 22)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "read a handshake");

			}
			else if (type != 23)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "unexpected record type: " + type + "; skipping");
				return readRecord();

			}

			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "  returning " + decrypted_payload.length + " byte record payload");
			return decrypted_payload;
		}

		private byte[] readHandshake() throws IOException
		{
			// acquire a handshake message
			byte type = (byte) read();
			int len = ( (read() & 0xff) << 16) | ( (read() & 0xff) << 8) | (read() & 0xff);
			byte[] rec = new byte[len + 4];
			rec[0] = type;
			rec[1] = (byte) ( ( (len & 0x00ff0000) >> 16) & 0xff);
			rec[2] = (byte) ( ( (len & 0x0000ff00) >> 8) & 0xff);
			rec[3] = (byte) ( (len & 0x000000ff) & 0xff);
			if (len > 0)
			{
				read(rec, 4, len);
			}
			return rec;
		}

		/** This reads the ServerHello, Certificate, and ServerHelloDone handshake messages */
		public void readServerHandshakes() throws IOException
		{
			for (; ; )
			{

				byte[] rec = readHandshake();
				handshakes = concat(new byte[][]
									{handshakes, rec});
				DataInputStream stream = new DataInputStream(new ByteArrayInputStream(rec, 4, rec.length - 4));

				switch (rec[0])
				{
					case 2: // ServerHello
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got ServerHello");
						byte ver_major = rec[4];
						byte ver_minor = rec[5];
						System.arraycopy(rec, 6, server_random, 0, server_random.length);
						short cipher_high = rec[6 + server_random.length + rec[6 + server_random.length] + 1];
						short cipher_low = rec[6 + server_random.length + rec[6 + server_random.length] + 2];

						if (cipher_low == 0x04 || cipher_high != 0x00)
						{
							export = false;
							LogHolder.log(LogLevel.DEBUG, LogType.PAY, "using SSL_RSA_WITH_RC4_128_MD5");

						}
						else if (cipher_low == 0x03 || cipher_high != 0x00)
						{
							export = true;
							LogHolder.log(LogLevel.DEBUG, LogType.PAY, "using SSL_RSA_EXPORT_WITH_RC4_40_MD5");

						}
						else
						{
							throw new SSLException("server asked for cipher " +
								( (cipher_high << 8) | cipher_low) +
								" but we only do SSL_RSA_WITH_RC4_128_MD5 (0x0004) and " +
								"SSL_RSA_EXPORT_WITH_RC4_40_MD5 (0x0003)");
						}

						byte compressionMethod = rec[6 + server_random.length + rec[6 + server_random.length] +
							3];
						if (compressionMethod != 0x0)
						{
							throw new SSLException("server asked for compression method " + compressionMethod +
								" but we don't support compression");
						}
						break;

					case 11: // Server's certificate(s)
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got Server Certificate(s)");
						int numcertbytes = ( (rec[4] & 0xff) << 16) | ( (rec[5] & 0xff) << 8) |
							(rec[6] & 0xff);
						int numcerts = 0;
						X509CertificateStructure last_cert = null;
						X509CertificateStructure this_cert = null;

						for (int i = 0; i < numcertbytes; )
						{
							int certlen = ( (rec[7 + i] & 0xff) << 16) | ( (rec[7 + i + 1] & 0xff) << 8) |
								(rec[7 + i + 2] & 0xff);
							try
							{
								DERInputStream dIn = new DERInputStream(new ByteArrayInputStream(rec,
									7 + i + 3, certlen));
								this_cert = new X509CertificateStructure( (DERConstructedSequence) dIn.
									readObject());
							}
							catch (Exception e)
							{
								SSLException t = new SSLException("error decoding server certificate: " + e);
								t.fillInStackTrace();
								throw t;
							}

							if (server_cert == null)
							{
								server_cert = this_cert;
								TBSCertificateStructure tbs = server_cert.getTBSCertificate();
								X509Name subject = tbs.getSubject();

								// gross hack to extract the Common Name so we can compare it to the server hostname
								String CN = tbs.getSubject().toString() + " ";
								boolean good = false;
								for (int j = 0; j < CN.length() - 3; j++)
								{
									if (CN.substring(j, j + 3).equals("CN="))
									{
										good = true;
										CN = CN.substring(j + 3, CN.indexOf(' ', j + 3));
										break;
									}
								}

								if (!good)
								{
									throw new SSLException("server certificate does not seem to have a CN: " +
										CN);
								}
								if (!ignoreUntrustedCert && !CN.equals(hostname))
								{
									throw new SSLException("connecting to host " + hostname +
										" but server certificate was issued for " + CN);
								}

								// SimpleDateFormat dateF = new SimpleDateFormat("MM-dd-yy-HH-mm-ss-z");
								SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");

								// the following idiocy is a result of the brokenness of the GNU Classpath's SimpleDateFormat
								String s = tbs.getStartDate().getTime();
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, "startDate: " + s);
								/*  s = s.substring(2, 4) + "-" + s.substring(4, 6) + "-" + s.substring(0, 2) + "-" + s.substring(6, 8) + "-" +
								  s.substring(8, 10) + "-" + s.substring(10, 12) + "-" + s.substring(12);
								  LogHolder.log(this,"startDate: "+s);
								 */
								Date startDate = dateF.parse(s, new ParsePosition(0));

								s = tbs.getEndDate().getTime();
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, "endDate: " + s);

								/*                                s = s.substring(2, 4) + "-" + s.substring(4, 6) + "-" + s.substring(0, 2) + "-" + s.substring(6, 8) + "-" +
								 s.substring(8, 10) + "-" + s.substring(10, 12) + "-" + s.substring(12);
								 */
								Date endDate = dateF.parse(s, new ParsePosition(0));

								Date now = new Date();
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, "now: " + now);
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, "start: " + startDate);
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, "end: " + endDate);

								if (!ignoreUntrustedCert && now.after(endDate))
								{
									throw new SSLException("server certificate expired on " + endDate);
								}
								if (!ignoreUntrustedCert && now.before(startDate))
								{
									throw new SSLException("server certificate will not be valid until " +
										startDate);
								}

								LogHolder.log(LogLevel.DEBUG, LogType.PAY,
											  "server cert (name, validity dates) checks out okay");

							}
							else
							{

								// don't check the top cert since some very old root certs lack a BasicConstraints field.
								if (certlen + 3 + i < numcertbytes)
								{
									// defend against Mike Benham's attack
									X509Extension basicConstraints = this_cert.getTBSCertificate().
										getExtensions().getExtension(X509Extensions.BasicConstraints);
									if (basicConstraints == null)
									{
										throw new SSLException(
											"certificate did not contain a basic constraints block");
									}
									DERInputStream dis = new DERInputStream(new ByteArrayInputStream(
										basicConstraints.getValue().getOctets()));
									BasicConstraints bc = new BasicConstraints( (DERConstructedSequence) dis.
										readObject());
									if (!bc.isCA())
									{
										throw new SSLException("non-CA certificate used for signing");
									}
								}

								if (!isSignedBy(last_cert, this_cert.getSubjectPublicKeyInfo()))
								{
									throw new SSLException("the server sent a broken chain of certificates");
								}
							}

							last_cert = this_cert;
							i += certlen + 3;
							numcerts++;
						}
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "  Certificate (" + numcerts + " certificates)");

						if (ignoreUntrustedCert)
						{
							break;
						}

						boolean good = false;

						// pass 1 -- only check CA's whose subject is a partial match
						String subject = this_cert.getSubject().toString();
						for (int i = 0; i < trusted_CA_public_keys.length; i++)
						{
							if (subject.indexOf(trusted_CA_public_key_identifiers[i]) != -1 &&
								isSignedBy(this_cert, trusted_CA_public_keys[i]))
							{
								LogHolder.log(LogLevel.DEBUG, LogType.PAY,
											  "pass 1: server cert was signed by trusted CA " + i);
								good = true;
								break;
							}
						}

						// pass 2 -- try all certs
						if (!good)
						{
							for (int i = 0; i < trusted_CA_public_keys.length; i++)
							{
								if (isSignedBy(this_cert, trusted_CA_public_keys[i]))
								{
									LogHolder.log(LogLevel.DEBUG, LogType.PAY,
												  "pass 2: server cert was signed by trusted CA " + i);
									good = true;
									break;
								}
							}
						}

						if (!good)
						{
							throw new SSLException("server cert was not signed by a trusted CA");
						}
						break;

					case 12:
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got ServerKeyExchange");
						serverKeyExchange = rec;
						break;

					case 13:
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "got Request for Client Certificates");
						cert_requested = true;
						break;

					case 14:
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "  ServerHelloDone");
						return;
					default:
						throw new SSLException("unknown handshake of type " + rec[0]);
				}
			}
		}

		public void readServerFinished() throws IOException
		{

			byte[] rec = readHandshake();
			if (rec[0] != 20)
			{
				throw new SSLException("expecting server Finished message, but got message of type " + rec[0]);
			}

			byte[] expectedFinished = concat(new byte[][]
											 {
											 md5(new byte[][]
												 {master_secret, pad2,
												 md5(new byte[][]
				{handshakes, new byte[]
				{ (byte) 0x53, (byte) 0x52, (byte) 0x56, (byte) 0x52}
				,
				master_secret, pad1})}),
											 sha(new byte[][]
												 {master_secret, pad2_sha,
												 sha(new byte[][]
				{handshakes, new byte[]
				{ (byte) 0x53, (byte) 0x52, (byte) 0x56, (byte) 0x52}
				,
				master_secret, pad1_sha})})});

			for (int i = 0; i < expectedFinished.length; i++)
			{
				if (expectedFinished[i] != rec[i + 4])
				{
					throw new SSLException("server Finished message mismatch!");
				}
			}

			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "server finished message checked out okay!");
		}

	}

	class SSLOutputStream extends OutputStream
	{

		/** the underlying outputstream */
		DataOutputStream raw;

		/** the sequence number for sending */
		public long seq_num = 0;

		/** the encryption engine for sending */
		RC4Engine rc4 = null;

		public SSLOutputStream(OutputStream raw)
		{
			this.raw = new DataOutputStream(raw);
		}

		public void flush() throws IOException
		{
			raw.flush();
		}

		public void write(int b) throws IOException
		{
			write(new byte[]
				  { (byte) b}
				  , 0, 1);
		}

		public void write(byte[] b, int off, int len) throws IOException
		{
			write(b, off, len, (byte) 23);
		}

		public void close() throws IOException
		{
			write(new byte[]
				  {0x1, 0x0}
				  , 0, 2, (byte) 21);
			raw.close();
		}

		/** writes a single SSL Record */
		public void write(byte[] payload, int off, int len, byte type) throws IOException
		{

			// largest permissible frame is 2^14 octets
			if (len > 1 << 14)
			{
				write(payload, off, 1 << 14, type);
				write(payload, off + 1 << 14, len - 1 << 14, type);
				return;
			}

			raw.writeByte(type);
			raw.writeShort(0x0300);

			if (rc4 == null)
			{
				raw.writeShort(len);
				raw.write(payload, off, len);

			}
			else
			{
				byte[] MAC = computeMAC(type, payload, off, len, client_write_MAC_secret, seq_num);
				byte[] encryptedPayload = new byte[MAC.length + len];
				rc4.processBytes(payload, off, len, encryptedPayload, 0);
				rc4.processBytes(MAC, 0, MAC.length, encryptedPayload, len);
				raw.writeShort(encryptedPayload.length);
				raw.write(encryptedPayload);

			}

			seq_num++;
		}

		/** tacks a handshake header onto payload before sending it */
		public void writeHandshake(int type, byte[] payload) throws IOException
		{
			byte[] real_payload = new byte[payload.length + 4];
			System.arraycopy(payload, 0, real_payload, 4, payload.length);
			real_payload[0] = (byte) (type & 0xFF);
			intToBytes(payload.length, real_payload, 1, 3);
			handshakes = concat(new byte[][]
								{handshakes, real_payload});
			write(real_payload, 0, real_payload.length, (byte) 22);
		}

		public void sendClientHandshakes() throws IOException
		{

			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "shaking hands");
			if (cert_requested)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "telling the server we have no certificates");
				writeHandshake(11, new byte[]
							   {0x0, 0x0, 0x0});
			}

			// generate the premaster secret
			byte[] pre_master_secret = new byte[48];
			pre_master_secret[0] = 0x03; // first two bytes of premaster secret are our version number
			pre_master_secret[1] = 0x00;
			getRandomBytes(pre_master_secret, 2, pre_master_secret.length - 2);

			// encrypt and send the pre_master_secret
			try
			{
				byte[] encrypted_pre_master_secret;

				SubjectPublicKeyInfo pki = server_cert.getSubjectPublicKeyInfo();
				RSAPublicKeyStructure rsa_pks = new RSAPublicKeyStructure( (DERConstructedSequence) pki.
					getPublicKey());
				BigInteger modulus = rsa_pks.getModulus();
				BigInteger exponent = rsa_pks.getPublicExponent();

				if (serverKeyExchange != null)
				{

					AsymmetricBlockCipher rsa = new PKCS1(new RSAEngine());
					rsa.init(false, new RSAKeyParameters(false, modulus, exponent));

					int modulus_size = ( (serverKeyExchange[4] & 0xff) << 8) | (serverKeyExchange[5] & 0xff);
					byte[] b_modulus = new byte[modulus_size];
					System.arraycopy(serverKeyExchange, 6, b_modulus, 0, modulus_size);
					modulus = new BigInteger(1, b_modulus);

					int exponent_size = ( (serverKeyExchange[6 + modulus_size] & 0xff) << 8) |
						(serverKeyExchange[7 + modulus_size] & 0xff);
					byte[] b_exponent = new byte[exponent_size];
					System.arraycopy(serverKeyExchange, 8 + modulus_size, b_exponent, 0, exponent_size);
					exponent = new BigInteger(1, b_exponent);

					byte[] server_params = new byte[modulus_size + exponent_size + 4];
					System.arraycopy(serverKeyExchange, 4, server_params, 0, server_params.length);

					byte[] expectedSignature = concat(new byte[][]
						{md5(new byte[][]
							 {client_random, server_random, server_params}),
						sha(new byte[][]
							{client_random, server_random, server_params})});

					byte[] recievedSignature = rsa.processBlock(serverKeyExchange, 6 + server_params.length,
						serverKeyExchange.length - 6 - server_params.length);

					for (int i = 0; i < expectedSignature.length; i++)
					{
						if (expectedSignature[i] != recievedSignature[i])
						{
							throw new SSLException("ServerKeyExchange message had invalid signature " + i);
						}
					}

					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "ServerKeyExchange successfully processed");
				}

				AsymmetricBlockCipher rsa = new PKCS1(new RSAEngine());
				rsa.init(true, new RSAKeyParameters(false, modulus, exponent));

				encrypted_pre_master_secret = rsa.processBlock(pre_master_secret, 0, pre_master_secret.length);
				writeHandshake(16, encrypted_pre_master_secret);

			}
			catch (Exception e)
			{
				SSLException t = new SSLException("exception encrypting premaster secret");
				t.fillInStackTrace();
				throw t;
			}

			// ChangeCipherSpec
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Handshake complete; sending ChangeCipherSpec");
			write(new byte[]
				  {0x01}
				  , 0, 1, (byte) 20);
			seq_num = 0;

			// compute master_secret
			master_secret = concat(new byte[][]
								   {
								   md5(new byte[][]
									   {pre_master_secret,
									   sha(new byte[][]
										   {new byte[]
										   {0x41}
										   , pre_master_secret, client_random, server_random})}),
								   md5(new byte[][]
									   {pre_master_secret,
									   sha(new byte[][]
										   {new byte[]
										   {0x42, 0x42}
										   , pre_master_secret, client_random, server_random})}),
								   md5(new byte[][]
									   {pre_master_secret,
									   sha(new byte[][]
										   {new byte[]
										   {0x43, 0x43, 0x43}
										   , pre_master_secret, client_random, server_random})})
			});

			// construct the key material
			byte[] key_material = new byte[]
				{};
			for (int i = 0; key_material.length < 72; i++)
			{
				byte[] crap = new byte[i + 1];
				for (int j = 0; j < crap.length; j++)
				{
					crap[j] = (byte) ( ( (byte) 0x41) + ( (byte) i));
				}
				key_material = concat(new byte[][]
									  {key_material,
									  md5(new byte[][]
										  {master_secret,
										  sha(new byte[][]
											  {crap, master_secret, server_random, client_random})})});
			}

			client_write_key = new byte[export ? 5 : 16];
			server_write_key = new byte[export ? 5 : 16];

			System.arraycopy(key_material, 0, client_write_MAC_secret, 0, 16);
			System.arraycopy(key_material, 16, server_write_MAC_secret, 0, 16);
			System.arraycopy(key_material, 32, client_write_key, 0, export ? 5 : 16);
			System.arraycopy(key_material, export ? 37 : 48, server_write_key, 0, export ? 5 : 16);

			if (export)
			{
				// see SSLv3 spec, 6.2.2 for explanation
				byte[] client_untrimmed = md5(new byte[][]
											  {concat(new byte[][]
					{client_write_key, client_random, server_random})});
				byte[] server_untrimmed = md5(new byte[][]
											  {concat(new byte[][]
					{server_write_key, server_random, client_random})});
				client_write_key = new byte[16];
				server_write_key = new byte[16];
				System.arraycopy(client_untrimmed, 0, client_write_key, 0, 16);
				System.arraycopy(server_untrimmed, 0, server_write_key, 0, 16);
			}

			rc4 = new RC4Engine();
			rc4.init(true, new KeyParameter(client_write_key));
			is.rc4 = new RC4Engine();
			is.rc4.init(false, new KeyParameter(server_write_key));

			// send Finished
			writeHandshake(20, concat(new byte[][]
									  {
									  md5(new byte[][]
										  {master_secret, pad2,
										  md5(new byte[][]
											  {handshakes, new byte[]
											  { (byte) 0x43, (byte) 0x4C, (byte) 0x4E, (byte) 0x54}
											  ,
											  master_secret, pad1})}),
									  sha(new byte[][]
										  {master_secret, pad2_sha,
										  sha(new byte[][]
											  {handshakes, new byte[]
											  { (byte) 0x43, (byte) 0x4C, (byte) 0x4E, (byte) 0x54}
											  ,
											  master_secret, pad1_sha})})
			}));
			raw.flush();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "wrote Finished message");

		}

		public void writeClientHello() throws IOException
		{

			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "sending ClientHello");
			int unixtime = (int) (System.currentTimeMillis() / (long) 1000);

			byte[] out = new byte[]
				{
				0x03, 0x00, // client version (SSLv3.0)
				// space for random bytes
				0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
				0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
				0x0, 0x0, 0x0, 0x0,

				0x0, // empty vector for sessionid
				0x0, 0x4, 0x0, 0x4, 0x0, 0x3, // we support two ciphersuites: SSL_RSA_WITH_RC4_128_MD5 and SSL_RSA_EXPORT_WITH_RC4_40_MD5
				0x1, 0x0 // we only support one compression method: none
			};

			// don't need to use secure random here since it's sent in the clear
			Random rand = new Random(System.currentTimeMillis());
			rand.nextBytes(client_random);
			intToBytes(unixtime, client_random, 0, 4);
			System.arraycopy(client_random, 0, out, 2, client_random.length);

			writeHandshake(1, out);
			flush();
		}
	}

	// Static Helpers ////////////////////////////////////////////////////////////////////

	/** copy the least significant num bytes of val into byte array b, startint at offset */
	public static void intToBytes(long val, byte[] b, int offset, int num)
	{
		for (int i = 0; i < num; i++)
		{
			b[offset + num - i - 1] = (byte) ( (val & (0xFFL << (i * 8))) >> (i * 8));
		}
	}

	/** fills b with random bytes */
	public static synchronized void getRandomBytes(byte[] b, int offset, int len)
	{
		MD5Digest md5 = new MD5Digest();
		byte[] b2 = new byte[16];
		while (len > 0)
		{
			md5.reset();
			md5.update(randpool, 0, randpool.length);
			intToBytes(randcnt++, b2, 0, 8);
			md5.update(b2, 0, 8);
			md5.doFinal(b2, 0);
			int n = len < 16 ? len : 16;
			System.arraycopy(b2, 0, b, offset, n);
			len -= n;
			offset += n;
		}
	}

	public static byte[] computeMAC(byte type, byte[] payload, int off, int len, byte[] MAC_secret,
									long seq_num)
	{
		byte[] MAC = new byte[16];
		MD5Digest md5 = new MD5Digest();
		md5.update(MAC_secret, 0, MAC_secret.length);
		md5.update(pad1, 0, pad1.length);

		byte[] b = new byte[11];
		intToBytes(seq_num, b, 0, 8);
		b[8] = type;
		intToBytes(len, b, 9, 2);
		md5.update(b, 0, b.length);

		md5.update(payload, off, len);
		md5.doFinal(MAC, 0);
		md5.reset();
		md5.update(MAC_secret, 0, MAC_secret.length);
		md5.update(pad2, 0, pad2.length);
		md5.update(MAC, 0, MAC.length);
		md5.doFinal(MAC, 0);

		return MAC;
	}

	public static byte[] concat(byte[][] inputs)
	{
		int total = 0;
		for (int i = 0; i < inputs.length; i++)
		{
			total += inputs[i].length;
		}
		byte[] ret = new byte[total];
		int pos = 0;
		for (int i = 0; i < inputs.length; i++)
		{
			System.arraycopy(inputs[i], 0, ret, pos, inputs[i].length);
			pos += inputs[i].length;
		}
		return ret;
	}

	SHA1Digest master_sha1 = new SHA1Digest();
	public byte[] sha(byte[][] inputs)
	{
		master_sha1.reset();
		for (int i = 0; i < inputs.length; i++)
		{
			master_sha1.update(inputs[i], 0, inputs[i].length);
		}
		byte[] ret = new byte[master_sha1.getDigestSize()];
		master_sha1.doFinal(ret, 0);
		return ret;
	}

	MD5Digest master_md5 = new MD5Digest();
	public byte[] md5(byte[][] inputs)
	{
		master_md5.reset();
		for (int i = 0; i < inputs.length; i++)
		{
			master_md5.update(inputs[i], 0, inputs[i].length);
		}
		byte[] ret = new byte[master_md5.getDigestSize()];
		master_md5.doFinal(ret, 0);
		return ret;
	}

	// FEATURE: improve error reporting in here
	/** returns true iff certificate "signee" is signed by public key "signer" */
	public static boolean isSignedBy(X509CertificateStructure signee, SubjectPublicKeyInfo signer) throws
		SSLException
	{

		Digest hash = null;

		String signature_algorithm_oid = signee.getSignatureAlgorithm().getObjectId().getId();
		if (signature_algorithm_oid.equals("1.2.840.113549.1.1.4"))
		{
			hash = new MD5Digest();
		}
		else if (signature_algorithm_oid.equals("1.2.840.113549.1.1.2"))
		{
			hash = new MD2Digest();
		}
		else if (signature_algorithm_oid.equals("1.2.840.113549.1.1.5"))
		{
			hash = new SHA1Digest();
		}
		else
		{
			throw new SSLException("unsupported signing algorithm: " + signature_algorithm_oid);
		}

		try
		{
			// decrypt the signature using the signer's public key
			byte[] ED = signee.getSignature().getBytes();
			SubjectPublicKeyInfo pki = signer;
			RSAPublicKeyStructure rsa_pks = new RSAPublicKeyStructure( (DERConstructedSequence) pki.
				getPublicKey());
			BigInteger modulus = rsa_pks.getModulus();
			BigInteger exponent = rsa_pks.getPublicExponent();
			AsymmetricBlockCipher rsa = new PKCS1(new RSAEngine());
			rsa.init(false, new RSAKeyParameters(false, modulus, exponent));

			// Decode the embedded octet string
			byte[] D = rsa.processBlock(ED, 0, ED.length);
			BERInputStream beris = new BERInputStream(new ByteArrayInputStream(D));
			DERObject derob = beris.readObject();
			DERConstructedSequence dercs = (DERConstructedSequence) derob;
			DEROctetString deros = (DEROctetString) dercs.getObjectAt(1);
			byte[] MD = deros.getOctets();

			// generate our own hash
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DEROutputStream dos = new DEROutputStream(baos);
			dos.writeObject(signee.getTBSCertificate());
			dos.flush();
			byte[] b = baos.toByteArray();
			hash.update(b, 0, b.length);
			byte[] md_out = new byte[MD.length];
			hash.doFinal(md_out, 0);

			// compare our hash to the signed hash
			for (int j = 0; j < MD.length; j++)
			{
				if (md_out[j] != MD[j])
				{
					return false;
				}
			}
			return true;

		}
		catch (Exception e)
		{
			return false;

		}
	}

	public static boolean alwaysFalse = false;

	static class entropySpinner extends Thread
	{
		volatile boolean stop = false;
		byte counter = 0;
		entropySpinner()
		{
			start();
		}

		public void run()
		{
			while (true)
			{
				counter++;

				// without this line, GCJ will over-optimize this loop into an infinite loop. Argh.
				if (alwaysFalse)
				{
					stop = true;

				}
				if (stop)
				{
					return;
				}
			}
		}
	}

	static
	{

		entropySpinner[] spinners = new entropySpinner[10];
		for (int i = 0; i < spinners.length; i++)
		{
			spinners[i] = new entropySpinner();

		}
		for (int i = 0; i < pad1.length; i++)
		{
			pad1[i] = (byte) 0x36;
		}
		for (int i = 0; i < pad2.length; i++)
		{
			pad2[i] = (byte) 0x5C;
		}
		for (int i = 0; i < pad1_sha.length; i++)
		{
			pad1_sha[i] = (byte) 0x36;
		}
		for (int i = 0; i < pad2_sha.length; i++)
		{
			pad2_sha[i] = (byte) 0x5C;

		}

		//RootCertificates rootCerts = new RootCertificates();
		//rootCerts.init();
		//trusted_CA_public_keys = rootCerts.trusted_CA_public_keys;
		//trusted_CA_public_key_identifiers = rootCerts.trusted_CA_public_key_identifiers;

		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "generating entropy...");
		randpool = new byte[10];
		try
		{
			Thread.sleep(100);
		}
		catch (Exception e)
		{}
		for (int i = 0; i < spinners.length; i++)
		{
			spinners[i].stop = true;
			randpool[i] = spinners[i].counter;
		}

		MD5Digest md5 = new MD5Digest();
		md5.update(randpool, 0, randpool.length);
		intToBytes(System.currentTimeMillis(), randpool, 0, 4);
		md5.update(randpool, 0, 4);
		intToBytes(Runtime.getRuntime().freeMemory(), randpool, 0, 4);
		md5.update(randpool, 0, 4);
		intToBytes(Runtime.getRuntime().totalMemory(), randpool, 0, 4);
		md5.update(randpool, 0, 4);
		intToBytes(System.identityHashCode(TinySSL.class), randpool, 0, 4);
		md5.update(randpool, 0, 4);
		Properties p = System.getProperties();
		for (Enumeration e = p.propertyNames(); e.hasMoreElements(); )
		{
			String s = (String) e.nextElement();
			byte[] b = s.getBytes();
			md5.update(b, 0, b.length);
			b = p.getProperty(s).getBytes();
			md5.update(b, 0, b.length);
		}
		randpool = new byte[md5.getDigestSize()];
		md5.doFinal(randpool, 0);

		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "TinySSL is initialized.");
	}

	/**
	 *  A PKCS1 encoder which uses TinySSL's built-in PRNG instead of java.security.SecureRandom.
	 *  This code was derived from BouncyCastle's org.bouncycastle.crypto.encoding.PKCS1Encoding.
	 */
	private static class PKCS1 implements AsymmetricBlockCipher
	{
		private static int HEADER_LENGTH = 10;
		private AsymmetricBlockCipher engine;
		private boolean forEncryption;
		private boolean forPrivateKey;

		public PKCS1(AsymmetricBlockCipher cipher)
		{
			this.engine = cipher;
		}

		public AsymmetricBlockCipher getUnderlyingCipher()
		{
			return engine;
		}

		public void init(boolean forEncryption, CipherParameters param)
		{
			engine.init(forEncryption, (AsymmetricKeyParameter) param);
			this.forPrivateKey = ( (AsymmetricKeyParameter) param).isPrivate();
			this.forEncryption = forEncryption;
		}

		public int getInputBlockSize()
		{
			return engine.getInputBlockSize() - (forEncryption ? HEADER_LENGTH : 0);
		}

		public int getOutputBlockSize()
		{
			return engine.getOutputBlockSize() - (forEncryption ? 0 : HEADER_LENGTH);
		}

		public byte[] processBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException
		{
			return forEncryption ? encodeBlock(in, inOff, inLen) : decodeBlock(in, inOff, inLen);
		}

		private byte[] encodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException
		{
			byte[] block = new byte[engine.getInputBlockSize()];
			if (forPrivateKey)
			{
				block[0] = 0x01; // type code 1
				for (int i = 1; i != block.length - inLen - 1; i++)
				{
					block[i] = (byte) 0xFF;
				}
			}
			else
			{
				getRandomBytes(block, 0, block.length);
				block[0] = 0x02; // type code 2

				// a zero byte marks the end of the padding, so all
				// the pad bytes must be non-zero.
				for (int i = 1; i != block.length - inLen - 1; i++)
				{
					while (block[i] == 0)
					{
						getRandomBytes(block, i, 1);
					}
				}
			}

			block[block.length - inLen - 1] = 0x00; // mark the end of the padding
			System.arraycopy(in, inOff, block, block.length - inLen, inLen);
			return engine.processBlock(block, 0, block.length);
		}

		private byte[] decodeBlock(byte[] in, int inOff, int inLen) throws InvalidCipherTextException
		{
			byte[] block = engine.processBlock(in, inOff, inLen);
			if (block.length < getOutputBlockSize())
			{
				throw new InvalidCipherTextException("block truncated");
			}
			if (block[0] != 1 && block[0] != 2)
			{
				throw new InvalidCipherTextException("unknown block type");
			}

			// find and extract the message block.
			int start;
			for (start = 1; start != block.length; start++)
			{
				if (block[start] == 0)
				{
					break;
				}
			}
			start++; // data should start at the next byte

			if (start >= block.length || start < HEADER_LENGTH)
			{
				throw new InvalidCipherTextException("no data in block");
			}

			byte[] result = new byte[block.length - start];
			System.arraycopy(block, start, result, 0, result.length);
			return result;
		}
	}

}

/*
Copyright (c) 2000, The JAP-Team
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
import java.security.SecureRandom;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import javax.xml.parsers.*;
//import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import java.security.*;
import java.io.*;
import java.util.*;
//import au.net.aba.security.cert.*;
import java.math.BigInteger;
import sun.security.x509.X509Cert;
import sun.security.pkcs.PKCS8Key;
import sun.security.pkcs.PKCS7;
import sun.security.x509.X509Key;
import sun.misc.BASE64Encoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.BigInt;
//import cryptix.jce.provider.Cryptix;
//import cryptix.jce.provider.CryptixCrypto;
//import java.security.Security;
//import cryptix.jce.provider.asn.*;
//import java.security.interfaces.RSAPublicKey;
//import javax.crypto.Cipher;
//import cryptix.jce.provider.key.RawSecretKey;
//import java.security.cert.CertificateFactory;
//import org.logi.crypto.keys.BlowfishKey;
//import iaik.security.provider.IAIK;
//import java.security.cert.X509Certificate;
import java.net.*;
import java.security.interfaces.*;
//import cryptix.jce.provider.rsa.*;
//import java.security.spec.InvalidKeySpecException;

import Rijndael.Rijndael_Algorithm;
//import com.sun.javaws.jardiff.*;
import java.util.zip.*;
//import cryptix.asn1.lang.*;
//import cryptix.asn1.encoding.*;
import anon.AnonService;
import anon.AnonServer;
import anon.AnonChannel;
import anon.AnonServiceFactory;
final public class JAPTest
{

	public static void main(String argc[])
		{
      try{
      AnonService s=AnonServiceFactory.create();
      s.connect(new AnonServer("mix.inf.tu-dresden.de",16544));
      InetAddress addr=InetAddress.getByName("dud33.inf.tu-dresden.de");
      AnonChannel c=s.createChannel(addr,9999);
      InputStream in=c.getInputStream();
      OutputStream out=c.getOutputStream();
      out.write("GET /index.html HTTP/1.0\n\r\n\r".getBytes());
      byte[] buff=new byte[1000];
      int len=0;
      while((len=in.read(buff,0,buff.length))>0)
        System.out.println(new String(buff,0,len));
        }
      catch(Exception e)
        {
          e.printStackTrace();
        }
      System.exit(0);
	/*	try{FileOutputStream fio=new FileOutputStream("test.test");
			fio.write(3);
			fio.flush();
			fio.close();
			}
		catch(Exception e)
											 {
												e.printStackTrace();
											 }
*/	//		readSig();
	//		readDSAPrivKey();
		//	testCertAba();
	//		readCerts();
		/*
			int MAX=128000;
			JAPASymCipher oRSA=new JAPASymCipher();
			byte[] buff=new byte[MAX];
			byte[] out=new byte[MAX];
			System.out.println("Init Random..");
			SecureRandom sr=new SecureRandom(SecureRandom.getSeed(20));
			System.out.println("Filling Buff..");
			sr.nextBytes(buff);
			System.out.println("Encrypting..");
			for(int i=0;i<MAX;i+=128)
				{
					buff[i]=(byte)(buff[i]&0x7F);
					oRSA.encrypt(buff,i,out,i);
				}
			System.out.println("done..");
			try{
			FileOutputStream io=new FileOutputStream("plain.bytes");
			io.write(buff);
			io.flush();
			io.close();
			io=new FileOutputStream("crypt.bytes");
			io.write(out);
			io.flush();
			io.close();}
			catch(Exception z)
			{z.printStackTrace();}
		*/
	//	readCertJCE();
//		testCert();
//		System.getProperties().list(System.out);
//	networkTest(argc);
	//testRRT();
		//testSymCipherCryptix();
/*		try{
			byte[] addr=InetAddress.getByName("passat.mesh.de").getAddress();
		String s="/feedback/"+Integer.toString(Math.abs(addr[0]))+"."+
																							 Integer.toString(addr[1])+"."+Byte.toString(addr[2])+"."+
						 Byte.toString(addr[3]);
			s=s+" ";}
		catch(Exception e)
											 {
												e.printStackTrace();
											 }
	*/
		//testJarDiff();
//		ASN1Test();
	//	JAPTest t=new JAPTest();
	//	t.testJarVerify();
//		ASN1Test();

		try{
	//		ServerSocket s=new ServerSocket(5002);
	//		Socket t=s.accept();
	//		Thread.sleep(50000);
	//		writeXml();
		testMixe();
		}catch(Exception e)
		{
		}
		System.exit(0);
		}

	public static void networkTest2()
		{
		try{
			ServerSocket os=new ServerSocket(9000);
			Socket oSocket=os.accept();
			InputStream out=oSocket.getInputStream();
			Thread.sleep(100000000);
			out.close();
		}
		catch(Exception e){e.printStackTrace();}
		}

	public static void networkTest(String argc[])
		{
		try{
			Socket oSocket=new Socket(argc[0],9000);
			OutputStream out=oSocket.getOutputStream();
			byte[] b=new byte[1];
			b[0]=3;
			out.write(b);
			out.flush();
			System.out.println("Writen one byte");
			b=new byte[9];
			System.out.println("Sleeping");
			Thread.sleep(1000);
			out.write(b);
			out.flush();
			System.out.println("Writen 999 byte");
			System.out.println("Infinite Sleeping");
			Thread.sleep(100000);
			out.close();
		}
		catch(Exception e){e.printStackTrace();}
		}

	public static void testCertAba()
	{
		try
		{
/*			java.security.Security.addProvider(new au.net.aba.crypto.provider.ABAProvider());
			CertificateFactory cf=CertificateFactory.getInstance("X509","ABA");
			FileInputStream f=new FileInputStream("ldbsh.cer");
			X509Certificate cer=(X509Certificate)cf.generateCertificate(f);
			f.close();
			PublicKey key=cer.getPublicKey();
			cer.verify(key);
	*/		System.out.println("Verified MAster");
		}
		catch(Exception e)
											 {
			e.printStackTrace();
											 }
	}

	public static void writeXml()
		{
			try {
			FileOutputStream f=null;
			f=new FileOutputStream("test.xml");
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			Transformer t=TransformerFactory.newInstance().newTransformer();
			Result r=new StreamResult(f);
			Source s=new DOMSource(doc);
			t.transform(s,r);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}



	public static void testCert()
		{
		try{
		/*	JAPCertificate master=new JAPCertificate();
			FileInputStream f=new FileInputStream("ldbsh.cer");
			master.decode(f);
			f.close();
			if(!master.verify(master.getPublicKey()))
				{
					System.out.println("master Certificate invalid..");
					System.exit(0);
				}
			System.out.println("Verifyed master");
			JAPCertificate cert=new JAPCertificate();
			f=new FileInputStream("jap.cer");
			cert.decode(f);
			f.close();
			if(!cert.verify(master.getPublicKey()))
				{
					System.out.println("slave Certificate invalid..");
					System.exit(0);
				}
			System.out.println("Verifyed slave");
			JAPCertificate c=new JAPCertificate();
			f=new FileInputStream("mix1.cer");
			c.decode(f);
			f.close();
			if(!c.verify(cert.getPublicKey()))
				{
					System.out.println("mix1 Certificate invalid..");
				}
			System.out.println("mix1 verifyed");
			FileOutputStream out=new FileOutputStream("base64.cer");
			c.encode(out,JAPCertificate.BASE64);
			out.close();
	*/
		//	Security.addProvider(new CryptixCrypto());
		/*	Cipher cipherEnc=Cipher.getInstance("Blowfish/ECB/None");
			Cipher cipherDec=Cipher.getInstance("Blowfish/ECB/None");
			byte[]key=new byte[16];
			RawSecretKey k=new RawSecretKey("Blowfish",key);
					try{cipherEnc.init(Cipher.ENCRYPT_MODE,k);}
					catch(Exception e){e.printStackTrace();}
					try{cipherDec.init(Cipher.DECRYPT_MODE,k);}
					catch(Exception e){e.printStackTrace();}
			byte[] buff=new byte[128];
			for(int i=0;i<buff.length;i++)
				buff[i]=(byte)i;
			cipherEnc.doFinal(buff,0,buff.length,buff,0);
			JAPSymCipher g=new JAPSymCipher();
			g.setDecryptionKey(key);
		//	g.decrypt(buff);
			cipherDec.doFinal(buff);
			for(int i=0;i<buff.length;i++)
				System.out.print(buff[i]+":");

		*/		FileInputStream f=new FileInputStream("ldbsh.cer");
			X509Cert cert=new X509Cert();
			cert.decode(f);
			System.out.println(cert.getFormat());
			System.out.println(cert.getGuarantor().getName());
	//		System.out.println(cert.getIssuerName().getState());
			System.out.println(cert.getPublicKey().getAlgorithm());
			System.out.println(cert.getPublicKey().getFormat());
//			f=new FileInputStream("test.p7b");
/*			PKCS8Key p8;
			p8.decode(
			PKCS7 p7=new PKCS7(f);
			System.out.println(p7.toString());
	*/
			X509Cert master=new X509Cert();
			f=new FileInputStream("jap.cer");
			master.decode(f);
	//		PublicKey k=cert.getPublicKey();
	//		RSAPublicKey kp=transformKey(k);
		//	sun.security.x509.X509Key kx=(sun.security.x509.X509Key)k;

		//	master.verify(kp);
		//	byte[] key=master.getPublicKey().getEncoded();

	/*		for(int i=0;i<key.length;i++)
				System.out.print(Integer.toHexString((key[i]&0x00FF))+":");
	*///		DerInputBuffer derbuff=new DerInputBuffer();
		//	derbuff.read;

	/*			DerInputStream in=new DerInputStream(key);
				System.out.println(in.available());
				System.out.println(in.toString());
				DerValue d=in.getDerValue();
		//		System.out.println(d.data.getClass().toString());
				System.out.println(d.toString());
				DerValue d2=d.data.getDerValue();
				System.out.println(d2.toString());
				d2=d.data.getDerValue();
				System.out.println(d2.toString());
				System.out.println(d2.tag_BitString);
				DerInputStream in3=new DerInputStream(d2.getBitString());
				DerValue d3=in3.getDerValue();
				System.out.println(d3);
				BigInteger n=d3.data.getInteger().toBigInteger();
				System.out.println(n.toString());
				BigInteger e=d3.data.getInteger().toBigInteger();
				System.out.println(e.toString());

			System.out.println(master.getFormat());
			System.out.println(master.getGuarantor().getName());
	//		System.out.println(master.getIssuerName().getState());
			System.out.println(master.getPublicKey().getAlgorithm());
			System.out.println(master.getPublicKey().getFormat());


			class ck implements RSAPublicKey
				{
				    public String getAlgorithm()
    {
        return "RSA";
    }

		public byte[] getEncoded()
		{
			System.out.println("getEncode called");
			return new byte[20];
		}
    public String getFormat()
    {
        return "X.509";
    }

					BigInteger e,n;
					public BigInteger getModulus()
																{
			System.out.println("getModulus called");
							return n;
																}
				public BigInteger getPublicExponent()
																{
			System.out.println("getPublic called"+e.toString());
							return e;
																}
				public ck(BigInteger f,BigInteger g)
				{
					n=f;
					e=g;
				}

			}
			ck h=new ck(n,e);
			master.verify(h);
			cert.verify(h);*/
			System.exit(0);

		}
		catch(Exception z)
		{
			z.printStackTrace();
		}
		}


/*	public static void readCertJCE()
	{
		try{
			java.security.Security.addProvider(new IAIK());
			CertificateFactory cf=CertificateFactory.getInstance("X.509");
			FileInputStream in=new FileInputStream("ldbsh.cer");
			X509Certificate master=(X509Certificate)cf.generateCertificate(in);
			in=new FileInputStream("jap.cer");
			X509Certificate slave=(X509Certificate)cf.generateCertificate(in);
			slave.verify(master.getPublicKey());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	*/
	public static void readCerts()
	{
		try{
			FileInputStream f=new FileInputStream("cert.xml");
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element nd=(Element)doc.getFirstChild();
			NodeList certs=nd.getElementsByTagName("certifcate");
			for(int i=0;i<certs.getLength();i++)
			{
				Element e=(Element)certs.item(i);
				NodeList childs=e.getChildNodes();
				for(int j=0;j<childs.getLength();j++)
					{
						if(childs.item(j).getNodeName()=="base64")
							{
								JAPCertificate c=new JAPCertificate();
								String s=childs.item(j).getFirstChild().getNodeValue();
								s.getBytes();
								c.decode(s.getBytes(),JAPCertificate.BASE64);
								System.out.println(c.toString());
						}
					}
			}
		//	System.out.println(cert1.getNodeType());
		//	System.out.println(Node.TEXT_NODE);
		//	System.out.println(cert1.getNodeValue());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	static void readDSAPrivKey()
	{
		try{
			byte[] buff;
			FileInputStream f=new FileInputStream("privkey.der");
			buff=new byte[f.available()];
			f.read(buff);
		DerInputStream in=new DerInputStream(buff);
		DerValue d=in.getDerValue();
		System.out.println(d.tag);

		DerInputStream in1=d.data;
		DerValue d1 =in1.getDerValue();
		System.out.println(d1.tag);
		System.out.println(d1.toString());

		BigInt b=d1.getInteger();
		System.out.println(b.toBigInteger().toString(16));

		DerValue d2 =in1.getDerValue();
		System.out.println(d2.tag);
		System.out.println(d2.toString());
		d2 =in1.getDerValue();
		System.out.println(d2.tag);
		System.out.println(d2.toString());
		byte[] bu=d2.getOctetString();
		System.out.println(bu.length);

		in1=new DerInputStream(bu);
		d1=in1.getDerValue();
		System.out.println(d1.toString());
		BigInt b3=d1.getInteger();
		System.out.println(b3.toBigInteger().toString(16));
		System.exit(0);

		/*DerInputStream in2=d2.data;
		DerValue d3 =in2.getDerValue();
		System.out.println(d3.tag);
		System.out.println(d3.toString());

		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		in2=d3.data;
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		BigInt b1=d3.getInteger();
		System.out.println(b1.toBigInteger().toString(16));
		*/

		}
		catch(Exception e)
		{
		e.printStackTrace();
		};
		 System.exit(0);
	}

	static void readSig()
	{
		try{
			byte[] buff;
			FileInputStream f=new FileInputStream("sig.der");
			buff=new byte[f.available()];
			f.read(buff);
			JAPCertificate c=new JAPCertificate();
			f=new FileInputStream("mix1.cer");
			c.decode(f);
			f.close();
			Signature si=Signature.getInstance("DSA");
			si.initVerify(c.getPublicKey());
			byte[] b1=new byte[10];
			for(int t=0;t<10;t++)
				b1[t]=0;
			si.update(b1);
			System.out.println(si.verify(buff));
			System.exit(0);
			DerInputStream in=new DerInputStream(buff);
		DerValue d=in.getDerValue();
		System.out.println(d.toString());

		DerInputStream in1=d.data;
		DerValue d1 =in1.getDerValue();
		System.out.println(d1.toString());
		 d1 =in1.getDerValue();
		System.out.println(d1.toString());
		System.exit(0);
		BigInt b=d1.getInteger();
		System.out.println(b.toBigInteger().toString(16));

		DerValue d2 =in1.getDerValue();
		System.out.println(d2.tag);
		System.out.println(d2.toString());
		d2 =in1.getDerValue();
		System.out.println(d2.tag);
		System.out.println(d2.toString());
		byte[] bu=d2.getOctetString();
		System.out.println(bu.length);

		in1=new DerInputStream(bu);
		d1=in1.getDerValue();
		System.out.println(d1.toString());
		BigInt b3=d1.getInteger();
		System.out.println(b3.toBigInteger().toString(16));
		System.exit(0);

		/*DerInputStream in2=d2.data;
		DerValue d3 =in2.getDerValue();
		System.out.println(d3.tag);
		System.out.println(d3.toString());

		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		in2=d3.data;
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		d3 =in2.getDerValue();
		System.out.println(d3.toString());
		BigInt b1=d3.getInteger();
		System.out.println(b1.toBigInteger().toString(16));
		*/

		}
		catch(Exception e)
		{
		e.printStackTrace();
		};
		 System.exit(0);
	}
/*
	static RSAPublicKey transformKey(PublicKey k) throws InvalidKeySpecException
	{
		try {
            AsnInputStream ais = new AsnInputStream( k.getEncoded() );
            AsnSequence seq = (AsnSequence)ais.read();
            if (seq.size() != 2)
                throw new InvalidKeySpecException(
                    "First SEQUENCE has " + seq.size() + " elements.");

            // XXX: check for valid AlgOID

           // AsnObject uh = seq.get(0);

            AsnBitString bs = (AsnBitString)seq.get(1);
            ais = new AsnInputStream( bs.toByteArray() );

            seq = (AsnSequence)ais.read();
            if (seq.size() != 2)
                throw new InvalidKeySpecException(
                    "Second SEQUENCE has " + seq.size() + " elements.");

            AsnInteger n = (AsnInteger)seq.get(0);
            AsnInteger e = (AsnInteger)seq.get(1);

            return new RSAPublicKeyImpl(n.toBigInteger(), e.toBigInteger());

        } catch(ClassCastException e) {
            throw new InvalidKeySpecException(
                "Unexpected ASN.1 type detected: " + e.getMessage() );

        } catch(IOException e) {
            throw new InvalidKeySpecException("Could not parse key.");
        }
    }
	*/
	static void testRRT()
		{
			try
				{
					DatagramSocket oSocket=new DatagramSocket();
					byte[] buff=new byte[100];
					long time=System.currentTimeMillis();
					BigInteger b=new BigInteger(Long.toString(time));
					byte[] timebuff=b.toByteArray();
					System.arraycopy(timebuff,0,buff,4+8-timebuff.length,timebuff.length);
					buff[0]=(byte)0x80;
					int localPort=oSocket.getLocalPort();
					buff[16]=(byte)(localPort>>8);
					buff[17]=(byte)(localPort&0xFF);
					//InetAddress returnAddr=oSocket.getLocalAddress();

					System.arraycopy(InetAddress.getLocalHost().getAddress(),0,buff,12,4);
					DatagramPacket oPacket=new DatagramPacket(buff,4+8+6);
					InetAddress addr=InetAddress.getAllByName("anon.inf.tu-dresden.de")[0];
					oPacket.setAddress(addr);
					oPacket.setPort(8001);
					oSocket.send(oPacket);
					oSocket.receive(oPacket);
					byte[] tmpBuff=new byte[8];
					System.arraycopy(oPacket.getData(),4,tmpBuff,0,8);
					b=new BigInteger(1,tmpBuff);
					System.out.println(b.toString(10));
				}
			catch(Exception e)
				{
					e.printStackTrace();
				}
		}
	/*
	static void testSymCipherCryptix()
		{
		try{
			JAPDebug d=JAPDebug.create();
			java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
			JAPSymCipher c=new JAPSymCipher();
			byte[]k=new byte[16];
			byte[] in=new byte[16];
			byte[] out=new byte[16];
			c.setEncryptionKey(k);
			c.setEncryptionKeyL(k);
			long l=System.currentTimeMillis();
			//for(int i=0;i<2;i++)
				c.encrypt(in);
				Thread.sleep(1000);
				c.encrypt(in);
			l=System.currentTimeMillis()-l;
			System.out.println("Zeit [ms]: "+Long.toString(l));
			l=System.currentTimeMillis();
	//		for(int i=0;i<2;i++)
				c.encryptL(out);
				Thread.sleep(1000);
				c.encryptL(out);
			l=System.currentTimeMillis()-l;
			System.out.println("Zeit [ms]: "+Long.toString(l));
			for(int i=0;i<1000;i++)
				if(in[i]!=out[i])
					System.out.print(i);
*//*			c.setDecryptionKeyL(k);
			c.setDecryptionKey(k);
			c.setEncryptionKeyL(k);
			byte[] in=new byte[1000];
			byte[] out=new byte[1000];
			c.encryptL(in);
			c.encryptL(in);
			c.encrypt(out);
			c.encrypt(out);
			c.encryptL(in);
			c.encrypt(in);
			c.decrypt(in);
			c.decryptL(in);
			c.decrypt(in);
			c.decryptL(in);
		*/
		//Init Crypto...
	//	}catch(Exception e)
	//	{
	//		e.printStackTrace();
		//}
	//	}
/*
public static void testAES()
{
	try{

			JAPDebug d=JAPDebug.create();
			JAPSymCipher c=new JAPSymCipher();
			byte[]k=new byte[16];
			byte[] in=new byte[1000];
			c.setEncryptionKeyAES(k);
			long l=System.currentTimeMillis();
			for(int i=0;i<1000;i++)
				c.encryptAES(in);
			l=System.currentTimeMillis()-l;
			System.out.println("Zeit [ms]: "+Long.toString(l));
	}
	catch(Exception e)
	{
		e.printStackTrace();
	}
}

*/
public static void testJarDiff()
{
	try{
			ZipFile zold=null;//new ZipInputStream(new FileInputStream("g:/projects/jap/classes/JAPold.jar"));
			ZipInputStream zdiff=null;//new ZipInputStream(new FileInputStream("g:/projects/jap/classes/test.jar"));
			ZipOutputStream znew=null;//new ZipOutputStream(new FileOutputStream("g:/projects/jap/classes/JAPnew.jar"));
			ZipEntry ze=null;
// geting old names
			zold=new ZipFile("g:/projects/jap/classes/JAPold.jar");
			Hashtable oldnames=new Hashtable();
			Enumeration e=zold.entries();
			while(e.hasMoreElements())
				{
					ze=(ZipEntry)e.nextElement();
					oldnames.put(ze.getName(),ze.getName());
				}
			zdiff=new ZipInputStream(new FileInputStream("g:/projects/jap/classes/test.jar"));
			znew=new ZipOutputStream(new FileOutputStream("g:/projects/jap/classes/JAPnew.jar"));
			znew.setLevel(9);
			byte[] b=new byte[5000];
			while((ze=zdiff.getNextEntry())!=null)
				{
					ZipEntry zeout=new ZipEntry(ze.getName());
					if(!ze.getName().equalsIgnoreCase("META-INF/INDEX.JD"))
						{
							System.out.println(ze.getName());
							oldnames.remove(ze.getName());
							int s=-1;
							zeout.setTime(ze.getTime());
							zeout.setComment(ze.getComment());
							zeout.setExtra(ze.getExtra());
							zeout.setMethod(ze.getMethod());
							if(ze.getSize()!=-1)
								zeout.setSize(ze.getSize());
							if(ze.getCrc()!=-1)
								zeout.setCrc(ze.getCrc());
							znew.putNextEntry(zeout);
							while((s=zdiff.read(b,0,5000))!=-1)
								{
									znew.write(b,0,s);
								}
							znew.closeEntry();
						}
					else
						{
							BufferedReader br=new BufferedReader(new InputStreamReader(zdiff));
							String s=null;
							while((s=br.readLine())!=null)
								{
									StringTokenizer st=new StringTokenizer(s);
									s=st.nextToken();
									if(s.equalsIgnoreCase("remove"))
										oldnames.remove(st.nextToken());
									else if(s.equalsIgnoreCase("move"))
										System.out.println("move "+st.nextToken());
									else
										System.out.println("unkown: "+s);
								}
						}
					zdiff.closeEntry();
				}
			e=oldnames.elements();
			while(e.hasMoreElements())
				{
					String s=(String)e.nextElement();
					System.out.println(s);
					ze=zold.getEntry(s);
					ZipEntry zeout=new ZipEntry(ze.getName());
					zeout.setTime(ze.getTime());
					zeout.setComment(ze.getComment());
					zeout.setExtra(ze.getExtra());
					zeout.setMethod(ze.getMethod());
					if(ze.getSize()!=-1)
						zeout.setSize(ze.getSize());
					if(ze.getCrc()!=-1)
						zeout.setCrc(ze.getCrc());
					znew.putNextEntry(zeout);
					System.out.println("Getting in..");
					InputStream in=zold.getInputStream(ze);
					int l=-1;
					System.out.println("Reading..");
					try{
					while((l=in.read(b,0,5000))!=-1)
						{
							znew.write(b,0,l);
					}}
					catch(Exception er)
					{
						er.printStackTrace(System.out);
					}
					in.close();
					znew.closeEntry();

				}

			znew.finish();
			znew.flush();
			znew.close();
			zold.close();
			zdiff.close();
	}
	catch(Throwable e)
	{
		e.printStackTrace();
	}
}
	/*
	class MyDSAPublicKey implements DSAPublicKey,DSAParams
	{
		BigInteger y;
		BigInteger p;
		BigInteger q;
		BigInteger g;
		byte [] enc;
		public MyDSAPublicKey(byte[] encKey)
		{
			try{

			enc=encKey;
			ByteArrayInputStream bin=new ByteArrayInputStream(encKey);
			ASNCoder bc=BaseCoder.getInstance("DER");
			bc.init(bin);
			ASNInteger P=new ASNInteger(true);
			ASNInteger Q=new ASNInteger(true);
			ASNInteger G=new ASNInteger(true);

			ASNSequence dsaparameters=new ASNSequence(true);
			dsaparameters.addComponent("p",P);
			dsaparameters.addComponent("q",Q);
			dsaparameters.addComponent("g",G);

			ASNSequence algorithmidentifier=new ASNSequence(true);
			ASNObject algorithmid=new ASNObjectIdentifier(true);
			algorithmidentifier.addComponent("algorithm",algorithmid);
			algorithmidentifier.addComponent("parameters",dsaparameters);

			ASNSequence publickey=new ASNSequence(true);
			publickey.addComponent("algorithm",algorithmidentifier);
			publickey.addComponent("publickey",new ASNBitString(true));

			bc.visit(publickey);
			byte[] pk=(byte[])publickey.getComponent("publickey").getValue();
			ASNInteger Y=new ASNInteger(true);
			bin=new ByteArrayInputStream(pk,1,pk.length-1);
			bc.init(bin);
			bc.visit(Y);
			y=(BigInteger)Y.getValue();
			p=(BigInteger)P.getValue();
			q=(BigInteger)Q.getValue();
			g=(BigInteger)G.getValue();

			//DerValue der=new DerValue(pk);
			//System.out.println(der);


			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		public BigInteger getY(){return y;}
		public BigInteger getP(){return p;}
		public BigInteger getQ(){return q;}
		public BigInteger getG(){return g;}
		public byte []getEncoded(){return enc;}
		public String getAlgorithm(){return "DSA";}
		public String getFormat(){return "X.509";}
		public DSAParams getParams(){return this;}
	}

public void testJarVerify()
{
	try
	{
		FileInputStream f=new FileInputStream("h:/temp/JAP.class");
		byte[] buff=new byte[50000];
		int s=-1;
		int p=0;
		while((s=f.read(buff,p,50000-p))!=-1)
		{
			p+=s;
		}
		MessageDigest md=MessageDigest.getInstance("SHA");
		md.update(buff,0,p);
		byte[] dig=md.digest();
		BASE64Encoder be=new BASE64Encoder();
		String enc=be.encodeBuffer(dig);
		System.out.println(enc);
		String st="Name: JAP.class\r\nSHA1-Digest: vqtchNwSD10AjvDir5z98WKio5g=\r\n\r\n";
		byte [] stb=st.getBytes();
//String st="vqtchNwSD10AjvDir5z98WKio5g=";
		md.update(stb);
		dig=md.digest();
		BASE64Encoder be1=new BASE64Encoder();
		enc=be1.encodeBuffer(dig);
		System.out.println(enc);
		ZipFile zold=new ZipFile("h:/JAP.jar");
		ZipEntry ze=zold.getEntry("META-INF/JAP.SF");
		InputStream in=zold.getInputStream(ze);
		byte[] japsf=new byte[50000];
		int sflen;
		s=-1;
		p=0;
		while((s=in.read(japsf,p,50000-p))!=-1)
		{
			p+=s;
		}
		sflen=p;
		in.close();
		ze=zold.getEntry("META-INF/JAP.DSA");
		in=zold.getInputStream(ze);
		s=-1;
		p=0;
		byte[] japdsa=new byte[50000];
		while((s=in.read(japdsa,p,50000-p))!=-1)
		{
			p+=s;
		}
		JAPCertificate jc=new JAPCertificate();
		jc.decode(new FileInputStream("H:/jap.cer"));

		Signature js=Signature.getInstance("DSA");
		PublicKey ke=jc.getPublicKey();
		MyDSAPublicKey kd=new MyDSAPublicKey(ke.getEncoded());
		//	KeyPairGenerator kg=KeyPairGenerator.getInstance("DSA");
	//	kg.initialize(512);
	//	ke=kg.generateKeyPair().getPublic();
	//	System.out.println(ke.getAlgorithm().toString());


		js.initVerify(kd);
		js.update(japsf,0,sflen);
		byte[] sig=new byte[46];
		System.arraycopy(japdsa,p-46,sig,0,46);
		System.out.println(js.verify(sig));

	}
	catch(Exception e)
	{
		e.printStackTrace();
	}

}

public static void ASN1Test()
{
	try{
//		Parser parser = new Parser(new FileInputStream("h:/cryptix.asn"));
 //   parser.Main(true); // without tracing

    ASNObject x509 = (ASNObject)
       Class.forName("cryptix.x509.Certificate").newInstance();
		FileInputStream fis = new FileInputStream("h:/jap.cer");
    ASNCoder der = BaseCoder.getInstance("DER");
    der.init(fis);
    x509.code(der);

//Obtain the signature's algorithm OID used in the TBSCertificate part of this certificate:

    ASNObject tbsCertificate = x509.getComponent("tbsCertificate");
    ASNObject signature = tbsCertificate.getComponent("signature");
    ASNObject algorithm = signature.getComponent("algorithm");
    String oid = (String) algorithm.getValue();

    System.out.println("Issuer's signature algorithm OID is: "+oid);

	//	algorithm.setValue("1.3.14.3.2.13");
	//	oid = (String) algorithm.getValue();
	//	System.out.println("Issuer's signature algorithm OID is: "+oid);


    ASNModule module = (ASNModule)
        Class.forName("cryptix.x509.PackageProperties").newInstance();
    System.out.println("OID "+oid+" is assigned to \""+
        module.getOID(oid)+"\"...");



    ASNObject version = tbsCertificate.getComponent("version");
		System.out.println(version.getValue());
		version.setValue(new BigInteger("0"));
		//version.setOptional(true);
		FileOutputStream fos = new FileOutputStream("h:/test.cer");
    ASNCoder der1 = BaseCoder.getInstance("DER");
    der1.init(fos);
    x509.code(der1);


	}
	catch(Exception ex)
	{
		ex.printStackTrace();
	}

}*/

	static class MixTestWorkerThreadSender extends Thread
		{
			Socket m_Socket;
			public MixTestWorkerThreadSender(Socket s)
				{
				  m_Socket=s;
				}
			public void run()
				{
					try{
							OutputStream o=m_Socket.getOutputStream();
							byte[] buff=new byte[900];
							for(int i=0;i<buff.length;i++)
								buff[i]='A';
					while(true)
						{
							o.write(buff);
						  Thread.sleep((int)(Math.random()*1000));
						}
						}
						catch(Exception e)
							{
							}
				}
		}

	static class MixTestWorkerThread implements Runnable
		{
			public void run()
				{
					System.out.println("Worker thread started!");
					try{
					while(true)
						{
						  Thread.sleep((int)(Math.random()*1000.0));
						  Socket s=null;
							try{
							s=new Socket("localhost",4001);
							}
							catch(Exception e){
							continue;}
							InputStream in=s.getInputStream();
							byte[] buff=new byte[900];
							new MixTestWorkerThreadSender(s).start();
							int len=0;
							try{
							while(len!=-1)
							{
									int aktLen=0;
									while(aktLen<buff.length&&(len=in.read(buff,aktLen,buff.length-aktLen))>0)
										{
											aktLen+=len;
										}
										if(len!=-1)
										{
									if(aktLen!=buff.length)
										{
											System.out.println("Not all read Error! Read: "+Integer.toString(aktLen));
										}
									else
										for(int i=0;i<buff.length;i++)
											{
												if(buff[i]!='A')
													{
														String st=new String(buff);
														System.out.println("Error!: "+st);
													}
											}
										}
									}
									}
									catch(Exception e)
										{
										}
							try{
							in.close();
							s.close();}
							catch(Exception e){}
						}

						}
						catch(Exception e)
							{
								////e.printStackTrace();
							}
					System.out.println("Worker thread finished!");
				}
		}
	public static void testMixe()
		{
			try{
			Thread[] threads=new Thread[10];
			for(int i=0;i<10;i++)
				{
					threads[i]=new Thread(new MixTestWorkerThread());
					threads[i].start();
				}
			for(int i=0;i<10;i++)
				{
				  threads[i].join();
				}
				}
				catch(Exception e)
				{
				e.printStackTrace();
				}
		}
}

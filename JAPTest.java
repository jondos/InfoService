import java.security.SecureRandom;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import javax.xml.parsers.*;
import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.*;
import java.security.*;
import java.io.*;
//import au.net.aba.security.cert.*;
import java.math.BigInteger;
import sun.security.x509.X509Cert;
import sun.security.pkcs.PKCS8Key;
import sun.security.pkcs.PKCS7;
import sun.security.x509.X509Key;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.BigInt;
//import cryptix.jce.provider.Cryptix;
//import cryptix.jce.provider.CryptixCrypto;
import java.security.Security;
//import java.security.interfaces.RSAPublicKey;
//import javax.crypto.Cipher;
//import cryptix.jce.provider.key.RawSecretKey;
//import java.security.cert.CertificateFactory;
//import org.logi.crypto.keys.BlowfishKey;
//import iaik.security.provider.IAIK;
//import java.security.cert.X509Certificate;
import java.net.*;
final public class JAPTest
{
	public static void main(String argc[])
		{
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
	//	testCert();
//		System.getProperties().list(System.out);
	networkTest(argc);
		System.exit(0);
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
			master.verify(cert.getPublicKey());
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

}

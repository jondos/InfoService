/*
Copyright (c) 2000 - 2003, The JAP-Team
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
package anon.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;

import java.util.Date;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import anon.server.impl.Base64;

import java.security.Signature;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class JAPCertificate
{
	private X509CertificateStructure x509cert;
	private JAPDSAPublicKey pubKey;
	private Time endDate;
	private Time startDate;
	private X509Name issuer;
	private X509Name subject;
	private DERBitString sig;
	private DERInteger serialno;
	private AlgorithmIdentifier sigalgo;
	private SubjectPublicKeyInfo pubkeyinfo;
	private TBSCertificateStructure tbscert;
	private int version;
	private boolean enabled;
	//private byte[] x509certenc;

	private JAPCertificate()
	{
	}

	public static JAPCertificate getInstance(InputStream in) throws IOException
	{
	  try
	  {
			BERInputStream is = new BERInputStream(in);
			ASN1Sequence dcs = (ASN1Sequence) is.readObject();
			X509CertificateStructure m_x509cert = new X509CertificateStructure(dcs);
			JAPCertificate m_japcert = new JAPCertificate(); 		m_japcert.sigalgo = m_x509cert.getSignatureAlgorithm();
			m_japcert.startDate = m_x509cert.getStartDate();
			m_japcert.endDate = m_x509cert.getEndDate();
			m_japcert.issuer = m_x509cert.getIssuer();
			m_japcert.subject = m_x509cert.getSubject();
			m_japcert.sig = m_x509cert.getSignature();
			m_japcert.serialno = m_x509cert.getSerialNumber();
			m_japcert.pubkeyinfo = m_x509cert.getSubjectPublicKeyInfo();
			m_japcert.tbscert = m_x509cert.getTBSCertificate();
			m_japcert.version = m_x509cert.getVersion();
			m_japcert.pubKey = new JAPDSAPublicKey(m_x509cert.getSubjectPublicKeyInfo());
			m_japcert.x509cert = m_x509cert;
//			m_japcert.x509certenc = encoded;
			return m_japcert;
		}
		// todo: fehlerbehandlung, falls werte nicht belegt sind oder werden können
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	} 

/*
	public static JAPCertificate getInstance(TBSCertificateStructure m_tbscert)
	{
		JAPCertificate m_japcert = new JAPCertificate();
		try
		{
			m_japcert.startDate = m_tbscert.getStartDate();
			m_japcert.endDate = m_tbscert.getEndDate();
			m_japcert.issuer = m_tbscert.getIssuer();
			m_japcert.subject = m_tbscert.getSubject();
			m_japcert.sigalgo = m_tbscert.getSignature();
			m_japcert.pubkeyinfo = m_tbscert.getSubjectPublicKeyInfo();
			m_japcert.serialno = m_tbscert.getSerialNumber();
			m_japcert.version = m_tbscert.getVersion();
			m_japcert.pubKey = new JAPDSAPublicKey(m_tbscert.getSubjectPublicKeyInfo());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return m_japcert;
	}
*/

	public static JAPCertificate getInstance(Node root) throws IOException
	{
//      System.out.println("found:  " + root.toString());
			if (!root.getNodeName().equals("X509Certificate"))
			{
				System.out.println("ist null!!!!");
				return null;
			}

			Element elemX509Cert = (Element) root;
			Text txtX509Cert = (Text) elemX509Cert.getFirstChild();
			String value = txtX509Cert.getNodeValue();
			byte[] bytecert = Base64.decode(value.toCharArray());
			
			try
			{
						return getInstance(bytecert);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return null;
	}

	public static JAPCertificate getInstance(File file)
	{
			if (file != null)
			{
					try
					{
							byte[] buff = new byte[(int) file.length()];
							FileInputStream fin = new FileInputStream(file);
							fin.read(buff);
							fin.close();
							return JAPCertificate.getInstance(buff);
					}
					catch (FileNotFoundException e)
					{
						new FileNotFoundException("Certificate file not found!");
					}
					catch (Exception e)
					{
						new Exception("Error while processing certificate !");
					}
			}
			return null;
		}


		public static JAPCertificate getInstance(byte[] encoded)
		{
			try
				{
					return getInstance(new ByteArrayInputStream(encoded));
				}
			catch(Exception e)
				{
					return null;
				}
		}


	public static class IllegalCertificateException extends RuntimeException
	{
		public IllegalCertificateException(String str) {super(str);}
	};

	public Date getStartDate()
	{
				return startDate.getDate();
	}

	public Date getEndDate()
	{
					return endDate.getDate();
	}

	public TBSCertificateStructure getTBSCertificate()
	{
		return tbscert;
	}

	public DERInteger getSerialNumber()
	{
		return serialno;
	}

	public DERBitString getSignature()
	{
		return sig;
	}

	public AlgorithmIdentifier getSignatureAlgorithm()
	{
		return sigalgo;
	}

	public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
	{
		return pubkeyinfo;
	}

	public X509Name getIssuer()
	{
		return issuer;
	}

	public X509Name getSubject()
	{
		return subject;
	}

	public int getVersion()
	{
		return version;
	}

	public PublicKey getPublicKey()
	{
		return (PublicKey) pubKey;
	}

	public char[] getEncoded()
{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		try
		{
			dOut.writeObject(this.x509cert);
			dOut.close();
		}
		catch (IOException e)
		{
			// throw new RuntimeException("IOException while encoding");
			e.printStackTrace();
		}
		return Base64.encode(bOut.toByteArray());
}

	public boolean validDate(Date date)
	{
		if (date.before(getStartDate()))
			return false;
		if (date.after(getEndDate()))
			return false;
		return true;
	}

	public void setEnabled(boolean b)
	{
		enabled = b; 
	}

	public boolean getEnabled()
	{	
		return enabled;
	}

	public boolean verify(PublicKey pubkey) throws NoSuchAlgorithmException, InvalidKeyException,SignatureException, JAPCertificateException
	{
		try
		{
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();

			JAPSignature sig = new JAPSignature();
			sig.initVerify(pubkey);

			(new DEROutputStream(bOut)).writeObject(this.getTBSCertificate());

			byte[] sigToVerify = this.getSignature().getBytes();
			return sig.verify(bOut.toByteArray(),sigToVerify);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}


  /**
   * Creates XML node of certificate
   *
   * @param doc The XML document, which is the environment for the created XML node.
   *
   * @return Certificate as XML node.
   */

  public Element toXmlNode(Document doc)
 {

		Element keyInfo = doc.createElement("KeyInfo");
		Element x509data = doc.createElement("X509Data");
		keyInfo.appendChild(x509data);
		Element x509cert = doc.createElement("X509Certificate");
		x509data.appendChild(x509cert);
		// x509cert.setAttribute("xml:space", "preserve");
		// falsch! x509cert.appendChild(doc.createTextNode(String.valueOf(getEncoded())));
		x509cert.appendChild(doc.createTextNode(String.valueOf(getEncoded())));
			
		return keyInfo;
  }

}
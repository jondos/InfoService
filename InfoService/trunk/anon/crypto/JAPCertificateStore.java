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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JAPCertificateStore
{
	private Hashtable certstore = null;
	private static JAPCertificateStore jcs = null;

	private JAPCertificateStore()
	{
		certstore = new Hashtable();
	}

	public static JAPCertificateStore create()
	{
		if (jcs == null)
		{
			jcs = new JAPCertificateStore();
		}
		return jcs;
	}

	public static JAPCertificateStore getStore()
	{
		return jcs;
	}

	public JAPCertificateStore(String certFileName)
	{
		this();
		System.out.println("got file:  " + certFileName);
		InputStream in = null;
		try
		{
			// this is necessary to make shure that the cert  is loaded when contained in a JAP.jar
			in = Class.forName("JAP").getResourceAsStream(certFileName);
		}
		catch (Exception e)
		{
			try
			{
				//we have to chek, if it exists as file
				in = new FileInputStream(certFileName);
			}
			catch (Exception e1)
			{
			}
		}
		try
		{
			addCertificate(JAPCertificate.getInstance(in));
		}
		catch (Exception e2)
		{
			// just for coding
			// if something
			File file = new File("../certificates/japroot.cer");
			try
			{
				addCertificate(JAPCertificate.getInstance(file));
			}
			catch (Exception e3)
			{
				e3.printStackTrace();
			}
		}
		try
		{
			in.close();
		}
		catch (Throwable t2)
		{}
		;

	}

	public JAPCertificateStore(NodeList x509certs)
	{
		this();
		int length = x509certs.getLength();

		Node certNode = null;

		for (int i = 0; i < length; i++)
		{
			certNode = (Node) x509certs.item(i);
			try
			{
				JAPCertificate j = JAPCertificate.getInstance(certNode);
				this.addCertificate(j);
			}
			catch (IOException ex_io)
			{
				System.out.println("init(NodeList) : I/O : ");
				ex_io.printStackTrace();

			}
			catch (NullPointerException ex_np)
			{
				System.out.println("init(NodeList) : Ex : ");
				ex_np.printStackTrace();
			}
		}
		// System.out.println("size: " + size());
	}

	public synchronized void addCertificate(JAPCertificate cert)
	{
		cert.setEnabled(true);

		try
		{
			certstore.put(JAPCertificateStoreId.getId(cert), cert);
		}
		catch (NullPointerException ex_np)
		{
			// ignore NPE, since it is broken for put()
		}

	}

	public void disableCertificate(JAPCertificate cert)
	{
		cert.setEnabled(false);
		certstore.put(JAPCertificateStoreId.getId(cert), cert);
	}

	public void enableCertificate(JAPCertificate cert)
	{
		cert.setEnabled(true);
		certstore.put(JAPCertificateStoreId.getId(cert), cert);
	}

	public JAPCertificate removeCertificate(JAPCertificate cert)
	{
		return (JAPCertificate) certstore.remove(JAPCertificateStoreId.getId(cert));
	}

	public boolean checkCertificateExists(JAPCertificate cert)
	{
		return certstore.containsKey(JAPCertificateStoreId.getId(cert));
	}

	public int size()
	{
		return certstore.size();
	}

	public Enumeration keys()
	{
		return certstore.keys();
	}

	public Enumeration elements()
	{
		return certstore.elements();
	}

	public void dumpKeys()
	{
		Enumeration k = keys();
		while (k.hasMoreElements())
		{
			System.out.println(k.nextElement());
		}
	}

	/**
	 * Creates the trusted CA XML node
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The trusted CAs XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		Element caNode = doc.createElement("CertificateAuthorities");

		Enumeration enum = elements();

		while (enum.hasMoreElements())
		{
			Element caChildNode = doc.createElement("CertificateAuthority");
			caNode.appendChild(caChildNode);
			Element caEnabled = doc.createElement("Enabled");
			caChildNode.appendChild(caEnabled);
			JAPCertificate cert = (JAPCertificate) enum.nextElement();
			boolean enabled = cert.getEnabled();
			caEnabled.appendChild(enabled ? doc.createTextNode("true") : doc.createTextNode("false"));

			caChildNode.appendChild(cert.toXmlNode(doc));
		}

		return caNode;
	}

}

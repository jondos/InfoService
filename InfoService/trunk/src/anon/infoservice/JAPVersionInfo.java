/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.infoservice;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.crypto.JAPCertificate;
import anon.crypto.XMLSignature;

public class JAPVersionInfo
{

	/**
	 * Describes a JAP release version.
	 */
	public final static int JAP_RELEASE_VERSION = 1;

	/**
	 * Describes a JAP development version.
	 */
	public final static int JAP_DEVELOPMENT_VERSION = 2;

	private int versionInfoType;
	private String m_Version;
	private Date m_Date = null;
	private String m_JAPJarFileName;
	private URL m_CodeBase;
	private String m_Description = "";

	/**
	 * Creates a new JAP version info out of a JNLP file (is an XML document) and the type of the
	 * JAP versioon.
	 *
	 * @param doc The JNLP XML document.
	 * @param type The type of the JAPVersionInfo (release / development). Look at the constants in
	 *             this class.
	 */
	public JAPVersionInfo(Document doc, int type) throws Exception
	{
		versionInfoType = type;
		Element root = doc.getDocumentElement();
		//signature check...
		JAPCertificate cert = InfoServiceHolder.getInstance().getCertificateForUpdateMessages();
		if (cert != null)
		{
			try
			{
				if (XMLSignature.verify(root,cert.getPublicKey())==null)
				{
					throw (new Exception("InfoService: new JAPVersionInfo: Signature check failed!"));
				}

			}
			catch (Exception e)
			{
				throw (new Exception("InfoService: new JAPVersionInfo: Signature check failed!"));
			}
		}

		m_Version = root.getAttribute("version"); //the JAP version
		try
		{
			String strDate = root.getAttribute("releaseDate") + " GMT";
			m_Date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").parse(strDate);
		}
		catch (Exception ed)
		{
			m_Date = null;
		}
		m_CodeBase = new URL(root.getAttribute("codebase"));
		NodeList nlResources = root.getElementsByTagName("resources");
		NodeList nlJars = ( (Element) nlResources.item(0)).getElementsByTagName("jar");
		for (int i = 0; i < nlJars.getLength(); i++)
		{
			try
			{
				Element elemJar = (Element) nlJars.item(i);
				String part = elemJar.getAttribute("part");
				if (part.equals("jap"))
				{
					m_JAPJarFileName = elemJar.getAttribute("href");
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	public String getVersionNumber()
	{
		return m_Version;
	}

	public Date getDate()
	{
		return m_Date;
	}

	public String getDescription()
	{
		return m_Description;
	}

	public URL getCodeBase()
	{
		return m_CodeBase;
	}

	public String getJAPJarFileName()
	{
		return m_JAPJarFileName;
	}

}

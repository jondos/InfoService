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

import java.util.Hashtable;
import org.bouncycastle.asn1.DERInteger;
import java.util.Enumeration;


public class JAPCertificateStore
	{
		private Hashtable certstore;

		public JAPCertificateStore()
			{
				certstore = new Hashtable();
			}

		public synchronized boolean addCertificate(JAPCertificate cert)
			{
				String issuerCN = (String) cert.getIssuer().getValues().elementAt(0);
				certstore.put(issuerCN, cert);
				if (certstore.containsKey(issuerCN))
					return true;
				else
					return false;
			}

		public void removeCertificate(JAPCertificate cert)
			{
				String issuerCN = (String) cert.getIssuer().getValues().elementAt(0);
				certstore.remove(issuerCN);
			}

		public JAPCertificate getCertificate(String issuerCN)
			{
				return (JAPCertificate) certstore.get(issuerCN);
			}

		public JAPCertificate removeCertificate(String issuerCN)
			{
				return (JAPCertificate) certstore.remove(issuerCN);
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

		public String dumpStoreData()
			{
				String m_dump = "";
				Enumeration enum = certstore.keys();
				while (enum.hasMoreElements())
					{
						m_dump = m_dump + enum.nextElement();
						if (enum.hasMoreElements())
							m_dump = m_dump + ",";
					}
				return m_dump;
			}

}
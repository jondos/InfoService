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
import java.security.Certificate;
import java.security.Principal;
import java.security.PublicKey;
import java.io.OutputStream;
import java.io.InputStream;
import sun.security.x509.X509Cert;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
import java.util.StringTokenizer;
final class JAPCertificate implements Certificate
	{
		private X509Cert cert;
		
		public final static int BASE64=1;
		public final static int DER=2;
		public final static int DEFAULT_ENCODING=DER;

		final class BASE64InputFilter extends InputStream
			{
				byte[] buff;
				int aktIndex;
					public BASE64InputFilter(String s)
						{
							buff=s.getBytes();
							aktIndex=0;
						}
					public int read()
					{
						while(aktIndex<buff.length&&(buff[aktIndex]==32||buff[aktIndex]==9))
							aktIndex++;
						if(aktIndex==buff.length)
							return -1;
						else
							return buff[aktIndex++];
					}
				}

		
		public JAPCertificate(byte[] certBuff)
			{
				try
					{
						cert=new X509Cert(certBuff);
					}
				catch(Exception e)
					{
						cert=null;
					}
			}
		
		public JAPCertificate()
			{
				cert=null;
			}
		
		public Principal getPrincipal()
			{
				return cert.getPrincipal();
			}

		public PublicKey getPublicKey()
			{
				return cert.getPublicKey();
			}
		
		public void encode(OutputStream out)
			{
				try
					{
						encode(out,DEFAULT_ENCODING);
					}
				catch(Exception e)
					{
					}
			}

		public void encode(OutputStream out,int type)
			{
				switch(type)
					{
						case BASE64:
							try
								{
									PrintWriter pw=new PrintWriter(out);
									pw.println("-----BEGIN CERTIFICATE-----");
									pw.flush();
									ByteArrayOutputStream bo=new ByteArrayOutputStream();
									BASE64Encoder b=new BASE64Encoder();
									cert.encode(bo);
									bo.flush();
									b.encodeBuffer(bo.toByteArray(),out);
									pw.println("-----END CERTIFICATE-----");
									pw.flush();
									pw.close();
								}
							catch(Exception e)
								{
								}
						break;	
						case DER:
							try
								{
									cert.encode(out);
								}
							catch(Exception e)
								{
									return;
								}
						break;
						default:
							return;
					}
			}

		public void decode(InputStream in)
			{
				try
					{
						cert=new X509Cert();
						cert.decode(in);
					}
				catch(Exception e)
					{
						e.printStackTrace();
					}
			}
		
		public void decode(byte[] buff,int type)
			{
				switch(type)
					{
						case BASE64:
							try
								{
									BASE64Decoder d=new BASE64Decoder();
									String s=new String(buff);
									s=s.trim();
									if(!s.startsWith("-----BEGIN CERTIFICATE-----")
										 ||!s.endsWith("-----END CERTIFICATE-----"))
										return;
									s=s.substring(27,s.length()-26);
									BASE64InputFilter in=new BASE64InputFilter(s);
									byte[] decoded=d.decodeBuffer(in);
									decode(decoded,DER);
								}
							catch(Exception e)
								{
									e.printStackTrace();
								}
						break;
						case DER:
							try
								{
									cert=new X509Cert(buff);
								}
							catch(Exception e)
								{
									cert=null;
									e.printStackTrace();
								}
						break;
						default:
							return;
					}
			}
		
		public String getFormat()
			{
				return cert.getFormat();
			}
		
		public String toString(boolean b)
			{
				return cert.toString(b);
			}
		
		public Principal getGuarantor()
			{
				return cert.getGuarantor();
			}
		
		public boolean verify(PublicKey key)
			{
				try
					{
						cert.verify(key);
						return true;
					}
				catch(Exception e)
					{
						e.printStackTrace();
						return false;
					}
			}
}

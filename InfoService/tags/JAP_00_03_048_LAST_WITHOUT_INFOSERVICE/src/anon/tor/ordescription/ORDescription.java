/*
 Copyright (c) 2004, The JAP-Team
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
package anon.tor.ordescription;

import java.io.LineNumberReader;
import java.util.StringTokenizer;

import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;

public class ORDescription
{

	private String m_address;
	private String m_name;
	private int m_port;
	private int m_portDir;
	private String m_strSoftware;
	private ORAcl m_acl;
	private MyRSAPublicKey m_onionkey;
	private MyRSAPublicKey m_signingkey;

	/**
	 * Constructor
	 * @param address
	 * address of the onion router
	 * @param name
	 * name for the onion router
	 * @param port
	 * port
	 * @param strSoftware
	 * version of the onion router software
	 */
	public ORDescription(String address, String name, int port, String strSoftware)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		m_portDir = -1;
		m_strSoftware = strSoftware;
		m_acl = new ORAcl();
	}

	/**
	 * sets the ACL for this onion router
	 * @param acl
	 * ACL
	 */
	public void setAcl(ORAcl acl)
	{
		m_acl = acl;
	}

	/**
	 * gets the ACL for this onion router
	 * @return
	 * ACL
	 */
	public ORAcl getAcl()
	{
		return m_acl;
	}

	/**
	 * sets the onionkey for this OR
	 * @param onionkey
	 * onionkey
	 * @return
	 * true if the key is a rsa key
	 */
	public boolean setOnionKey(byte[] onionkey)
	{
		m_onionkey = MyRSAPublicKey.getInstance(onionkey);
		return m_onionkey != null;
	}

	/**
	 * gets the onionkey
	 * @return
	 * onionkey
	 */
	public MyRSAPublicKey getOnionKey()
	{
		return this.m_onionkey;
	}

	/**
	 * sets the signing key
	 * @param signingkey
	 * signing key
	 * @return
	 * true if the key is a rsa key
	 */
	public boolean setSigningKey(byte[] signingkey)
	{
		m_signingkey = MyRSAPublicKey.getInstance(signingkey);
		return m_signingkey != null;
	}

	/**
	 * gets the signing key
	 * @return
	 * signing key
	 */
	public MyRSAPublicKey getSigningKey()
	{
		return this.m_signingkey;
	}

	/**
	 * gets the address of the OR
	 * @return
	 * address
	 */
	public String getAddress()
	{
		return this.m_address;
	}

	/**
	 * gets the name of the OR
	 * @return
	 * name
	 */
	public String getName()
	{
		return this.m_name;
	}

	/**
	 * sets the port of the directory server
	 * @param port
	 * port
	 */
	public void setDirPort(int port)
	{
		m_portDir = port;
	}

	/**
	 * gets the port
	 * @return
	 * port
	 */
	public int getPort()
	{
		return this.m_port;
	}

	/**
	 * gets the port of the directory server
	 * @return
	 * port
	 */
	public int getDirPort()
	{
		return m_portDir;
	}

	/**
	 * gets the software version of this OR
	 * @return
	 * software version
	 */
	public String getSoftware()
	{
		return m_strSoftware;
	}

	/**
	 * test if two OR's are identical
	 * @param or
	 * OR
	 * @return
	 */
	public boolean equals(ORDescription or)
	{
		if (or == null)
		{
			return false;
		}
		return m_address.equals(or.getAddress()) &&
			m_name.equals(or.getName()) &&
			(m_port == or.getPort());
	}

	/**Tries to parse an router specification according to the desing document.
	 * @param reader
	 * reader
	 */
	public static ORDescription parse(LineNumberReader reader)
	{
		try
		{
			String ln = reader.readLine();
			if (ln == null || !ln.startsWith("router"))
			{
				return null;
			}
			StringTokenizer st = new StringTokenizer(ln);
			st.nextToken(); //skip router
			String nickname = st.nextToken();
			String adr = st.nextToken();
			String orport = st.nextToken();
			String socksport = st.nextToken();
			String dirport = st.nextToken();
			byte[] key = null;
			byte[] signingkey = null;
			ORAcl acl = new ORAcl();
			String strSoftware = "";
			for (; ; )
			{
				ln = reader.readLine();
				if (ln == null)
				{
					return null;
				}
				if (ln.startsWith("platform"))
				{
					st = new StringTokenizer(ln);
					st.nextToken();
					strSoftware = st.nextToken() + " " + st.nextToken();
				}
				else if (ln.startsWith("onion-key"))
				{
					StringBuffer buff = new StringBuffer();
					ln = reader.readLine(); //skip -----begin
					for (; ; )
					{
						ln = reader.readLine();
						if (ln.startsWith("-----END"))
						{
							key = Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if (ln.startsWith("signing-key"))
				{
					StringBuffer buff = new StringBuffer();
					ln = reader.readLine(); //skip -----begin
					for (; ; )
					{
						ln = reader.readLine();
						if (ln.startsWith("-----END"))
						{
							signingkey = Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if (ln.startsWith("router-signature"))
				{
					for (; ; )
					{
						ln = reader.readLine(); // skip siganture
						if (ln.startsWith("-----END"))
						{
							ORDescription ord = new ORDescription(adr, nickname, Integer.parseInt(orport),
								strSoftware);
							if (!ord.setOnionKey(key) || !ord.setSigningKey(signingkey))
							{
								return null;
							}
							ord.setAcl(acl);
							try
							{
								ord.setDirPort(Integer.parseInt(dirport));
							}
							catch (Exception e)
							{
							}
							return ord;
						}
					}

				}
				else if (ln.startsWith("accept") || ln.startsWith("reject"))
				{
					acl.add(ln);

				}

			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}

	public String toString()
	{
		return "ORRouter: " + this.m_name + " on " + this.m_address + ":" + this.m_port;
	}

}
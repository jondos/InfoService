package anon.tor.ordescription;

//import java.util.HashSet;

import anon.util.Base64;
import java.io.*;
import java.util.StringTokenizer;
import anon.crypto.MyRSAPublicKey;

/*
 * Created on Mar 25, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ORDescription
{

	private String m_address;
	private String m_name;
	private int m_port;
	private String m_strSoftware;
	private ORAcl m_acl;
	private MyRSAPublicKey m_onionkey;
	private MyRSAPublicKey m_signingkey;

	public ORDescription(String address, String name, int port, String strSoftware)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		m_strSoftware = strSoftware;
		m_acl = new ORAcl();
	}

	public void setAcl(ORAcl acl)
	{
		m_acl = acl;
	}

	public ORAcl getAcl()
	{
		return m_acl;
	}

	public boolean setOnionKey(byte[] onionkey)
	{
		m_onionkey = MyRSAPublicKey.getInstance(onionkey);
		return m_onionkey != null;
	}

	public MyRSAPublicKey getOnionKey()
	{
		return this.m_onionkey;
	}

	public boolean setSigningKey(byte[] signingkey)
	{
		m_signingkey = MyRSAPublicKey.getInstance(signingkey);
		return m_signingkey != null;
	}

	public MyRSAPublicKey getSigningKey()
	{
		return this.m_signingkey;
	}

	public String getAddress()
	{
		return this.m_address;
	}

	public String getName()
	{
		return this.m_name;
	}

	public int getPort()
	{
		return this.m_port;
	}

	public String getSoftware()
	{
		return m_strSoftware;
	}

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

	/*Tries to parse an router specification according to the desing document.*/
	public static ORDescription parse(LineNumberReader reader) throws Exception
	{
		String ln = reader.readLine();
		if (!ln.startsWith("router"))
		{
			throw new Exception("Wrong router format - does not start with 'router'");
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
		ln = reader.readLine();
		if (ln.startsWith("platform"))
		{
			st = new StringTokenizer(ln);
			st.nextToken();
			strSoftware = st.nextToken() + " " + st.nextToken();
		}
		for (; ; )
		{
			ln = reader.readLine();
			if (ln.startsWith("onion-key"))
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

	public String toString()
	{
		return "ORRouter: " + this.m_name + " on " + this.m_address + ":" + this.m_port;
	}

}

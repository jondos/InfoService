package anon.tor.ordescription;
//import java.util.HashSet;

import anon.util.Base64;
import java.io.*;
import java.util.StringTokenizer;
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
public class ORDescription {

	private String m_address;
	private String m_name;
	private int m_port;
	private String m_strSoftware;
	private ORAcl m_acl;
	private byte[] m_onionkey;
	private byte[] m_signingkey;

	public ORDescription(String address, String name, int port,String strSoftware)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		m_strSoftware=strSoftware;
		m_acl=new ORAcl();
	}

	public void setAcl(ORAcl acl)
	{
		m_acl=acl;
	}
	public ORAcl getAcl()
	{
		return m_acl;
	}
	public void setOnionKey(byte[] onionkey)
	{
		this.m_onionkey = onionkey;
	}

	public byte[] getOnionKey()
	{
		return this.m_onionkey;
	}

	public void setSigningKey(byte[] signingkey)
	{
		this.m_signingkey = signingkey;
	}

	public byte[] getSigningKey()
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
		if(this.m_address.equals(or.getAddress()))
		{
			if(this.m_name.equals(or.getName()))
			{
				if(this.m_port==or.getPort())
				{
					return true;
				}
			}
		}
		return false;
	}
	/*Tries to parse an router specification according to the desing document.*/
	public static ORDescription parse(LineNumberReader reader) throws Exception
	{
			String ln=reader.readLine();
			if(!ln.startsWith("router"))
				throw new Exception("Wrong router format - does not start with 'router'");
			StringTokenizer st=new StringTokenizer(ln);
			st.nextToken(); //skip router
			String nickname=st.nextToken();
			String adr=st.nextToken();
			String orport=st.nextToken();
			String socksport=st.nextToken();
			String dirport=st.nextToken();
			byte[] key=null;
			byte[] signingkey=null;
			ORAcl acl=new ORAcl();
			String strSoftware="";
			ln=reader.readLine();
			if(ln.startsWith("platform"))
			{
				st=new StringTokenizer(ln);
				st.nextToken();
				strSoftware=st.nextToken()+" "+st.nextToken();
			}
			for(;;)
			{
				ln=reader.readLine();
				if(ln.startsWith("onion-key"))
				{
					StringBuffer buff=new StringBuffer();
					ln=reader.readLine(); //skip -----begin
					for(;;)
					{
						ln = reader.readLine();
						if (ln.startsWith("-----END"))
						{
							key=Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if(ln.startsWith("signing-key"))
				{
					StringBuffer buff=new StringBuffer();
					ln=reader.readLine(); //skip -----begin
					for(;;)
					{
						ln = reader.readLine();
						if (ln.startsWith("-----END"))
						{
							signingkey=Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if(ln.startsWith("router-signature"))
				{
					for(;;)
					{
						ln = reader.readLine(); // skip siganture
						if (ln.startsWith("-----END"))
						{
							ORDescription ord=new ORDescription(adr,nickname,Integer.parseInt(orport),strSoftware);
							ord.setOnionKey(key);
							ord.setSigningKey(signingkey);
							ord.setAcl(acl);
							return ord;
						}
					}

				}
				else if(ln.startsWith("accept")||ln.startsWith("reject"))
					acl.add(ln);


		}
	}

	public String toString()
	{
		return "ORRouter: "+this.m_name+" on "+this.m_address+":"+this.m_port;
	}

}

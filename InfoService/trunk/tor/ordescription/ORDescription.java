package tor.ordescription;
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

	private String address;
	private String name;
	private int port;
	private boolean running;
	private ORAcl m_acl;
	private byte[] onionkey;

	public ORDescription(String address, String name, int port, boolean running)
	{
		this.address = address;
		this.name = name;
		this.port = port;
		this.running = running;
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
		this.onionkey = onionkey;
	}

	public byte[] getOnionKey()
	{
		return this.onionkey;
	}

	public String getAddress()
	{
		return this.address;
	}

	public String getName()
	{
		return this.name;
	}

	public int getPort()
	{
		return this.port;
	}

	public boolean isRunnung()
	{
		return this.running;
	}

	public boolean equals(ORDescription or)
	{
		if(this.address.equals(or.getAddress()))
		{
			if(this.name.equals(or.getName()))
			{
				if(this.port==or.getPort())
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
			ORAcl acl=new ORAcl();
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
				else if(ln.startsWith("router-signature"))
				{
					for(;;)
					{
						ln = reader.readLine(); // skip siganture
						if (ln.startsWith("-----END"))
						{
							ORDescription ord=new ORDescription(adr,nickname,Integer.parseInt(orport),true);
							ord.setOnionKey(key);
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
		return "ORRouter: "+name+" on "+this.address+":"+this.port;
	}

}

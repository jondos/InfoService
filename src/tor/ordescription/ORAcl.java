package tor.ordescription;

import java.util.*;
import java.net.*;
public class ORAcl
{
	private Vector m_Constraints;
	private class AclElement
	{
		byte[] arAdrWithMask;
		byte[] arAdrMask;
		int portLow;
		int portHigh;
		boolean bIsAccept;
		public AclElement(boolean baccept,String a,String m,int l,int h) throws Exception
		{
			InetAddress ia=InetAddress.getByName(a);
			arAdrWithMask=ia.getAddress();
			ia=InetAddress.getByName(m);
			arAdrMask=ia.getAddress();
			for(int i=0;i<4;i++)
				arAdrWithMask[i]&=arAdrMask[i];
			portLow=l;
			portHigh=h;
			bIsAccept=baccept;
		}

		public boolean isContained(String adr,int port) throws Exception
		{
			if(port<portLow||port>portHigh)
				return false;
			InetAddress ia=InetAddress.getByName(adr);
			byte[] arIP=ia.getAddress();
			for(int i=0;i<4;i++)
				if((arIP[i]&arAdrMask[i])!=this.arAdrWithMask[i])
					return false;

			return true;
		}

		public boolean isAccept()
		{
			return bIsAccept;
		}

	}
    public ORAcl()
    {
		m_Constraints=new Vector();
    }

	public void add(String acl) throws Exception
	{
		StringTokenizer st=new StringTokenizer(acl);
		String s=st.nextToken();
		boolean bAccept=false;
		if(s.equals("accept"))
			bAccept=true;
		s=st.nextToken();
		st=new StringTokenizer(s,":");
		String a=st.nextToken();
		String ports=st.nextToken();
		int l=0xFFFF;
		int h=0;
		if(ports.equals("*"))
		{
			l=0;
			h=0xFFFF;
		}
		else
		{
			st=new StringTokenizer(ports,"-");
			l=Integer.parseInt(st.nextToken());
			if(st.hasMoreTokens())
				h=Integer.parseInt(st.nextToken());
			else
				h=l;
		}
		String adr=null;
		String mask=null;
		if(a.equals("*"))
		{
			adr="0.0.0.0";
			mask="0.0.0.0";
		}
		else
		{
		st=new StringTokenizer(a,"/");
		adr=st.nextToken();
		if(st.hasMoreElements())
		mask=st.nextToken();
		else
			mask="255.255.255.255";
		}
		m_Constraints.addElement(new AclElement(bAccept,adr,mask,l,h));
	}

	public boolean isAllowed(String adr,int port)
	{
		try{
		for(int i=0;i<m_Constraints.size();i++)
		{
			AclElement acl=(AclElement)m_Constraints.elementAt(i);
			if(acl.isContained(adr,port))
				return acl.isAccept();
		}}
		catch(Exception e)
		{
		}
		return false;
	}
}

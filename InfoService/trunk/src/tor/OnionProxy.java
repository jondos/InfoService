/*
 * Created on May 5, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.*;
//import java.util.HashMap;

import tor.tinytls.TinyTLS;
import tor.cells.Cell;
import tor.cells.CreatedCell;
import tor.cells.DestroyCell;
import tor.cells.RelayCell;
import tor.util.helper;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class OnionProxy {

	private static OnionProxy op = null;
	private Hashtable connections;
	private Hashtable cells;

	private OnionProxy()
	{
		this.connections = new Hashtable();
		this.cells = new Hashtable();
	}

	public static OnionProxy getInstance()
	{
		if(op==null)
		{
			op = new OnionProxy();
		}
		return op;
	}

	public void send(InetAddress addr,int port,Cell cell) throws Exception
	{
		TinyTLS tlssocket = getTLSSocket(addr,port);
		tlssocket.getOutputStream().write(cell.getCellData());
	}

	public Cell read(InetAddress addr,int port,int circid) throws Exception
	{
		//vielleicht f?llt mir hier ja noch was eleganteres ein
		String s = addr.getHostAddress()+":"+port+":"+circid;

		InputStream is= getTLSSocket(addr,port).getInputStream();
		Vector l;
		Cell cell = null;;

		while(!this.cells.containsKey(s))
		{
			while(is.available()<512);
			byte[] b = new byte[512];
			is.read(b);
			int cid = ((b[0] & 0xFF)<<8) | (b[1] & 0xFF);
			int type = b[2] & 0xFF;
			System.out.println("zelltyp : "+type);
			switch(type)
			{
				case 2 :
				{
					cell = new CreatedCell(cid,helper.copybytes(b,3,509));
					break;
				}
				case 3 :
				{
					cell = new RelayCell(cid,helper.copybytes(b,3,509));
					break;
				}
				case 4 :
				{
					cell = new DestroyCell(cid,helper.copybytes(b,3,509));
					break;
				}
				default :
				{
					System.out.println("unbekannter zelltyp");
				}
			}
			String s2 = addr.getHostAddress()+":"+port+":"+cid;
			if(this.cells.containsKey(s2))
			{
				l = (Vector)this.cells.get(s2);
			} else
			{
				l = new Vector();
			}
			l.addElement(cell);
			this.cells.put(s2,l);
		}

		l = (Vector)this.cells.get(s);
		cell = (Cell)l.elementAt(0);
		l.removeElementAt(0);
		if(l.isEmpty())
		{
			this.cells.remove(s);
		} else
		{
			this.cells.put(s,l);
		}

		return cell;
	}

	private TinyTLS getTLSSocket(InetAddress addr,int port) throws Exception
	{
		TinyTLS tlssocket;
		if(connections.containsKey(addr))
		{
			Object o = connections.get(addr);
			if(o instanceof TinyTLS)
			{
				tlssocket = (TinyTLS)o;
				return tlssocket;
			} else
			{
				throw new Exception("cannot cast to TinyTLS");
			}
		} else
		{
		tlssocket = new TinyTLS(addr.getHostAddress(),port);
		tlssocket.startHandshake();
		connections.put(addr,tlssocket);
		return tlssocket;
		}

	}

}

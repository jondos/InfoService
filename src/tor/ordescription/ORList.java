package tor.ordescription;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
//import java.util.ArrayList;

import anon.util.Base64;

import HTTPClient.*;
import java.io.*;
import java.util.Vector;
import logging.*;
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
public class ORList {

	private Vector onionrouters;

	public ORList()
	{
		this.onionrouters = new Vector();
		ORDescription ord = new ORDescription("141.76.46.90","jap",9001,true);
		ord.setOnionKey(Base64.decode("MIGJAoGBANkRpoPUldepI8wlUeZYs5p4IoCyKvsO4O0jwOI8P059ej+FNIkiQUzlDT9W01DPpThSj06lE52AunrHfnfrQ7gLq2WGmVYnzGgWTSsVNVaCi6LDTA86gimdFMkdUSw4JHak2raQGOkbe2yF5RBEsh5pMdwFb1TGQP7AGFzGA4ifAgMBAAE="));
		this.onionrouters.addElement(ord);
		ord = new ORDescription("18.244.0.188","moria1",9001,true);
		ord.setOnionKey(Base64.decode("MIGJAoGBALu5eVTpIJhnKWjRyP1kpKy8jaGQLS9K/KddKw0MJHnp1F9gr/dDEtgJoYkR5WqLZlhisIAOUidowC3T7tZTshqMaDTnC8gLno+Jcd8Il8wJxf/Pkjy9J7B+bsVLjRgHSoDHxhslYH2wPHvL6CQAVoLi3j8L1Gzl3ZXYZnEsLfdfAgMBAAE="));
		this.onionrouters.addElement(ord);
		ord = new ORDescription("80.190.251.24 ","ned",9001,true);
		ord.setOnionKey(Base64.decode("MIGJAoGBAL3w7Uk/pRTyPHIopXRPGjQfKE+tMspppHvBlurAppGnTVIfmjOIuatjUV1gfLG3XAwJdBZfWgUTbazk1EDDUg++A+IVuiT+d0XgHLTAIzpPmyBX2gGv+97hsObfbXHtFbUhVvtfgRHrUIbTKs+vOry9w5XL+NWgP5IOZ1R4E39HAgMBAAE="));
		this.onionrouters.addElement(ord);
   	}

	public void updateList(String server, int port) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[UPDATE OR-LIST] Starting update on "+server+":"+port);
		HTTPConnection http=new HTTPConnection(server,port);
		HTTPResponse resp=http.Get("/");
		if( resp.getStatusCode()!=200 )
		{
			throw new IOException("Cannot recieve OR-List");
		}
		String doc=resp.getText();
		System.out.println(doc);
		parseDocument(doc);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[UPDATE OR-LIST] Update finished");
	}

	public Vector getList()
	{
		return this.onionrouters;
	}

	private void parseDocument(String strDocument) throws Exception
	{
		Vector ors=new Vector();
		LineNumberReader reader=new LineNumberReader(new StringReader(strDocument));
		for(;;)
		{
			reader.mark(200);
			String aktLine=reader.readLine();
			if(aktLine==null)
				break;
			if(aktLine.startsWith("router"))
			{
				reader.reset();
				ORDescription ord=ORDescription.parse(reader);
				if(ord!=null)
				{
					ors.addElement(ord);
					System.out.println("Added: "+ord);
				}
			}
		}
		onionrouters=ors;
	}
}



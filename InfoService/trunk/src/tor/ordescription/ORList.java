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
		ord.setOnionKey(Base64.decode("MIGJAoGBAL9ngMDNXrsqgL3NOk1BH5ns7wF44Uic8gGY9lgW83u49V4eHi5pggo4Cza5FQF48oFIuRhbLdhCBSxXDDwQuCuK0RiwLcJftcreZncpoWzZgS785YO5JPmr8NJYTrRV9YS1PijTWgcrh8dLI6Da+1MEwyR/nqW+HGzYqP4s5OZJAgMBAAE="));

	this.onionrouters.addElement(ord);
		ord = new ORDescription("18.244.0.188","moria1",9001,true);
		ord.setOnionKey(Base64.decode("MIGJAoGBALiqAA5BEjA3kjhigdDvwLraYfsgzIWrOgk15sMsZ9oT+uTaw8B6gYrJO3Ld1OYtXvVMXUDsNaPwUUIWMPeNLoBJGSjMVP7ZNQ+AWA7HlAeBx9InHbru9cNU+5aCOsspQoCqgDPSQGgVUM/JtFlmo5DoLCYYCcDHYxnWRGwNRpH5AgMBAAE="));

		this.onionrouters.addElement(ord);
		ord = new ORDescription("80.190.251.24 ","ned",9001,true);
		ord.setOnionKey(Base64.decode("MIGJAoGBAL3w7Uk/pRTyPHIopXRPGjQfKE+tMspppHvBlurAppGnTVIfmjOIuatjUV1gfLG3XAwJdBZfWgUTbazk1EDDUg++A+IVuiT+d0XgHLTAIzpPmyBX2gGv+97hsObfbXHtFbUhVvtfgRHrUIbTKs+vOry9w5XL+NWgP5IOZ1R4E39HAgMBAAE="));
		this.onionrouters.addElement(ord);
   	}

	   /** Updates the list of available ORRouters.
		* @return true if it was ok, false otherwise
		*/

	   public boolean updateList(String server, int port)
	{
		try{
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[UPDATE OR-LIST] Starting update on "+server+":"+port);
		HTTPConnection http=new HTTPConnection(server,port);
		HTTPResponse resp=http.Get("/");
		if( resp.getStatusCode()!=200 )
		{
			return false;
		}
		String doc=resp.getText();
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"ORList: "+doc);
		parseDocument(doc);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[UPDATE OR-LIST] Update finished");
		return true;
		}
		catch(Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"There was a problem with fetching the available ORRouters: "+t.getMessage());
		}
		return false;
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
					LogHolder.log(LogLevel.DEBUG,LogType.MISC,"Added: "+ord);
				}
			}
		}
		onionrouters=ors;
	}
}



/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor.test;

import java.net.InetAddress;

import anon.AnonChannel;
import tor.Circuit;
import tor.OnionRouter;
import tor.ordescription.ORDescription;
import tor.ordescription.ORList;
import logging.*;
import java.util.Vector;
import jap.JAPDebug;
import tor.crypto.CTRBlockCipher;
import java.io.*;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.KeyParameter;
//import org.bouncycastle.crypto.params.
/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class tor2jap {

	public static void main(String[] args) throws Exception {
		/*FileInputStream fin=new FileInputStream("/Users/sk13/tvec");
		byte[] bin=new byte[16384];
		byte[] bout=new byte[16384];
		byte[] fbin=new byte[16384];
		int lrn=fin.read(fbin);
		CTRBlockCipher ci=new CTRBlockCipher(new AESFastEngine());
		ci.init(true,new ParametersWithIV(new KeyParameter(new byte[16]),new byte[16]));
		for(int i=0;i<16384/509;i++)
			ci.processBlock(bin,i*509,bout,i*509,509);
		//ci.processBlock(bin,0,bout,0,16384);
		for(int i=0;i<bout.length;i++)
		{
			if(bout[i]!=fbin[i])
				System.out.println(i+".: "+bout[i]+" - "+fbin[i]);
		}*/
		LogHolder.setLogInstance(JAPDebug.getInstance());
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Establishing connection");
		ORList orl = new ORList();
		orl.updateList("moria.seul.org",9031);
		Vector ors=orl.getList();
		Vector orsToUse=new Vector(2);
		orsToUse.addElement("w");
		orsToUse.addElement("w");
		for(int i=0;i<ors.size();i++)
		{
			ORDescription od=(ORDescription)ors.elementAt(i);
			/*if(od.getName().equalsIgnoreCase("jap"))
				orsToUse.setElementAt(od,0);
			else*/ if(od.getName().equalsIgnoreCase("moria1"))
				orsToUse.setElementAt(od,0);
		}

		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating new circuit");
		Circuit c = new Circuit(10,orsToUse);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connecting");
		c.connect();
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating new Stream ...");
		AnonChannel channel = c.createChannel(InetAddress.getByName("mix.inf.tu-dresden.de"),6544);
		//channel.getOutputStream().write(("GET /index.html HTTP/1.0\n\r\n\r").getBytes());
		for(;;)
		{
			int b=channel.getInputStream().read();
			if(b<0)
				break;
			System.out.print((char)b);
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Closing Connection");
		c.close();
	}
}

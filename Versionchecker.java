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
import java.net.URL;
import java.io.DataInputStream;
import java.io.FileOutputStream;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

/** 
 * This is a class for version checking over the Internet.
 * The programmer has to provide a text file on a web server
 * at http://path.
 * <P>
 * <code>String getNewVersionNumberFromNet(String path)</code> gets the content of this file.
 * This function can be used to compare the version on the Internet with the current version
 * of the running program.
 * <P>
 * <code>void getVersionFromNet(String path, String localFilename)</code> gets the new version
 * of the program from http://path and stores the received data to file <code>localFilename</code>.
 * 
 */
final class Versionchecker implements Runnable {

	private VersioncheckerProgress vcp = null;
	private URL url;
	private int result = 0;
	private String fn;

	public String getNewVersionnumberFromNet(String path) throws Exception 
		{
			try
				{
					URL url=new URL(path);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Versionchecker:"+
						" Prot: "+url.getProtocol()+
						" Host: "+url.getHost()+
						" Port: "+url.getPort()+
						" File: "+url.getFile());
					HTTPConnection.setProxyServer(null,0);
					HTTPConnection con=new HTTPConnection(url.getHost(),
																								url.getPort());
					HTTPResponse resp=con.Get(url.getFile());
					if (resp==null || resp.getStatusCode()!=200)
						throw new Exception("Versioncheck bad response from server: "+resp.getReasonLine());
					// read remaining header lines
					byte[] buff=resp.getData();
					String s=new String(buff).trim();
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"New Version: "+s);
					if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
						return s;
					throw new Exception("Versionfile has wrong format! Found: \""+s+ "\". Should be \"nn.nn.nnn\"!");
				}
			catch(Exception e)
				{
					throw new Exception("Versioncheck getNewVersionnumberFromNet() failed: "+e);
				}
		}
	
	public void getVersionFromNet(String path, String localFilename) throws Exception {
		try {
			this.url = new URL(path);
			this.fn  = localFilename;
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	public void run() {
		try {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Versionchecker:"+
			" Prot: "+url.getProtocol()+
			" Host: "+url.getHost()+
			" Port: "+url.getPort()+
			" File: "+url.getFile());
			HTTPConnection.setProxyServer(null,0);
			HTTPConnection con=new HTTPConnection(url.getHost(),
																						url.getPort());
			HTTPResponse resp=con.Get(url.getFile());
			if (resp.getStatusCode()!=200) {
				throw new Exception("Download: Bad response from server: "+resp.getReasonLine());
			}
			
			int len = -1;
			len=resp.getHeaderAsInt("Content-Length");
			//
			if(len==-1) {
				throw new Exception("Download: Unkown Size!");
			}
			
			byte[] buff=new byte[len];
			DataInputStream in=new DataInputStream(resp.getInputStream());
			int progressCounter = 0;
			int cnt = 0;
			int onePercent = (len/100)+1; // nr of bytes for one percent progress
			while(len>0)
				{
					onePercent=Math.min(onePercent,len); //for the last percent...
					in.readFully(buff,cnt,onePercent);
					cnt+=onePercent;
					len-=onePercent;
					vcp.progress(++progressCounter);
				}
//			
			in.close();
			
			FileOutputStream f=new FileOutputStream(fn);
			f.write(buff);
			f.flush();
			f.close();
			vcp.progress(100);
		}
		catch(Exception e) {
			result = -1;
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"Download error: "+e);
//			throw e;
		}
	}
	
	public int getResult() 
		{
			return result;
		}
	
	public void registerProgress(VersioncheckerProgress o)
		{
			vcp = o;
		}	
}

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
public final class Versionchecker implements Runnable {

	private VersioncheckerProgress vcp = null;
	private URL url;
	private int result = 0;
	private String fn;

	public String getNewVersionnumberFromNet(String path) throws Exception {
		try {
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
			if (resp.getStatusCode()!=200)
				throw new Exception("Versioncheck bad response from server: "+resp.getReasonLine());
			// read remaining header lines
			byte[] buff=resp.getData();
			String s=new String(buff).trim();
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"New Version: "+s);
			if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
				return s;
			throw new Exception("Versionfile has wrong format! Found: \""+s+ "\". Should be \"nn.nn.nnn\"!");
		}
		catch(Exception e) {
			throw new Exception("Versioncheck failed: "+e);
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

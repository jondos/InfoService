import java.net.*;
import java.io.*;

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

/*
 * Stefan: Ich musste das hier alles aendern, da der Versionschecker bei mir immer versucht hat,
 * ueber den (lokalen) Proxy zu gehen, d.h. URL geht nicht. Deshalb musste ich es "von Hand" machen.
 */
	public String getNewVersionnumberFromNet(String path) throws Exception {
		try {
			byte[] buff=new byte[9];
			URL url=new URL(path);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Versionchecker:"+
			" Prot: "+url.getProtocol()+
			" Host: "+url.getHost()+
			" Port: "+url.getPort()+
			" File: "+url.getFile());
			if (url.getProtocol().equalsIgnoreCase("http")!=true)
				throw new Exception("Versioncheck wrong protocol: "+url.getProtocol()+" Should be http!");
			Socket socket = new Socket(url.getHost(),((url.getPort()==-1)?80:url.getPort()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			out.write("GET "+url.getFile()+" HTTP/1.0\r\n\r\n");
			out.flush();
			DataInputStream in=new DataInputStream(socket.getInputStream());
			String line = readLine(in);
			if (line.indexOf("200") == -1)
				throw new Exception("Versioncheck bad response from server: "+line);
			// read remaining header lines
			while (line.length() != 0) {
				line = readLine(in);
			}
			in.readFully(buff);
			in.close();
			out.close();
			socket.close();
			String s=new String(buff);
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
			if (url.getProtocol().equalsIgnoreCase("http")!=true)
				throw new Exception("Download: Wrong protocol: "+url.getProtocol()+" Should be http!");
			Socket socket = new Socket(url.getHost(),((url.getPort()==-1)?80:url.getPort()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			out.write("GET "+url.getFile()+" HTTP/1.0\r\n\r\n");
			out.flush();
			DataInputStream in=new DataInputStream(socket.getInputStream());
			String line = readLine(in);
			if (line.indexOf("200") == -1) {
				throw new Exception("Download: Bad response from server: "+line);
			}
			// read remaining header lines
			int len = -1;
			while (line.length() != 0) {
				int firstSpacePosition = line.indexOf(' ');
				if (firstSpacePosition == -1) {
					throw new Exception("Download: Bad header: missing space!");
				}
				String headerCommand = line.substring(0,firstSpacePosition);
				String headerValue   = line.substring(firstSpacePosition + 1).trim();
				if (headerCommand.equalsIgnoreCase("Content-length:")) {
					len = Integer.parseInt(headerValue);
				}
				line = readLine(in);
			}
			//
			if(len==-1) {
				throw new Exception("Download: Unkown Size!");
			}
			
			byte[] buff=new byte[len];
//entweder:			
//			in.readFully(buff);
//oder:		
			int progressCounter = 0;
			int cnt = 0;
			int onePercent = len/100; // nr of bytes for one percent progress
			for (int i = 0; i < len; i++) {
				buff[i]=in.readByte();
				cnt++;
				if (cnt == onePercent) {
					progressCounter++;
					vcp.progress(progressCounter);
					cnt = 0;
				}
			}
//			
			in.close();
			out.close();
			socket.close();
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
	
	public int getResult() {
		return result;
	}
	
	public void registerProgress(Object o) {
		this.vcp = (VersioncheckerProgress)o;
	}
	
    private String readLine(DataInputStream inputStream) throws Exception {
		String returnString = "";
		try{
			int byteRead = inputStream.read();
			while (byteRead != 10 && byteRead != -1) {
			if (byteRead != 13) returnString += (char)byteRead;
			byteRead = inputStream.read();
			}
		} catch (Exception e) {
			throw e;
		}
		return returnString;
    }
	
}

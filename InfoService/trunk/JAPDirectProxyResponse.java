

import java.io.*;
import java.net.*;
import java.text.*;

public class JAPDirectProxyResponse implements Runnable /*extends Thread*/ {
    private int threadNumber;
    private static int threadCount;

    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private Socket inputSocket,outputSocket;

    public JAPDirectProxyResponse (Socket in, Socket out) {
	this.inputSocket = in;
	this.outputSocket = out;
    }

    public void run() {
	threadNumber = getThreadNumber();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - Response thread started.");
	try {
	    inputStream  = new DataInputStream(inputSocket.getInputStream());
	    outputStream = new DataOutputStream(outputSocket.getOutputStream());
/*//----------------------------------------------	    
	    int byteRead = inputStream.read();
//	    System.out.print((char)byteRead);
	    while (byteRead != -1) {
		outputStream.write(byteRead);
		byteRead = inputStream.read();
//		System.out.print((char)byteRead);
	    }
*///------------------------------------------------- 
//------------BUFFERED---------------------------		
		byte[] buff=new byte[1000];
		int len;
		while((len=inputStream.read(buff))!=-1) {
			if(len>0) {
				outputStream.write(buff,0,len);
			}
		}
//-----------------------------------------------		
	    outputStream.flush();
	    JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - EOF from Server.");
	} catch (IOException ioe) {
	    // this is normal when we get killed
	    // so just do nothing...
//	    JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"R("+threadNumber+") - IOException: " + ioe);
	} catch (Exception e) {
 	    JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"R("+threadNumber+") - Exception during transmission: " + e);
	}
	try{
	    inputStream.close();
	    outputStream.close();
	} catch (Exception e) {
	    JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"R("+threadNumber+") - Exception while closing: " + e);
	}
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - Response thread stopped.");

    }
    
    private synchronized int getThreadNumber() {
	return threadCount++;
    }
}

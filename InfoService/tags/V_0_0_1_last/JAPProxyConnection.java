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
import java.io.InputStream;

public final class JAPProxyConnection extends Thread { 
	private JAPSocket inSocket; 
	private JAPMuxSocket outSocket;
	private InputStream fromClient; 
	private int channel;

    public JAPProxyConnection (JAPSocket s, int channelID, JAPMuxSocket muxSocket) { 
		inSocket = s;
		channel=channelID; 
		outSocket=muxSocket; 
	}
	
	public void run() { 
		try { 
			fromClient = inSocket.getInputStream(); 
			byte[] buff=new byte[1000]; 
			int len; 
			while((len=fromClient.read(buff,0,1000))!=-1) {
				if(len>0&&outSocket.send(channel,buff,(short)len)==-1) {
					break; 
				} 
			} 
		} // if (protocol....)  
		catch (Exception e) { }
		try { 
			fromClient.close(); 
		} catch(Exception e) { } 
		try {
			if(!inSocket.isClosed()) { 
				outSocket.close(channel);
				inSocket.close(); 
			} 
		} catch (Exception e) {
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.THREAD,"ProxyConnection - Exception while closing: "+e); 
		} 
	} 
}

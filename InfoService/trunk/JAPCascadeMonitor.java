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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.text.NumberFormat;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import anon.JAPAnonService;

public final class JAPCascadeMonitor implements Runnable {
	private int IDLETIME = 60000;
	private ServerSocket listener=null;

	public void CascadeMonitor() {
	}
	
	public synchronized void run() {
/*		
		try {
			if (listener==null) listener = new ServerSocket(model.getPortNumber()+1);
		}
		catch (Exception ex) {
		}
		while(runFlag) {
			// get the feedback from InfoSercive
			statusTextField.setText(model.getString("chkGettingFeedback"));
			Enumeration enum = db.elements();
			while (enum.hasMoreElements()) {
				model.getInfoService().getFeedback((AnonServerDBEntry)enum.nextElement());
				tableView.repaint();
			}
			statusTextField.setText(model.getString("chkFeedbackReceived"));
			// connect to all Mix cascades
			int nr = db.size();
			if (listener==null) {
				nr = 0;
				statusTextField.setText(model.getString("chkListenerError"));
			}
			try {
				t.sleep(2000);
			} catch (Exception e) {
			}			
			for(int i=0;i<nr;i++) {
				AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(i);
				statusTextField.setText(model.getString("chkCnctToCasc")+" "+e.getName());
				e.setStatus(model.getString("chkConnecting"));
				tableView.repaint();
				// create the AnonService
				JAPAnonService proxyAnon=new JAPAnonService(listener,JAPAnonService.PROTO_HTTP);
				if (model.getUseProxy()) {
					// connect vi proxy to first mix (via ssl portnumber)
					if (model.anonSSLPortNumber == -1) {
						proxyAnon.setAnonService(e.getHost(),e.getPort());
						proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
						proxyAnon.connectViaFirewall(true);
					} else {
						proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
						proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
						proxyAnon.connectViaFirewall(true);
					}
				} else {
					// connect directly to first mix
					proxyAnon.setAnonService(e.getHost(),e.getPort());
				}
				// start the AnonService
				long dtConnect  = 0;
				long dtResponse = 0;
				long t1 = 0;
				long t2 = 0;
				t1 = System.currentTimeMillis();
				int ret=proxyAnon.start();
				t2 = System.currentTimeMillis();
				dtConnect = t2-t1;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t1);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t2);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+dtConnect);
				if(ret==JAPAnonService.E_SUCCESS)     e.setStatus(model.getString("chkConnected"));
				else if (ret==JAPAnonService.E_BIND)  e.setStatus(model.getString("chkBindError"));		
				else                                  e.setStatus(model.getString("chkConnectionError"));
				tableView.repaint();
				// if sucessfull perform check
				if (ret==JAPAnonService.E_SUCCESS) {					
					// send request via AnonService
					//
					try {						
						URL url = new URL("http://"+model.getInfoServiceHost()+":"+model.getInfoServicePort()+model.aktJAPVersionFN); 
//						URL url = new URL("http://www.inf.tu-dresden.de/cgi-bin/cgiwrap/hf2/img.cgi/monitor"); 
						HTTPConnection c = new HTTPConnection(url.getHost(),url.getPort());
						c.setProxyServer(InetAddress.getLocalHost().getHostAddress(),model.getPortNumber()+1);
						c.setAllowUserInteraction(false);
						c.setTimeout(6000);
						t1 = System.currentTimeMillis();
						HTTPResponse resp = c.Get(url.getFile());
						t2 = System.currentTimeMillis();
						dtResponse = t2-t1;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t1);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t2);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+dtResponse);
						if (resp==null || resp.getStatusCode()!=200) {
							e.setStatus(model.getString("chkBadResponse"));
						} else {
							e.setStatus(model.getString("chkCascResponding"));
						}
						tableView.repaint();
						byte[] buff=resp.getData();
						String s=new String(buff).trim();
						if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
							e.setStatus("OK");
						tableView.repaint();
					}
					catch (Exception ex) {
						e.setStatus(model.getString("chkConButError"));
						tableView.repaint();
					}					
				}
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMaximumFractionDigits(2);
				e.setDelay("" + nf.format((float)dtConnect/100.0) + "/" + nf.format((float)dtResponse/100.0) + " s");
				tableView.repaint();
				ret = proxyAnon.stop();
			}
			statusTextField.setText(model.getString("chkIdle")+" "+IDLETIME/1000+" s");
			view.setCursor(Cursor.getDefaultCursor());
			// sleep for a while
			try {
				t.sleep(IDLETIME);
			} catch (Exception e) {
			}
		}
*/		
	}
}

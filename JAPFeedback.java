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
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

final class JAPFeedback implements Runnable {
	
	private JAPModel model;
	private volatile boolean runFlag;
	
	private Thread m_threadRunLoop;
	
	public JAPFeedback()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPFeedback:initializing...");
			model = JAPModel.getModel();
			m_threadRunLoop=null;
			runFlag=false;
		}
		
	public void run()
		{
			runFlag = true;
			while(runFlag)
				{
					if (model.isAnonMode())
						{
							model.getInfoService().getFeedback(model.getAnonServer());
							model.notifyJAPObservers();
						}
					try 
						{
							Thread.sleep(60000);
							//Thread.sleep(6000); // for testing only
						}
					catch (Exception e)
						{}
				}
		}
	
	public void startRequests() 
		{
			if(!runFlag)
				{
					m_threadRunLoop=new Thread(this);
					m_threadRunLoop.setPriority(Thread.MIN_PRIORITY);
					m_threadRunLoop.start();
				}
		}

	public void stopRequests() 
		{
			runFlag = false;
			if(m_threadRunLoop!=null)
				{
					m_threadRunLoop.interrupt();
					try{m_threadRunLoop.join();}catch(Exception e){}
					m_threadRunLoop=null;
				}
		}

}

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
package anon.server.impl;

import logging.Log;
import logging.LogLevel;
import logging.LogType;

class DummyTraffic implements Runnable
	{
		private MuxSocket m_MuxSocket=null;
	  private volatile boolean m_bRun=false;
		private Thread m_threadRunLoop=null;
		private Log m_Log;
    private final static long DUMMY_TRAFFIC_INTERVAL=10000; //How long maximum to wait between packets ?

		public DummyTraffic(MuxSocket muxSocket,Log log)
			{
        m_Log=log;
				m_MuxSocket=muxSocket;
				m_bRun=false;
				m_threadRunLoop=null;
			}

    public void setLogging(Log log)
      {
        m_Log=log;
      }

    public void run()
			{
				while(m_bRun)
					{
						if(System.currentTimeMillis()-m_MuxSocket.getTimeLastPacketSend()>DUMMY_TRAFFIC_INTERVAL)
							{
							  m_Log.log(LogLevel.DEBUG,LogType.NET,"Sending Dummy!");
								m_MuxSocket.send(12345,0,null,(short)0); //this is a channel close for a hopefully non existend channel
						  }
						try
							{
								m_threadRunLoop.sleep(10000);
							}
						catch(InterruptedException e)
							{
							}
					}
			}

		public void start()
			{
				m_threadRunLoop=new Thread(this);
				m_bRun=true;
				m_threadRunLoop.start();
			}

		public void stop()
			{
				m_bRun=false;
				m_threadRunLoop.interrupt();
			  try
					{
						m_threadRunLoop.join();
					}
				catch(Exception e)
					{
					}
				m_threadRunLoop=null;
			}
	}
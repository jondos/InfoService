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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final class DummyTraffic implements Runnable
{
	private MuxSocket m_MuxSocket = null;
	private volatile boolean m_bRun = false;
	private Thread m_threadRunLoop = null;
	private int m_Intervall;

//    private final static long DUMMY_TRAFFIC_INTERVAL=10000; //How long maximum to wait between packets ?

	public DummyTraffic(MuxSocket muxSocket, int intervall)
	{
		m_MuxSocket = muxSocket;
		m_bRun = false;
		m_threadRunLoop = null;
		m_Intervall = intervall;
	}

	public void run()
	{
		while (m_bRun)
		{
			if (System.currentTimeMillis() - m_MuxSocket.getTimeLastPacketSend() > m_Intervall)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sending Dummy!");
				m_MuxSocket.sendDummy(); //send a dummy
			}
			try
			{
				m_threadRunLoop.sleep(m_Intervall);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	public synchronized void start()
	{
		if (!m_bRun)
		{
			m_threadRunLoop = new Thread(this, "JAP - Dummy Traffic");
			m_bRun = true;
			m_threadRunLoop.start();
		}
	}

	public synchronized void stop()
	{
		m_bRun = false;
		try
		{
			if (m_threadRunLoop != null)
			{
				m_threadRunLoop.interrupt();
				m_threadRunLoop.join();
			}
		}
		catch (Exception e)
		{
		}
		m_threadRunLoop = null;
	}
}

/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jap;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final class JAPFeedback implements Runnable
{
	private final Object THREAD_SYNC = new Object();
	private JAPController controller;

	private Thread m_threadRunLoop;

	public JAPFeedback()
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPFeedback:initializing...");
		controller = JAPController.getInstance();
	}

	public void run()
	{
		while (!Thread.currentThread().isInterrupted())
		{
			if (controller.getAnonMode() && !JAPModel.isInfoServiceDisabled())
			{
				controller.getCurrentMixCascade().fetchCurrentStatus();
				controller.notifyJAPObservers();
			}
			try
			{
				synchronized (Thread.currentThread())
				{
					Thread.currentThread().wait(60000);
				}
				//Thread.sleep(6000); // for testing only
			}
			catch (InterruptedException a_e)
			{
				break;
			}
		}
	}

	public void startRequests()
	{
		synchronized (THREAD_SYNC)
		{
			if (m_threadRunLoop == null)
			{
				m_threadRunLoop = new Thread(this, "JAP - Feedback");
				m_threadRunLoop.setDaemon(true);
				m_threadRunLoop.setPriority(Thread.MIN_PRIORITY);
				m_threadRunLoop.start();
			}
		}
	}

	public void stopRequests()
	{
		synchronized (THREAD_SYNC)
		{
			if (m_threadRunLoop != null)
			{
				while (m_threadRunLoop.isAlive())
				{
					try
					{
						synchronized (m_threadRunLoop)
						{
							m_threadRunLoop.notifyAll();
							m_threadRunLoop.interrupt();
						}
						m_threadRunLoop.join(1000);
					}
					catch (InterruptedException a_e)
					{
						// ignore
					}
				}
				m_threadRunLoop = null;
			}
		}
	}

}

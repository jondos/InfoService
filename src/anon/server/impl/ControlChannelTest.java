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

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class ControlChannelTest extends SyncControlChannel implements Runnable
{
	private Thread m_Thread;
	private volatile boolean m_bRun;

	public ControlChannelTest()
	{
		super(255, false);
		m_bRun = true;
		m_Thread = new Thread(this, "ControlChannelTest - loop");
		m_Thread.setDaemon(true);
		m_Thread.start();
	}

	public void finalize()
	{
		m_bRun = false;
		m_Thread.interrupt();
	}

	public void proccessXMLMessage(Document docMsg)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "ControlTestChannel received a message: " + XMLUtil.toString(docMsg));
	}

	public void run()
	{
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element elemRoot = doc.createElement("TestControlChannelMessages");
			doc.appendChild(elemRoot);
			int count = 1;
			while (m_bRun)
			{
				try
				{
					Thread.sleep(60000);
				}
				catch (InterruptedException e)
				{
					continue;
				}
				elemRoot.setAttribute("count", Integer.toString(count));
				count++;
				sendMessage(doc);
			}
		}
		catch (Exception e)
		{
		}
	}
}

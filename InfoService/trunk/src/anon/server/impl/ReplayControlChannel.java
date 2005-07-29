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

import org.w3c.dom.Document;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.Vector;
import org.w3c.dom.Node;

final public class ReplayControlChannel extends SyncControlChannel
{


	private IReplayCtrlChannelMsgListener m_MsgListener;

	public ReplayControlChannel()
	{
		super(3, false);
		m_MsgListener = null;
	}

	public void setMessageListener(IReplayCtrlChannelMsgListener theListener)
	{
		m_MsgListener = theListener;
	}

	public void proccessXMLMessage(Document docMsg)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "ReplayControlChannel received a message: " + XMLUtil.toString(docMsg));
		if (m_MsgListener == null)
		{
			return; //nothing todo
		}
		Element elemRoot = docMsg.getDocumentElement();
		if (elemRoot.getNodeName().equals("Mixes"))
		{
			Vector v = new Vector();
			Node elemChild = XMLUtil.getFirstChildByName(elemRoot, "Mix");
			while (elemChild != null)
			{
				String strMixID = XMLUtil.parseAttribute(elemChild, "id", null);
				Node elemReplay = XMLUtil.getFirstChildByName(elemChild, "Replay");
				Node elemReplayTimestamp = XMLUtil.getFirstChildByName(elemReplay, "ReplayTimestamp");
				int offset = XMLUtil.parseAttribute(elemReplayTimestamp, "offset", -1);
				int interval = XMLUtil.parseAttribute(elemReplayTimestamp, "interval", -1);
				if (interval != -1 && offset != -1 && strMixID != null)
				{
					ReplayTimestamp rt = new ReplayTimestamp(strMixID, interval, offset);
					v.addElement(rt);
				}
				elemChild=elemChild.getNextSibling();
			}
			if (v.size() > 0)
			{
				ReplayTimestamp[] rts = new ReplayTimestamp[v.size()];
				v.copyInto(rts);
				m_MsgListener.gotTimestamps(rts);
			}
		}
	}

	/** Sends a request for the current Replaytimestamps via the replay control channel.
	 * @return E_SUCCESS if successful, errorcode otherwise
	 *
	 */
	synchronized int getTimestamps()
	{
		return sendXMLMessage("<?xml version=\"1.0\" encoding=\"UTF-8\"?><GetTimestamps/>");
	}
}

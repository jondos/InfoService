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
import anon.ErrorCodes;

public abstract class AbstractControlChannel
{
	private int m_iChannelID;
	private ControlChannelDispatcher m_Dispatcher;
	public AbstractControlChannel(int channelId, boolean bIsEncrypted)
	{
		m_iChannelID = channelId;
		m_Dispatcher = null;
	}

	protected void setDispatcher(ControlChannelDispatcher a_Dispatcher)
	{
		m_Dispatcher = a_Dispatcher;
	}

	public int getID()
	{
		return m_iChannelID;
	}

	public int sendMessage(Document docMsg)
	{
		byte[] msg = XMLUtil.toString(docMsg).getBytes();
		if (msg.length > 0xFFFF)
		{
			return ErrorCodes.E_SPACE;
		}
		byte[] msglen = new byte[2];
		msglen[0] = (byte) ( (msg.length >> 8) & 0x00FF);
		msglen[1] = (byte) ( (msg.length) & 0x00FF);
		m_Dispatcher.sendMessages(m_iChannelID, false, msglen, 2);
		m_Dispatcher.sendMessages(m_iChannelID, false, msg, msg.length);
		return ErrorCodes.E_SUCCESS;
	}

	abstract void proccessMessage(byte[] msg, int len);

	abstract void proccessMessageComplete();
}

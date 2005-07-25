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

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public abstract class SyncControlChannel extends AbstractControlChannel
{
	private int m_MsgBytesLeft, m_aktIndex;
	private byte m_MsgBuff[];
	public SyncControlChannel(int channelID, boolean bIsEncrypted)
	{
		super(channelID, bIsEncrypted);
		m_MsgBytesLeft = 0;
		m_aktIndex = 0;
		m_MsgBuff = new byte[0xFFFF + 2];
	}

	void proccessMessage(byte[] msg, int msglen)
	{
		if (m_MsgBytesLeft == 0) //start of new XML Msg
		{
			if (msglen < 2) //this should never happen...
			{
				return;
			}
			m_MsgBytesLeft = ( (msg[0] << 8) & 0x00FF00) | (msg[1] & 0x00FF);
			msglen -= 2;
			m_aktIndex = msglen;
			m_MsgBytesLeft -= msglen;
			System.arraycopy(msg, 2, m_MsgBuff, 0, msglen);
		}
		else //received some part...
		{
			msglen = Math.min(m_MsgBytesLeft, msglen);
			System.arraycopy(msg, 0, m_MsgBuff, m_aktIndex, msglen);
			m_aktIndex += msglen;
			m_MsgBytesLeft -= msglen;
		}
		if (m_MsgBytesLeft == 0)
		{ //whole msg receveid
			proccessMessageComplete();
		}
	}

	public abstract void proccessXMLMessage(Document docMsg);

	void proccessMessageComplete()
	{
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new ByteArrayInputStream(this.m_MsgBuff, 0, this.m_aktIndex));
			if (doc != null)
			{
				proccessXMLMessage(doc);
			}
		}
		catch (Exception e)
		{
		}
	}

}

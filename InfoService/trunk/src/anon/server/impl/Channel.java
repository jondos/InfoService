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

import java.io.IOException;

final class Channel extends AbstractChannel
{
	private MuxSocket m_muxSocket;
	private int m_type;
	private boolean m_bFirstPacket;

	Channel(MuxSocket muxSocket, int id, int type) throws IOException
	{
		super(id);
		m_type = type;
		m_muxSocket = muxSocket;
		m_bFirstPacket = true;
	}

	protected void close_impl()
	{
		m_muxSocket.close(m_id);
	}

	protected void send(byte[] buff, int len)
	{
		if (len >= 957)
		{
			int i=3;
		}
		m_muxSocket.send(m_id, m_type, buff, (short) len);
		m_bFirstPacket = false;
	}

	public int getOutputBlockSize()
	{
		if (m_bFirstPacket)
		{
			return MuxSocket.PAYLOAD_SIZE - m_muxSocket.getNumberOfMixes() * MuxSocket.KEY_SIZE;
		}
		else
		{
			return MuxSocket.PAYLOAD_SIZE;
		}

	}
}

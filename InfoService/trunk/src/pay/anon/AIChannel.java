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
package pay.anon;

import java.io.IOException;
import anon.server.impl.AbstractChannel;
import anon.server.impl.MuxSocket;

/**
 * Channel mit der Nummer 0xFFFFFFFF der nur für die Kommunikation mit der AI benutzt wird
 * @author Grischan Gl&auml;nzel
 */

public final class AIChannel extends AbstractChannel
{
	private static AIChannel me;
	protected MuxSocket m_muxSocket;

	private AIChannel(MuxSocket muxSocket) throws IOException
	{
		super(0xFFFFFFFF);
		m_muxSocket = muxSocket;
	}

	public static AIChannel create(MuxSocket muxSocket)
	{
		try
		{
			if (me == null)
			{
				me = new AIChannel(muxSocket);
			}
		}
		catch (Exception ex)
		{}
		return me;
	}

	protected void close_impl()
	{}

	protected void send(byte[] buff, int len)
	{
		byte[] toSend = new byte[len];
		System.arraycopy(buff, 0, toSend, 0, len);
		m_muxSocket.sendPayPackets(toSend);
	}
	public int getOutputBlockSize()
	{
		return 1;
	}

}

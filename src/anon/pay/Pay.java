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
package anon.pay;

import anon.server.impl.MuxSocket;
import anon.infoservice.ImmutableProxyInterface;

/**
 * This class is the high-level part of the communication with the AI.
 * It contains functions for creating accounts, charging, etc.
 *
 * @author Andreas Mueller, Grischan Glaenzel, Bastian Voigt
 * @todo rewrite documentation
 */
public class Pay
{
	/** the MuxSocket, needed for counting the bytes */
	private MuxSocket m_MuxSocket;

	/** the control channel */
	private AIControlChannel m_AIControlChannel;

	/**
	 * make default constructor private: singleton
	 * @param thBI BI
	 * @param accountsData Element the xml account configuration.
	 */
	public Pay(MuxSocket currentMuxSocket, ImmutableProxyInterface a_proxy)
	{
		// register AI control channel
		m_MuxSocket = currentMuxSocket;
		m_AIControlChannel = new AIControlChannel(m_MuxSocket, new ImmutableProxyInterface[]{a_proxy});
		m_MuxSocket.getControlChannelDispatcher().registerControlChannel(m_AIControlChannel);
	}

	public AIControlChannel getAIControlChannel()
	{
		return m_AIControlChannel;
	}

	/**
	 * removes the controlchannel
	 */
	public void shutdown()
	{
		m_MuxSocket.getControlChannelDispatcher().removeControlChannel(m_AIControlChannel);
	}
}

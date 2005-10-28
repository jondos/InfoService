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
package anon.mixminion;

import anon.AnonService;
import anon.AnonServerDescription;
import anon.infoservice.ImmutableProxyInterface;
import anon.AnonChannel;
import java.net.ConnectException;
import anon.AnonServiceEventListener;
import anon.tor.TorSocksChannel;

/** This class implements the Mixminion anonymity service, which can be used to sent anonymous e-mail
 *
 */
public class Mixminion implements AnonService
{
	private static Mixminion ms_theMixminionInstance = null;

	private Mixminion()
	{
	}

	public int initialize(AnonServerDescription anonServer)
	{
		return 0;
	}

	public int setProxy(ImmutableProxyInterface a_Proxy)
	{
		return 0;
	}

	public void shutdown()
	{
	}

	public boolean isConnected()
	{
		return false;
	}

	/**
	 * creates a SMTP channel which sents e-mail through the mixminion-network
	 * @param type
	 * channeltype - only AnonChannel.SMTP is supported at the moment
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public AnonChannel createChannel(int type) throws ConnectException
	{
		if (type != AnonChannel.SMTP)
		{
			return null;
		}
		try
		{
			return new MixminionSMTPChannel();
		}
		catch (Exception e)
		{
			throw new ConnectException("Could not create a Mixminion-Channel: " + e.getMessage());
		}
	}

	/** Always returns NULL as normal TCP/IP channels are not supported at the moment
	 */
	public AnonChannel createChannel(String host, int port) throws ConnectException
	{
		return null;
	}

	public void addEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListener(AnonServiceEventListener l)
	{
	}

	/**
	 * Returns a Instance of Mixminion
	 * @return a Instance of Mixminion
	 */
	public static Mixminion getInstance()
	{
		if (ms_theMixminionInstance == null)
		{
			ms_theMixminionInstance = new Mixminion();
		}
		return ms_theMixminionInstance;
	}

}

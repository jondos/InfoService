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
package misc;

import jap.JAPConstants;
import jap.JAPDebug;
import jap.JAPModel;

import java.net.ServerSocket;
import java.util.Properties;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import proxy.AnonProxy;
import proxy.ProxyListener;
import anon.ErrorCodes;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;

final class JAPLean implements ProxyListener
{

	static AnonProxy japAnonProxy = null;

	static int portNumberHTTPListener;
	static int portNumberMixCascade;
	static String hostNameMixCascade;

	static int nrOfChannels = 0;
	static int nrOfBytes = 0;

	JAPLean() throws Exception
	{
		try
		{
			Properties systemProperties = System.getProperties();
			systemProperties.put("java.awt.headless", "true");
			System.setProperties(systemProperties);
		}
		catch (Throwable t)
		{
		}

		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(LogType.ALL);
		JAPDebug.getInstance().setLogLevel(LogLevel.DEBUG);
		JAPModel.getInstance();
		HTTPConnectionFactory.getInstance().setTimeout(JAPConstants.DEFAULT_INFOSERVICE_TIMEOUT);
		InfoServiceHolder.getInstance().setPreferredInfoService(
				  new InfoServiceDBEntry("DeafultIS",
						new ListenerInterface(JAPConstants.DEFAULT_INFOSERVICE_HOSTNAME,
											  JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBER).toVector(),true,true));
		// JAPAnonService.init();
		ServerSocket listener = null;
		try
		{
			listener = new ServerSocket(portNumberHTTPListener);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		japAnonProxy = new AnonProxy(listener, null);
		japAnonProxy.setMixCascade(new MixCascade(null, null, hostNameMixCascade, portNumberMixCascade));
		int returnCode = japAnonProxy.start();
		japAnonProxy.setProxyListener(this);
		if (returnCode == ErrorCodes.E_SUCCESS)
		{
			System.out.print("Amount of anonymized bytes: ");
			Thread t = new Thread(new JAPLeanActivityLoop());
			t.start();
		}
		else if (returnCode == AnonProxy.E_BIND)
		{
			System.err.println("Error binding listener!");
			System.exit(1);
		}
		else
		{
			System.err.println("Error connecting to anon service!");
			System.exit(1);
		}
	}

	public static void main(String[] argv) throws Exception
	{
		// check for command line
		if (argv == null || argv.length < 3)
		{
			System.err.println("Usage: JAPLean <listener_port> <first_mix_address> <first_mix_port>");
			System.exit(1);
		}
		portNumberHTTPListener = Integer.parseInt(argv[0]);
		hostNameMixCascade = argv[1];
		portNumberMixCascade = Integer.parseInt(argv[2]);
		System.out.println("[" + portNumberHTTPListener + "]-->[" + hostNameMixCascade + ":" +
						   portNumberMixCascade + "]");
		try
		{
			new JAPLean();
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}

	/* Implementation of Interface JAPAnonServiceListener */
	public void channelsChanged(int channels)
	{
		nrOfChannels = channels;
	}

	/* Implementation of Interface JAPAnonServiceListener */
	public void transferedBytes(int bytes,int i)
	{
		nrOfBytes += bytes;
	}

	private final class JAPLeanActivityLoop implements Runnable
	{
		int nrOfBytesBefore = -1;
		public void run()
		{
			while (true)
			{
				if (nrOfBytesBefore < nrOfBytes)
				{
					System.out.print("[" + nrOfBytes + "] ");
					nrOfBytesBefore = nrOfBytes;
				}
				try
				{
					Thread.sleep(60000);
				}
				catch (Exception e)
				{
				}
			}
		}
	}
}

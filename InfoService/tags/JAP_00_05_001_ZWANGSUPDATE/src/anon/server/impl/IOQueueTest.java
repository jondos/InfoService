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

public class IOQueueTest implements Runnable
{
	boolean m_bIsConsumer;
	IOQueue m_Queue;
	private static byte[] outBuff;
	public IOQueueTest(boolean bConsumer, IOQueue queue)
	{
		m_bIsConsumer = bConsumer;
		m_Queue = queue;
	}

	public void run()
	{
		try
		{
			byte[] buff = new byte[1000];
			if (m_bIsConsumer)
			{
				System.out.println("Start read");
				int v = 0;
				int t = 0;
				for (; ; )
				{
					int len = (int) (Math.random() * 1000);
					len = m_Queue.read(buff, 0, len);
					//System.out.println("Read: "+len);
					for (int i = 0; i < len; i++)
					{
						t = (int) (buff[i] & 0x00FF);
						if (t != v)
						{
							System.out.println("Error");
						}
						v++;
						if (v > 10)
						{
							v = 0;
						}
					}
					Thread.sleep( (int) (Math.random() * 100));
				}
			}

			else
			{
				System.out.println("Start write");
				int aktIndex = 0;
				byte j=0;
				for (; ; )
				{
					int len = (int) (Math.random() * 1000);
					for(int k=0;k<len;k++)
					{
						outBuff[aktIndex+k]=j;
						j++;
						if(j==11)
							j=0;
					}
					m_Queue.write(outBuff, aktIndex, len);
					aktIndex += len;
					aktIndex = aktIndex % 256;
					Thread.sleep( (int) (Math.random() * 0));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] h)
	{
		outBuff = new byte[2560];
		for (int j = 0; j < 10; j++)
		{
			for (int i = 0; i < 256; i++)
			{
				outBuff[i + j * 256] = (byte) i;
			}
		}
		IOQueue queue = new IOQueue();
		new Thread(new IOQueueTest(true, queue)).start();
		new IOQueueTest(false, queue).run();
	}
}

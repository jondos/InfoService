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
package anon;
import java.io.InputStream;
import JAPDebug;

public class JAPAnonChannel implements Runnable
	{
		private final JAPSocket inSocket;
		private final JAPMuxSocket outSocket;
		private final int channel;
		private final int m_Type;

    	public JAPAnonChannel (JAPSocket s, int channelID, int type,JAPMuxSocket muxSocket)
				{
					inSocket = s;
					channel=channelID;
					outSocket=muxSocket;
					m_Type=type;
				}

		public void run()
			{
				InputStream fromClient=null;
				try
					{
						fromClient= inSocket.getInputStream();
						byte[] buff=new byte[JAPMuxSocket.DATA_SIZE];
						int len=fromClient.read(buff,0,JAPMuxSocket.DATA_SIZE-3-outSocket.getChainLen()*JAPMuxSocket.KEY_SIZE);
						if(len!=-1)
							{
								outSocket.send(channel,m_Type,buff,(short)len);
								while((len=fromClient.read(buff,0,JAPMuxSocket.DATA_SIZE-3))!=-1)
									{
										if(len>0)
											{
												int ret=outSocket.send(channel,m_Type,buff,(short)len);
												if(ret==-1)
													break;
												//if(ret==JAPMuxSocket.E_CHANNEL_SUSPENDED)
												//	sleep(1000);
											}
									}
							}
					} // if (protocol....)
				catch(Exception e)
					{
					}
				try
					{
						fromClient.close();
					}
				catch(Exception e)
					{
					}
				try
					{
						if(!inSocket.isClosed())
							{
								outSocket.close(channel);
								inSocket.close();
							}
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.THREAD,"JAPAnonChannel:Exception while closing: "+e);
					}
				JAPDebug.out(JAPDebug.INFO,JAPDebug.THREAD,"JAPAnonChannel:Channel "+Integer.toString(channel)+" closed.");
			}
	}

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
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public final class JAPSocket 
	{
		private volatile boolean bisClosed;
		private Socket s;
		
		public JAPSocket()
			{
				bisClosed=false;
			}
		
		public JAPSocket(Socket so)	
			{
				s=so;
				bisClosed=false;
			}
		
		public void close() throws IOException
			{
				synchronized(this)
					{
						if(!bisClosed)
							{
								bisClosed=true;
								try
									{
										s.close();
									}
								catch (IOException ioe)
									{
										throw ioe;
									}
							}
					}
			}
		
		public boolean isClosed()
			{
				synchronized(this)
					{
						return bisClosed;
					}
			}
		
		public OutputStream getOutputStream()
			{
				try
					{
						return s.getOutputStream();
					}
				catch(Exception e)
					{
						return null;
					}
			}
	
		public InputStream getInputStream()
			{
				try
					{
						return s.getInputStream();
					}
				catch(Exception e)
					{
						return null;
					}
			}
	}

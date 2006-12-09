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
package proxy;
import jap.JAPDebug;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

final class DirectProxyResponse implements Runnable /*extends Thread*/
	{
		private int threadNumber;
    private static int threadCount;

    private OutputStream outputStream;
    private InputStream inputStream;

    public DirectProxyResponse(InputStream in, OutputStream out)
    {
			inputStream = in;
      outputStream= out;
    }

    public void run()
			{
				threadNumber = getThreadNumber();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - Response thread started.");
				try
					{
						byte[] buff=new byte[1000];
						int len;
						while((len=inputStream.read(buff))!=-1)
							{
								if(len>0)
                {
 									outputStream.write(buff,0,len);
                  }
							}
						//-----------------------------------------------
						outputStream.flush();
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - EOF from Server.");
					}
				catch (IOException ioe)
					{
						 // this is normal when we get killed
						 // so just do nothing...
					}
				catch (Exception e)
					{
 						JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"R("+threadNumber+") - Exception during transmission: " + e);
					}
				try
					{
						inputStream.close();
						outputStream.close();
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"R("+threadNumber+") - Exception while closing: " +e.toString());
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R("+threadNumber+") - Response thread stopped.");
    }

    private synchronized int getThreadNumber() {
	return threadCount++;
    }
}
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

import anon.AnonChannel;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

abstract class AbstractChannel implements AnonChannel
  {
    private boolean m_bIsClosedByPeer=false;
    private boolean m_bIsClosed=false;
    protected int m_id;
    protected int m_type;


    private ChannelInputStream m_inputStream;
    private ChannelOutputStream m_outputStream;

    AbstractChannel(int id,int type) throws IOException
      {
        m_id=id;
        m_type=type;
        m_bIsClosedByPeer=false;
        m_bIsClosed=false;
        m_inputStream=new ChannelInputStream(this);
        m_outputStream=new ChannelOutputStream(this);
      }

    public void finalize()
      {
        close();
      }

    public InputStream getInputStream()
      {
        return m_inputStream;
      }

     public OutputStream getOutputStream()
      {
        return m_outputStream;
      }

    public /*synchronized*/ void close()
      {
        try
          {
            if(!m_bIsClosed&&!m_bIsClosedByPeer)
              {
                m_outputStream.close();
                m_inputStream.close();
                close_impl();
              }
          }
        catch(Exception e)
          {
          }
        m_bIsClosed=true;
      }

    //Use m_id
    abstract protected void close_impl();

    //called from the AnonService to send data to this channel
    protected /*synchronized*/ void recv(byte[] buff,int pos,int len) throws IOException
      {
        m_inputStream.recv(buff,pos,len);
      }

    //called from ChannelOutputStream to send data to the AnonService which belongs to this channel
    abstract protected /*synchronized*/ void send(byte[] buff,int len) throws IOException;

    protected /*synchronized*/ void closedByPeer()
      {
        try
          {
            m_inputStream.closedByPeer();
            m_outputStream.closedByPeer();
          }
        catch(Exception e)
          {
          }
        m_bIsClosedByPeer=true;
      }



  }

final class IOQueue
  {
    private byte[] buff;
    private int readPos;
    private int writePos;
    private boolean bWriteClosed;
    private boolean bReadClosed;
    private final static int BUFF_SIZE=10000;
    private boolean bFull;

    public IOQueue()
      {
        buff=new byte[BUFF_SIZE];
        readPos=0;
        writePos=0;
        bWriteClosed=bReadClosed=false;
        bFull=false;
      }

    public synchronized void write(byte[] in,int pos,int len) throws IOException
      {
        int toCopy;
        while(len>0)
          {
            if(bReadClosed||bWriteClosed)
              throw new IOException("IOQueue closed");
            if(bFull)
              {
                notify(); //awake readers
                try{wait();}catch(InterruptedException e){throw new IOException("IOQueue write interrupted");} //wait;
                continue;
              }
            if(readPos<=writePos)
              toCopy=BUFF_SIZE-writePos;
            else
              toCopy=readPos-writePos;
            if(toCopy>len)
               toCopy=len;
            System.arraycopy(in,pos,buff,writePos,toCopy);
            pos+=toCopy;
            writePos+=toCopy;
            len-=toCopy;
            if(writePos>=BUFF_SIZE)
              writePos=0;
            if(readPos==writePos)
              bFull=true;
          }//End while
        notify(); //awake Readers
      }

    public synchronized int read() throws IOException
      {
        while(true)
          {
            if(bReadClosed)
              throw new IOException("IOQueue closed");
            if(readPos==writePos&&!bFull) //IOQueue is empty
              {
                if(bWriteClosed)
                  return -1;
                else
                  {
                    notify(); //awake Writers;
                    try{wait();}catch(InterruptedException e){throw new IOException("IOQueue read() interrupted");}
                    continue;
                  }
              }
            int i=buff[readPos++]&0xFF;
            if(readPos>=BUFF_SIZE)
              readPos=0;
            if(bFull)
              {
                bFull=false;
                notify(); //awake Writers;
              }
            return i;
          }
      }

    public synchronized int read(byte[] in,int pos,int len) throws IOException
      {
       while(true)
          {
            if(bReadClosed)
              throw new IOException("IOQueue closed");
            if(readPos==writePos&&!bFull) //IOQueue is empty
              {
                if(bWriteClosed)
                  return -1;
                else
                  {
                    notify(); //awake Writers;
                    try{wait();}catch(InterruptedException e){throw new IOException("IOQueue read() interrupted");}
                    continue;
                  }
              }
            int toCopy;
            if(writePos<=readPos)
              toCopy=BUFF_SIZE-readPos;
            else
              toCopy=writePos-readPos;
            if(toCopy>len)
               toCopy=len;
            System.arraycopy(buff,readPos,in,pos,toCopy);
            readPos+=toCopy;
            if(readPos>=BUFF_SIZE)
              readPos=0;
            if(bFull)
              {
                bFull=false;
                notify(); //awake Writers;
              }
            return toCopy;
          }

      }

    public synchronized int available()
      {
        if(bFull)
          return BUFF_SIZE;
        if(writePos>=readPos)
          return writePos-readPos;
        return BUFF_SIZE-readPos+writePos;
      }

    public synchronized void closeWrite()
      {
        bWriteClosed=true;
        notify();
      }

    public synchronized void closeRead()
      {
        bReadClosed=true;
        notify();
      }

    public synchronized void finalize()
      {
        bReadClosed=bWriteClosed=true;
        notify();
        buff=null;
      }
  }

final class ChannelInputStream extends InputStream
  {
    private IOQueue m_Queue=null;
    private boolean m_bIsClosedByPeer=false;
    private boolean m_bIsClosed=false;
    private AbstractChannel m_channel;

    ChannelInputStream(AbstractChannel c) throws IOException
      {
        m_Queue=new IOQueue();
        m_bIsClosedByPeer=false;
        m_bIsClosed=false;
        m_channel=c;
      }

    protected /*synchronized*/ void recv(byte[] buff,int pos,int len)
      {
        try
          {
             m_Queue.write(buff,pos,len);
          }
        catch(Exception e)
          {
          }
      }

    public synchronized int available() throws IOException
      {
        return m_Queue.available();
      }

    public int read() throws IOException
      {
        return m_Queue.read();
      }

    public int read(byte[] out,int pos,int len) throws IOException
      {
        return m_Queue.read(out,pos,len);
      }

    protected /*synchronized*/ void closedByPeer()
      {
        m_bIsClosedByPeer=true;
       try{m_Queue.closeWrite();}catch(Exception e){}
      }

    public /*synchronized*/ void close() throws IOException
      {
        m_bIsClosed=true;
        if(!m_bIsClosed)
          {
            try{m_Queue.closeWrite();}catch(Exception e){}
            m_Queue.closeRead();
            m_Queue=null;
          }
      }
  }

final class ChannelOutputStream extends OutputStream
  {
    boolean m_bIsClosedByPeer=false;
    boolean m_bIsClosed=false;
    AbstractChannel m_channel=null;

    protected ChannelOutputStream(AbstractChannel c)
      {
        m_channel=c;
      }

    //OutputStream Methods
    public /*synchronized*/ void write(int i) throws IOException
      {
        if(m_bIsClosedByPeer||m_bIsClosed)
          throw new IOException("Channel closed by peer");
        byte[] buff=new byte[1];
        buff[0]=(byte)i;
        m_channel.send(buff,1);
      }

    public /*synchronized*/ void write(byte[] buff,int start,int len) throws IOException
      {
        if(m_bIsClosedByPeer||m_bIsClosed)
          throw new IOException("Channel closed by peer");
        m_channel.send(buff,(short)len);
      }

    public /*synchronized*/ void close()
      {
        if(m_bIsClosed||m_bIsClosedByPeer)
          return;
        m_bIsClosed=true;
      }

    protected void closedByPeer()
      {
      }
  }
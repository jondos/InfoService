package anonnew.server.impl;

import anonnew.AnonChannel;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

class Channel implements AnonChannel
  {
    protected MuxSocket m_muxSocket;
    private boolean m_bIsClosedByPeer=false;
    private boolean m_bIsClosed=false;
    protected int m_id;



    private ChannelInputStream m_inputStream;
    private ChannelOutputStream m_outputStream;

    Channel(MuxSocket muxSocket,int id,int type) throws IOException
      {
        m_muxSocket=muxSocket;
        m_id=id;
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

    /*public void directOutputTo(OutputStream out)
      {
        m_outputStream=out;
      }*/
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
                m_muxSocket.close(m_id);
              }
          }
        catch(Exception e)
          {
          }
        m_bIsClosed=true;
      }
    //called from MuxSocket
    protected /*synchronized*/ void recv(byte[] buff,int pos,int len) throws IOException
      {
 //       m_OutputStream.write(buff,pos,len);
        m_inputStream.recv(buff,pos,len);
      }

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

class Queue
  {
    class QueueEntry
      {
        byte[] buff;
        int len;
        QueueEntry next;
      }

    QueueEntry m_first;
    QueueEntry m_last;
    public Queue()
      {
        m_first=null;
      }

    public void add(byte[] in,int pos,int len)
      {
        QueueEntry newEntry=new QueueEntry();
        newEntry.buff=new byte[len];
        System.arraycopy(newEntry.buff,0,in,pos,len);
        newEntry.len=len;
        newEntry.next=null;
        if(m_last==null)
          {
            m_first=newEntry;
            m_last=newEntry;
          }
        else
         m_last.next=newEntry;
      }

    public int get(byte[] out,int pos,int len)
      {
        return -1;
      }
  }

class ChannelInputStream extends InputStream
  {
    private PipedInputStream m_pipedInputStream;
    private PipedOutputStream m_pipedOutputStream;
    private boolean m_bIsClosedByPeer=false;
    private boolean m_bIsClosed=false;
    private Channel m_channel;

    ChannelInputStream(Channel c) throws IOException
      {
        m_pipedOutputStream=new PipedOutputStream();
        m_pipedInputStream=new PipedInputStream(m_pipedOutputStream);
        m_bIsClosedByPeer=false;
        m_bIsClosed=false;
        m_channel=c;
      }

    protected /*synchronized*/ void recv(byte[] buff,int pos,int len)
      {
        try
          {
             m_pipedOutputStream.write(buff,pos,len);
          }
        catch(Exception e)
          {
            e.printStackTrace();
          }
      }

    public synchronized int available() throws IOException
      {
        return m_pipedInputStream.available();
      }

    public int read() throws IOException
      {
        return m_pipedInputStream.read();
      }

    public int read(byte[] out,int pos,int len) throws IOException
      {
        return m_pipedInputStream.read(out,pos,len);
      }

    protected /*synchronized*/ void closedByPeer()
      {
        m_bIsClosedByPeer=true;
        try{m_pipedOutputStream.close();}catch(Exception e){}
      }

    public /*synchronized*/ void close() throws IOException
      {
        m_bIsClosed=true;
        try{m_pipedOutputStream.close();}catch(Exception e){}
        m_pipedInputStream.close();
      }
  }

class ChannelOutputStream extends OutputStream
  {
    boolean m_bIsClosedByPeer=false;
    boolean m_bIsClosed=false;
    Channel m_channel=null;

    protected ChannelOutputStream(Channel c)
      {
        m_channel=c;
      }

    //OutputStream Methods
    public /*synchronized*/ void write(int i) throws IOException
      {
        if(m_bIsClosedByPeer||m_bIsClosed)
          throw new IOException("Channel closed by peer");
      }

    public /*synchronized*/ void write(byte[] buff,int start,int len) throws IOException
      {
        if(m_bIsClosedByPeer||m_bIsClosed)
          throw new IOException("Channel closed by peer");
        m_channel.m_muxSocket.send(m_channel.m_id,0,buff,(short)len);
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
package anon;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */
import JAPDebug;
class JAPDummyTraffic implements Runnable
	{
		JAPMuxSocket m_MuxSocket=null;
	  volatile boolean m_bRun=false;
		Thread m_threadRunLoop=null;
		private final static long DUMMY_TRAFFIC_INTERVAL=10000; //How long maximum to wait between packets ?

		public JAPDummyTraffic(JAPMuxSocket muxSocket)
			{
				m_MuxSocket=muxSocket;
				m_bRun=false;
				m_threadRunLoop=null;
			}

		public void run()
			{
				while(m_bRun)
					{
						if(System.currentTimeMillis()-m_MuxSocket.getTimeLastPacketSend()>DUMMY_TRAFFIC_INTERVAL)
							{
							  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Sending Dummy!");
								m_MuxSocket.send(12345,JAPAnonService.PROTO_HTTP,null,(short)0); //this is a channel close for a hopefully non existend channel
						  }
						try
							{
								m_threadRunLoop.sleep(10000);
							}
						catch(InterruptedException e)
							{
							}
					}
			}

		public void start()
			{
				m_threadRunLoop=new Thread(this);
				m_bRun=true;
				m_threadRunLoop.start();
			}

		public void stop()
			{
				m_bRun=false;
				m_threadRunLoop.interrupt();
			  try
					{
						m_threadRunLoop.join();
					}
				catch(Exception e)
					{
					}
				m_threadRunLoop=null;
			}
	}
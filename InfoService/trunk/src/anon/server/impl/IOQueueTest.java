package anon.server.impl;

 public class IOQueueTest implements Runnable
    {
      boolean m_bIsConsumer;
      IOQueue m_Queue;
      private static byte[] outBuff;
      public IOQueueTest(boolean bConsumer,IOQueue queue)
        {
          m_bIsConsumer=bConsumer;
          m_Queue=queue;
        }

      public void run()
        {
          try{
          byte[] buff=new byte[1000];
          if(m_bIsConsumer)
            {
              System.out.println("Start read");
              int v=0;
              int t=0;
              for(;;)
              {
              int len=(int)(Math.random()*1000);
              len=m_Queue.read(buff,0,len);
              //System.out.println("Read: "+len);
              for(int i=0;i<len;i++)
                {
                  t=(int)(buff[i]&0x00FF);
                  if(t!=v)
                    System.out.println("Error");
                  v++;
                  if(v>255)
                    v=0;
                }
              Thread.sleep((int)(Math.random()*1));
            }}

          else
            {
              System.out.println("Start write");
              int aktIndex=0;
              for(;;)
                {
                  int len=(int)(Math.random()*1000);
                  m_Queue.write(outBuff,aktIndex,len);
                  aktIndex+=len;
                  aktIndex=aktIndex%256;
                  Thread.sleep((int)(Math.random()*0));
                }
            }
            }
            catch(Exception e)
              {
                e.printStackTrace();
              }
        }

      public static void main(String[] h)
        {
          outBuff=new byte[2560];
          for(int j=0;j<10;j++)
          for(int i=0;i<256;i++)
            outBuff[i+j*256]=(byte)i;
          IOQueue queue=new IOQueue();
          new Thread(new IOQueueTest(true,queue)).start();
          new IOQueueTest(false,queue).run();
        }
    }

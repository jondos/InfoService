import java.security.SecureRandom;

public final class JAPKeyPool /*extends Thread*/ implements Runnable
	{
		private SecureRandom sr;
		private int aktSize;
		private KeyList keys;
		private KeyList pool;
		private KeyList aktKey;
		private int keySize;
		private int poolSize;
		private Object l1;
		private Object l2;
		private boolean runflag;
		
		private final class KeyList
			{
				public byte[] key;
				public KeyList next;
				public KeyList(int ks)
					{
						key=new byte[ks];
						next=null;
					}
			}
		
		public JAPKeyPool(int ps,int keylength)
			{	JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPKeyPool:initializing...");
//				setPriority(Thread.MIN_PRIORITY);
				keySize=keylength;
				poolSize=ps;
				pool=null;
				keys=null;
				aktKey=null;
				l1=new Object();
				l2=new Object();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:initialization finished!");
			}
		
		public void run()
			{	
				sr=new SecureRandom(SecureRandom.getSeed(20));
				KeyList tmpKey;
				pool=new KeyList(keySize);
				for(int i=1;i<poolSize;i++)
					{
						tmpKey=new KeyList(keySize);
						tmpKey.next=pool;
						pool=tmpKey;
					}
				runflag=true;
				while(runflag)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:run() loop");
						if(pool!=null)
							{
								synchronized(this)
									{
										sr.nextBytes(pool.key);
										tmpKey=pool;
										pool=pool.next;
										tmpKey.next=keys;
										keys=tmpKey;
										aktKey=keys;
										synchronized(l2)
										{l2.notify();}
									}
							}
						else
							try
								{
									synchronized(l1)
										{l1.wait();}
								}
							catch(InterruptedException e)
								{
									JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:run() waiting interrupted!");
								}
					}
			}
		
		public void getKey(byte[] key)
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:getKey()");
				if(aktKey==null)
					try	
						{
							synchronized(l2)
							{l2.wait();}
						}
					catch(InterruptedException e)
						{
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:getKey() waiting interrupted!");
						}
				synchronized(this)
					{
						KeyList tmpKey;
						System.arraycopy(aktKey.key,0,key,0,key.length);
						tmpKey=aktKey;
						if(aktKey.next!=null)
							aktKey=aktKey.next;
						else
							aktKey=keys;
						tmpKey.next=pool;
						pool=tmpKey;
					}
				synchronized(l1)
				{l1.notify();}				
			}
	}

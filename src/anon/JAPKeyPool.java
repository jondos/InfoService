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
import java.security.SecureRandom;
import JAPDebug;

final class JAPKeyPool implements Runnable
	{
		private SecureRandom sr;
		private int aktSize;
		//private KeyList keys;
		private KeyList pool;
		private KeyList aktKey;
		private int keySize;
		private int poolSize;
		private Object l1;
		private Object l2;
		private boolean runflag;
		private static JAPKeyPool m_KeyPool=null;
		private Thread m_KeyPoolThread=null;
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
		
		
		private JAPKeyPool(int poolsize,int keylength)
			{	
				JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPKeyPool:initializing...");
				keySize=keylength;
				this.poolSize=poolsize;
				pool=null;
			//	keys=null;
				aktKey=null;
				l1=new Object();
				l2=new Object();
				m_KeyPoolThread=new Thread(this);
				m_KeyPoolThread.setPriority(Thread.MIN_PRIORITY);
				m_KeyPoolThread.start();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:initialization finished!");
			}
		
		public static JAPKeyPool start(int poolsize,int keylength)
			{
				if(m_KeyPool==null)
					m_KeyPool=new JAPKeyPool(poolsize,keylength);
				return m_KeyPool;
			}
		
		public void run()
			{	
				byte[] seed=null;
				try
					{
						seed=SecureRandom.getSeed(20);
						sr=new SecureRandom(seed);
					}
				catch(Throwable e)
					{
						sr=new SecureRandom();
					}
				seed=null;
				KeyList tmpKey;
				pool=new KeyList(keySize);
				for(int i=1;i<poolSize;i++)
					{
						tmpKey=new KeyList(keySize);
						tmpKey.next=pool;
						pool=tmpKey;
					}
				//keys=null;
				aktKey=null;
				runflag=true;
				while(runflag)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:run() loop");
						if(pool!=null)
							{
								synchronized(this)
									{
										sr.nextBytes(pool.key);
										//tmpKey ausketten aus pool
										tmpKey=pool;
										pool=pool.next;
										
										//einketten in keys..
									//	tmpKey.next=keys;
									//	keys=tmpKey;
										
										// aktueller=kopf....
										tmpKey.next=aktKey;
										aktKey=tmpKey;
									synchronized(l2)
											{
												l2.notify();
											}
	
									}
							}
						else
							try
								{
									synchronized(l1)
										{
											l1.wait();
										}
								}
							catch(InterruptedException e)
								{
									JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:run() waiting interrupted!");
								}
					}
			}
		
		public static void getKey(byte[] key)
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:getKey()");
				if(m_KeyPool.aktKey==null)
					try	
						{
							synchronized(m_KeyPool.l2)
							{m_KeyPool.l2.wait();}
						}
					catch(InterruptedException e)
						{
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPKeyPool:getKey() waiting interrupted!");
						}
				synchronized(m_KeyPool)
					{
						KeyList tmpKey;
						System.arraycopy(m_KeyPool.aktKey.key,0,key,0,m_KeyPool.keySize);
						tmpKey=m_KeyPool.aktKey;
						//if(aktKey.next!=null)
							m_KeyPool.aktKey=m_KeyPool.aktKey.next;
						//else
						//	aktKey=keys;
						tmpKey.next=m_KeyPool.pool;
						 							
						m_KeyPool.pool=tmpKey;
					}
				synchronized(m_KeyPool.l1)
				{m_KeyPool.l1.notify();}				
			}
	}

/*
Copyright (c) 2000 - 2004, The JAP-Team
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.net.ConnectException;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import java.io.InterruptedIOException;
import java.lang.Integer;
import java.util.Enumeration;

import java.math.BigInteger;

import java.security.SecureRandom;
import java.security.PublicKey;
import java.security.InvalidKeyException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import logging.Log;
import logging.LogLevel;
import logging.LogType;

import HTTPClient.Codecs;
import anon.ErrorCodes;
import anon.AnonChannel;
import anon.ToManyOpenChannelsException;
import anon.NotConnectedToMixException;
import anon.server.AnonServiceImpl;
import anon.crypto.JAPSignature;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.MixCascade;
import anon.infoservice.ListenerInterface;

public final class MuxSocket implements Runnable
	{
		private SecureRandom m_SecureRandom;
		private Dictionary m_ChannelList;
		private BufferedOutputStream m_outStream;
		private DataInputStream m_inDataStream;
		private byte[] m_MixPacketSend;
		private byte[] m_MixPacketRecv;

		private ProxyConnection m_ioSocket;

		private	byte[] m_arOutBuff;
		private byte[] m_arOutBuff2;
		private byte[] m_arEmpty;
		private ASymCipher[] m_arASymCipher;
		private KeyPool m_KeyPool;
		private int m_iChainLen;
		private volatile boolean m_bRunFlag;
		private boolean m_bIsConnected=false;

		private SymCipher m_cipherIn;
		private SymCipher m_cipherOut;
		private boolean m_bisCrypted;

		private final static int KEY_SIZE=16;
		private final static int DATA_SIZE=992;
		private final static int PAYLOAD_SIZE=989;
		private final static int PACKET_SIZE=998;  //DATA_SIZE+6
		private final static int RSA_SIZE=128;
		private final static short CHANNEL_DATA=0;
		private final static short CHANNEL_CLOSE=1;
		private final static short CHANNEL_OPEN=8;
		private final static short CHANNEL_DUMMY=16;

		private final static int CHANNEL_TYPE_HTTP=0;
		private final static int CHANNEL_TYPE_SOCKS=1;

		private final static int MAX_CHANNELS_PER_CONNECTION=50;

		private Thread threadRunLoop;

		private long m_TimeLastPacketSend=0;
		private DummyTraffic m_DummyTraffic=null;
		private Log m_Log=null;

		private boolean m_bMixProtocolWithTimestamp;
		private int m_iTimestampSize;
		private final static Calendar m_scalendarGMT=Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		private final class ChannelListEntry
			{
				ChannelListEntry(AbstractChannel c)
					{
						channel=c;
						arCipher=null;
						bIsSuspended=false;
					}
				public final AbstractChannel channel;
				public SymCipher[] arCipher;
				public boolean bIsSuspended;
			};

		private MuxSocket(Log log)
			{
				m_Log=log;
				//m_iLastChannelId=0;
				m_arASymCipher=null;
				m_arOutBuff=new byte[DATA_SIZE];
				m_arOutBuff2=new byte[DATA_SIZE];
				m_arEmpty=new byte[DATA_SIZE];
				for(int i=0;i<m_arEmpty.length;i++)
					m_arEmpty[i]=0;
				m_MixPacketSend=new byte[PACKET_SIZE];
				m_MixPacketRecv=new byte[PACKET_SIZE];
				m_cipherIn=new SymCipher();
				m_cipherOut=new SymCipher();
				m_bisCrypted=false;
				threadRunLoop=null;
				m_KeyPool=KeyPool.start(log);
				//m_RunCount=0;
				m_DummyTraffic=null;
				m_TimeLastPacketSend=0;
				m_SecureRandom=new SecureRandom();
				m_bMixProtocolWithTimestamp=false;
				m_iTimestampSize=0;
				//threadgroupChannels=null;
			}

		public static MuxSocket create(Log log)
			{
				/*if(ms_MuxSocket==null)
					ms_MuxSocket=new MuxSocket(log);
				*/
				return new MuxSocket(log);//ms_MuxSocket;
			}

		public void setLogging(Log log)
			{
				m_Log=log;
				if(m_DummyTraffic!=null)
					m_DummyTraffic.setLogging(log);
				if(m_KeyPool!=null)
					{
						m_KeyPool.setLogging(log);
					}
			}

		public /*static*/ boolean isConnected()
			{
				return (/*ms_MuxSocket!=null&&ms_MuxSocket.*/m_bIsConnected);
			}

		/**Enables or Disables DummyTraffic.
		 * @param intervall milliseconds of inactivity after which a dummy is send. Set to '-1' to disable Dummy Traffic.
		 *
		 */
		public void setDummyTraffic(int intervall)
			{
				if(intervall==-1)
					if(/*ms_MuxSocket.*/m_DummyTraffic==null)
						return;
					else
						{
							/*ms_MuxSocket.*/m_DummyTraffic.stop();
							/*ms_MuxSocket.*/m_DummyTraffic=null;
							return;
						}
				if(/*ms_MuxSocket.*/m_DummyTraffic!=null)
					/*ms_MuxSocket.*/m_DummyTraffic.stop();
				/*ms_MuxSocket.*/m_DummyTraffic=new DummyTraffic(this/*ms_MuxSocket*/,intervall,/*ms_MuxSocket.*/m_Log);
				if(isConnected())
					/*ms_MuxSocket.*/m_DummyTraffic.start();
			}

		//2001-02-20(HF)
		public int connectViaFirewall(MixCascade mixCascade, int fwType,String fwHost, int fwPort,String fwUserID,String fwPasswd,
																	boolean bCheckMixCerts,JAPCertificateStore certsTrustedRoots)
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							return ErrorCodes.E_ALREADY_CONNECTED;
						int err=ErrorCodes.E_CONNECT;
					 //try all possible listeners
						m_ioSocket=null;
						for(int l = 0; l < mixCascade.getNumberOfListenerInterfaces(); l++)
							try
								{
									String host = mixCascade.getListenerInterface(l).getIpString();
									if (host==null) {
										host = mixCascade.getListenerInterface(l).getHostName();
									}
									m_ioSocket=new ProxyConnection(m_Log,fwType,fwHost,fwPort,fwUserID,fwPasswd,host,mixCascade.getListenerInterface(l).getPort());
									if (m_ioSocket!=null) {
										break;
									}
								}
							catch(Throwable t)
								{
									m_ioSocket=null;
								}
						try
							{
								m_inDataStream=new DataInputStream(m_ioSocket.getInputStream());
								m_outStream=new BufferedOutputStream(m_ioSocket.getOutputStream(),PACKET_SIZE);
								m_bisCrypted=false;
							//	m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Reading len...");
								int len=m_inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
							//	m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Reading m_iChainLen...");
								m_iChainLen=m_inDataStream.readByte();
								if(m_iChainLen=='<') //assuming XML-Key-Exchange
									{
										byte[] buff=new byte[len];
										buff[0]=(byte)m_iChainLen; //we have already read the beginning '<' !!
										m_inDataStream.readFully(buff,1,len-1);
										err=processXmlKeys(buff,bCheckMixCerts,certsTrustedRoots);
										if(err!=ErrorCodes.E_SUCCESS)
											throw new Exception("Error during Xml-Key Exchange");
									}
								else
									{
										//m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:m_iChainLen="+m_iChainLen);
										m_arASymCipher=new ASymCipher[m_iChainLen];
										for(int i=m_iChainLen-1;i>=0;i--)
											{
												m_arASymCipher[i]=new ASymCipher();
												int tmp=m_inDataStream.readUnsignedShort();
												byte[] buff=new byte[tmp];
												m_inDataStream.readFully(buff);
												BigInteger n=new BigInteger(1,buff);
												tmp=m_inDataStream.readUnsignedShort();
												buff=new byte[tmp];
												m_inDataStream.readFully(buff);
												BigInteger e=new BigInteger(1,buff);
												m_arASymCipher[i].setPublicKey(n,e);
											}
										}
								m_ioSocket.setSoTimeout(0); //Now we have a unlimited timeout...
	//							m_ioSocket.setSoTimeout(1000); //Now we have asecond timeout...
							}
						catch(Exception e)
							{
								m_Log.log(LogLevel.EXCEPTION,LogType.NET,"MuxSocket:Exception(2) during connection: "+e);
								m_arASymCipher=null;
								try{m_inDataStream.close();}catch(Exception e1){}
								try{m_outStream.close();}catch(Exception e1){}
								try{m_ioSocket.close();}catch(Exception e1){}
								m_inDataStream=null;
								m_outStream=null;
								m_ioSocket=null;
								m_bIsConnected=false;
								return err;
							}
						m_bIsConnected=true;
						m_ChannelList=new Hashtable();
						if(m_DummyTraffic!=null)
							m_DummyTraffic.start();
						return ErrorCodes.E_SUCCESS;
					}
			}

		private synchronized void setEnableEncryption(boolean b)
			{
				m_bisCrypted=b;
			}

		private synchronized void setEncryptionKeys(byte[] keys)
			{
				m_cipherIn.setEncryptionKeyAES(keys,0);
				m_cipherOut.setEncryptionKeyAES(keys,16);
			}

		/*Reads the public key from the Mixes and try to initialize the key array*/
		private int processXmlKeys(byte[] buff,boolean bCheckMixCerts, JAPCertificateStore certsTrustedRoots)
			{
				try
					{
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buff));
						Element root=doc.getDocumentElement();
						if(!root.getNodeName().equals("MixCascade"))
							return ErrorCodes.E_UNKNOWN;
						//TODO Check Signautre of whole XML struct
//---
						if (bCheckMixCerts)
							{
								// Signature Check First Mix
								// - check signature
								// - check if certificate is OK
								// - extract public key of X509 certificate
								try
									{
										//this should be moved to JAPSignature....
										Node nodeSig=XMLUtil.getFirstChildByName(root,"Signature");
										Node mix1_x509cert = XMLUtil.getFirstChildByNameUsingDeepSearch(nodeSig,"X509Certificate");
										JAPCertificate cert_1 = JAPCertificate.getInstance(mix1_x509cert);
										PublicKey pk_1 = cert_1.getPublicKey();
										JAPSignature m_sig1 = new JAPSignature();
										try
											{
												m_sig1.initVerify(pk_1);
											}
										catch (InvalidKeyException ex)
											{
												return ErrorCodes.E_INVALID_KEY;
											}
										if (!m_sig1.verifyXML(root))
											return ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
										//ok now the know that the XML was signed with the included Cert
										//but lets check if we do trust this cert...
										// lets look up the certificate list we trust!
										JAPCertificate j = null;
										Enumeration m_certs = certsTrustedRoots.elements();
										while (m_certs.hasMoreElements())
											{
												j = (JAPCertificate) m_certs.nextElement();
												if (j.getIssuer().equals(cert_1.getIssuer()))
													{
														break;
													}
											}
										try
											{
												if(!cert_1.verify(j.getPublicKey()))
													return ErrorCodes.E_INVALID_CERTIFICATE;
											}
										catch (Exception e)
											{
												return ErrorCodes.E_INVALID_CERTIFICATE;
											}
									}
								catch (Exception e)
									{
										return ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
									}
							}//end if check Signatures
//---
						Element elemMixProtocolVersion=(Element)root.getElementsByTagName("MixProtocolVersion").item(0);
						if(elemMixProtocolVersion==null)
							return ErrorCodes.E_MIX_PROTOCOL_NOT_SUPPORTED;
						Node n=elemMixProtocolVersion.getFirstChild();
						if(n==null||n.getNodeType()!=n.TEXT_NODE)
							return ErrorCodes.E_MIX_PROTOCOL_NOT_SUPPORTED;
						String strProtocolVersion=n.getNodeValue().trim();
						if(strProtocolVersion.equals("0.2"))
							{
								m_bMixProtocolWithTimestamp=false;
								m_iTimestampSize=0;
							}
						else if(strProtocolVersion.equals("0.3"))
							{
								m_bMixProtocolWithTimestamp=true;
								m_iTimestampSize=2;
							}
						else
							return ErrorCodes.E_MIX_PROTOCOL_NOT_SUPPORTED;
						Element elemMixes=(Element)root.getElementsByTagName("Mixes").item(0);
						m_iChainLen=Integer.parseInt(elemMixes.getAttribute("count"));
						m_arASymCipher=new ASymCipher[m_iChainLen];
						int i=0;
						Node child=elemMixes.getFirstChild();
						boolean bIsFirst=true;
						while(child!=null)
							{
								if(child.getNodeName().equals("Mix"))
									{
										//TODO: Check signatures for Mix 2 .. n (we skip the first Mix because
										//this key is already signed to the Signature below the whole MixCascade struct
//---
										if (bCheckMixCerts&&!bIsFirst)
											{
												try
													{
														//this should be moved to JAPSignature....
														Node nodeSig=XMLUtil.getFirstChildByName(child,"Signature");
														Node mixn_x509cert = XMLUtil.getFirstChildByNameUsingDeepSearch(nodeSig,"X509Certificate");
														JAPCertificate cert_n = JAPCertificate.getInstance(mixn_x509cert);
														PublicKey pk_n = cert_n.getPublicKey();
														JAPSignature m_sign = new JAPSignature();
														try
															{
																m_sign.initVerify(pk_n);
															}
														catch (InvalidKeyException ex)
															{
																return ErrorCodes.E_INVALID_KEY;
															}
														if (!m_sign.verifyXML(child))
															return ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;
														//ok now the know that the XML was signed with the included Cert
														//but lets check if we do trust this cert...
														// lets look up the certificate list we trust!
														JAPCertificate j = null;
														Enumeration m_certs = certsTrustedRoots.elements();
														while (m_certs.hasMoreElements())
															{
																j = (JAPCertificate) m_certs.nextElement();
																if (j.getIssuer().equals(cert_n.getIssuer()))
																	{
																		break;
																	}
															}
														try
															{
																if(!cert_n.verify(j.getPublicKey()))
																	return ErrorCodes.E_INVALID_CERTIFICATE;
															}
														catch (Exception e)
															{
																return ErrorCodes.E_INVALID_CERTIFICATE;
															}
													}
												catch (Exception e)
													{
														return ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;
													}
/*
												try
													{
														Node mixn_x509cert = XMLUtil.getFirstChildByNameUsingDeepSearch(child, "X509Certificate");
														if (mixn_x509cert != null)
															{
																JAPCertificate cert_n = JAPCertificate.getInstance(mixn_x509cert);

																// the issuer does not match the subject
																// assumption: the certificate was certified by jap team root certificate
																if (!(cert_n.getIssuer().equals(cert_n.getSubject())))
																	{
																		JAPCertificate j2 = null;
																		Enumeration m_certs2 = JAPModel.getCertificateStore().elements();
																		while (m_certs2.hasMoreElements())
																			{
																				j2 = (JAPCertificate) m_certs2.nextElement();
																				if (j2.getIssuer().equals(cert_n.getIssuer()))
																					{
																						break;
																					}
																			}
																		try
																			{
																				cert_n.verify(j2.getPublicKey());
																			}
																		catch (Exception e)
																			{
																				e.printStackTrace();
																				return ErrorCodes.E_INVALID_CERTIFICATE;
																			}
																	}
																PublicKey pk_n = cert_n.getPublicKey();
																if (!(pk_n instanceof PublicKey))
																	return ErrorCodes.E_INVALID_KEY;
																JAPSignature m_sign = new JAPSignature();
																try
																	{
																		m_sign.initVerify(pk_n);
																	}
																catch (InvalidKeyException ex)
																	{
																		return ErrorCodes.E_INVALID_KEY;
																	}
																if (!m_sign.verifyXML(child))
																	return ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;
															}
													} catch (Exception e) { e.printStackTrace(); }
											*/}
//---
										bIsFirst=false;
										m_arASymCipher[i]=new ASymCipher();
										if(m_arASymCipher[i++].setPublicKey((Element)child)!=ErrorCodes.E_SUCCESS)
											return ErrorCodes.E_UNKNOWN;
									 }
								child=child.getNextSibling();
							}
						//Sending symmetric keys for Mux encryption...
						System.arraycopy("KEYPACKET".getBytes(),0,m_MixPacketSend,6,9);
						byte[] tmpBuff=new byte[32];
						m_KeyPool.getKey(tmpBuff,0);
						m_KeyPool.getKey(tmpBuff,16);
						System.arraycopy(tmpBuff,0,m_MixPacketSend,15,32);
						m_arASymCipher[0].encrypt(m_MixPacketSend,6,m_MixPacketSend,6);
						sendMixPacket();
						setEncryptionKeys(tmpBuff);
						setEnableEncryption(true);
						return ErrorCodes.E_SUCCESS;
					}
				catch(Exception e)
					{
						return ErrorCodes.E_UNKNOWN;
					}
			}



	 public Channel newChannel(int type) throws ConnectException
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							{
								if(m_ChannelList.size()>MAX_CHANNELS_PER_CONNECTION)
									throw new ToManyOpenChannelsException();
								try
									{
										int channelId=m_SecureRandom.nextInt();
										while(m_ChannelList.get(new Integer(channelId))!=null)
											channelId=m_SecureRandom.nextInt();
										Channel c=new Channel(this,channelId,type);
										m_ChannelList.put(new Integer(channelId),new ChannelListEntry(c));

										return c;
									}
								catch(Exception e)
									{
										throw new ConnectException("Error trying open a new channel!");
									}
							}
						else
							throw new NotConnectedToMixException("Lost connection to mix or not connected to a mix!");
					}
			}

		public int close(int channel_id)
			{
				synchronized(this)
					{
						m_ChannelList.remove(new Integer(channel_id));
						send(channel_id,0,null,(short)0);
						return 0;
					}
			}

		protected synchronized void sendDummy()
			{
				synchronized(this)
					{
						try
							{
								m_TimeLastPacketSend=System.currentTimeMillis();
								//First the Channel in Network byte order
								int channel=m_SecureRandom.nextInt();
								m_MixPacketSend[0]=(byte)((channel>>24)&0xFF);
								m_MixPacketSend[1]=(byte)((channel>>16)&0xFF);
								m_MixPacketSend[2]=(byte)((channel>>8)&0xFF);
								m_MixPacketSend[3]=(byte)((channel)&0xFF);
								//Then the flags...
								m_MixPacketSend[4]=(byte)((CHANNEL_DUMMY>>8)&0xFF);
								m_MixPacketSend[5]=(byte)((CHANNEL_DUMMY)&0xFF);
								//and then the payload (data)
								System.arraycopy(m_arOutBuff,0,m_MixPacketSend,6,DATA_SIZE);
								//Send it...
								sendMixPacket();
							}
						catch(Exception e)
							{
								m_Log.log(LogLevel.ERR,LogType.NET,"MuxSocket:sendDummy() Exception!");
							}
					}
			}

		public int startService()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;
							threadRunLoop=new Thread(this,"JAP - MuxSocket");
							threadRunLoop.setPriority(Thread.MAX_PRIORITY);
							threadRunLoop.start();
					}
				return ErrorCodes.E_SUCCESS;
			}

		public void stopService()
			{
				synchronized(this)
					{
						//m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:stopService()");
						//m_RunCount--;
						//if(m_RunCount==0)
						close();
						//return m_RunCount;
					}
			}

		private int close()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;
						if(m_DummyTraffic!=null)
							{
								m_DummyTraffic.stop();
								m_DummyTraffic=null;
							}
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:close() Closing MuxSocket...");
						m_bRunFlag=false;
						try{m_inDataStream.close();}catch(Exception e1){}
						try{threadRunLoop.interrupt();}catch(Exception e7){}
						try
							{
								threadRunLoop.join(1000);
							}
						catch(Exception e)
							{
								//e.printStackTrace();
							}
						if(threadRunLoop.isAlive())
							{
									m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:close() Hm...MuxSocket is still alive...");
									threadRunLoop.stop();
									runStoped();
							}
						threadRunLoop=null;
						m_bisCrypted=false;
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:close() MuxSocket closed!");
						return 0;
					}
			}



		public void run()
			{
				m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:run()");
				byte[] buff=new byte[DATA_SIZE];
				int flags=0;
				int channel=0;
				int len=0;
				m_bRunFlag=true;
				while(m_bRunFlag)
					{
						try
							{
								m_inDataStream.readFully(m_MixPacketRecv);
								if(m_bisCrypted)
									m_cipherIn.encryptAES(m_MixPacketRecv,0,m_MixPacketRecv,0,16);
								channel=(m_MixPacketRecv[0]<<24);
								channel|=((m_MixPacketRecv[1]<<16)&0x00FF0000);
								channel|=((m_MixPacketRecv[2]<<8)&0x0000FF00);
								channel|=(m_MixPacketRecv[3]&0xFF);
								//m_inDataStream.readInt();
								flags=	((m_MixPacketRecv[4]<<8)&0x0000FF00)|
												((m_MixPacketRecv[5])&0xFF);
								//m_inDataStream.readShort();
								//m_inDataStream.readFully(buff);
								System.arraycopy(m_MixPacketRecv,6,buff,0,DATA_SIZE);
								m_TimeLastPacketSend=System.currentTimeMillis();
							}
						catch(Exception e)
							{
								m_Log.log(LogLevel.ERR,LogType.NET,"JAPMuxSocket:run() Exception while receiving!");
								break;
							}
						if(flags==CHANNEL_DUMMY) //Dummies go to /dev/null ...
							{
								m_Log.log(LogLevel.DEBUG,LogType.NET,"MuxSocket:run() Received a Dummy...");
								continue;
							}
						ChannelListEntry tmpEntry=(ChannelListEntry)m_ChannelList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(flags==CHANNEL_CLOSE)
									{
										m_ChannelList.remove(new Integer(channel));
										tmpEntry.channel.closedByPeer();
									}
								else if(flags==CHANNEL_DATA)
									{
										for(int i=0;i<m_iChainLen;i++)
												tmpEntry.arCipher[i].encryptAES2(buff);
										len=(buff[0]<<8)|(buff[1]&0xFF);
										len&=0x0000FFFF;
										if(len<0||len>DATA_SIZE)
											m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Receveived MuxPacket with invalid data size: "+Integer.toString(len));
										else
										{
											try
												{
													tmpEntry.channel.recv(buff,3,len);
												}
											catch(Exception e)
												{
													m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Fehler bei write to channel..."+e.toString());
												}
										}
									}
						/*		else if(flags==CHANNEL_SUSPEND)
									{
										tmpEntry.bIsSuspended=true;
										m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Suspending Channel: "+Integer.toString(channel));
										}
								else if(flags==CHANNEL_RESUME)
									{
										tmpEntry.bIsSuspended=false;
										m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:Resuming Channel: "+Integer.toString(channel));
									}*/
							}
					}
				runStoped();
				m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:MuxSocket thread run exited...");
			}

		private void runStoped()
			{
				synchronized(this)
					{
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:runStoped()");
						m_bRunFlag=false;
						if(m_DummyTraffic!=null)
							{
								m_DummyTraffic.stop();
								m_DummyTraffic=null;
							}
						if(m_ChannelList!=null)
							{
								Enumeration e=m_ChannelList.elements();
								while(e.hasMoreElements())
									{
										ChannelListEntry entry=(ChannelListEntry)e.nextElement();
										entry.channel.closedByPeer();
									}
							}
						m_ChannelList=null;
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:MuxSocket all channels closed...");
						m_bIsConnected=false;
						try{m_inDataStream.close();}catch(Exception e1){}
						try{m_outStream.close();}catch(Exception e2){}
						try{m_ioSocket.close();}catch(Exception e3){}
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:MuxSocket socket closed...");
						m_inDataStream=null;
						m_outStream=null;
						m_ioSocket=null;
						m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket:All done..");
					}
			}

		private void sendMixPacket() throws Exception
			{
				if(m_bisCrypted)
					m_cipherOut.encryptAES(m_MixPacketSend,0,m_MixPacketSend,0,16);
				m_outStream.write(m_MixPacketSend);
				m_outStream.flush();
			}

		public synchronized int send(int channel,int type,byte[] buff,short len)
			{
				try
					{
						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;

						short channelMode=CHANNEL_DATA;
						m_TimeLastPacketSend=System.currentTimeMillis();
						if(buff==null&&len==0)
							{
								//First the Channel in Network byte order
								m_MixPacketSend[0]=(byte)((channel>>24)&0xFF);
								m_MixPacketSend[1]=(byte)((channel>>16)&0xFF);
								m_MixPacketSend[2]=(byte)((channel>>8)&0xFF);
								m_MixPacketSend[3]=(byte)((channel)&0xFF);
								//Then the flags...
								m_MixPacketSend[4]=(byte)((CHANNEL_CLOSE>>8)&0xFF);
								m_MixPacketSend[5]=(byte)((CHANNEL_CLOSE)&0xFF);
								//and then the payload (data)
								System.arraycopy(m_arEmpty,0,m_MixPacketSend,6,DATA_SIZE);
								//Send it...
								sendMixPacket();
								return 0;
							}
						if(buff==null)
							return -1;
						if(len==0)
							return 0;
						ChannelListEntry entry=(ChannelListEntry)m_ChannelList.get(new Integer(channel));
						if(entry==null)
							return ErrorCodes.E_UNKNOWN;
						System.arraycopy(m_arEmpty,0,m_arOutBuff,0,m_arOutBuff.length);
						if(entry.arCipher==null)
							{
								int size=PAYLOAD_SIZE-KEY_SIZE-m_iTimestampSize;
								entry.arCipher=new SymCipher[m_iChainLen];

								//Last Mix
								entry.arCipher[m_iChainLen-1]=new SymCipher();
								m_KeyPool.getKey(m_arOutBuff);
								m_arOutBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)

								int timestamp=getCurrentTimestamp();
								if(m_bMixProtocolWithTimestamp)
									{
										m_arOutBuff[KEY_SIZE]=(byte)(timestamp>>8);
										m_arOutBuff[KEY_SIZE+1]=(byte)(timestamp%256);
									}
								m_arOutBuff[KEY_SIZE+m_iTimestampSize]=(byte)(len>>8);
								m_arOutBuff[KEY_SIZE+m_iTimestampSize+1]=(byte)(len%256);
								if(type==AnonChannel.SOCKS)
									m_arOutBuff[KEY_SIZE+m_iTimestampSize+2]=CHANNEL_TYPE_SOCKS;
								else
									m_arOutBuff[KEY_SIZE+m_iTimestampSize+2]=CHANNEL_TYPE_HTTP;

								System.arraycopy(buff,0,m_arOutBuff,KEY_SIZE+m_iTimestampSize+3,len);

								entry.arCipher[m_iChainLen-1].setEncryptionKeyAES(m_arOutBuff);
//								m_arASymCipher[m_iChainLen-1].encrypt(outBuff,0,buff,0);
//								entry.arCipher[m_iChainLen-1].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
								m_arASymCipher[m_iChainLen-1].encrypt(m_arOutBuff,0,m_arOutBuff2,0);
								entry.arCipher[m_iChainLen-1].encryptAES(m_arOutBuff,RSA_SIZE,m_arOutBuff2,RSA_SIZE,DATA_SIZE-RSA_SIZE);
								size-=(KEY_SIZE+m_iTimestampSize);
								for(int i=m_iChainLen-2;i>=0;i--)
									{
										entry.arCipher[i]=new SymCipher();
										m_KeyPool.getKey(m_arOutBuff);
										m_arOutBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
										if(m_bMixProtocolWithTimestamp)
											{
												m_arOutBuff[KEY_SIZE]=(byte)(timestamp>>8);
												m_arOutBuff[KEY_SIZE+1]=(byte)(timestamp%256);
											}
										entry.arCipher[i].setEncryptionKeyAES(m_arOutBuff);
										System.arraycopy(m_arOutBuff2,0,m_arOutBuff,KEY_SIZE+m_iTimestampSize,size);
										m_arASymCipher[i].encrypt(m_arOutBuff,0,m_arOutBuff2,0);
										entry.arCipher[i].encryptAES(m_arOutBuff,RSA_SIZE,m_arOutBuff2,RSA_SIZE,DATA_SIZE-RSA_SIZE);
										size-=(KEY_SIZE+m_iTimestampSize);
									}
								channelMode=CHANNEL_OPEN;
							}
						else
							{
								System.arraycopy(buff,0,m_arOutBuff,3,len);
								m_arOutBuff[2]=0;
								m_arOutBuff[0]=(byte)(len>>8);
								m_arOutBuff[1]=(byte)(len%256);
								for(int i=m_iChainLen-1;i>0;i--)
									entry.arCipher[i].encryptAES(m_arOutBuff); //something throws a null pointer....
								entry.arCipher[0].encryptAES(m_arOutBuff,0,m_arOutBuff2,0,DATA_SIZE); //something throws a null pointer....
							}
						//First the Channel in Network byte order
						m_MixPacketSend[0]=(byte)((channel>>24)&0xFF);
						m_MixPacketSend[1]=(byte)((channel>>16)&0xFF);
						m_MixPacketSend[2]=(byte)((channel>>8)&0xFF);
						m_MixPacketSend[3]=(byte)((channel)&0xFF);
						//Then the flags...
						m_MixPacketSend[4]=(byte)((channelMode>>8)&0xFF);
						m_MixPacketSend[5]=(byte)((channelMode)&0xFF);
						//and then the payload (data)
						System.arraycopy(m_arOutBuff2,0,m_MixPacketSend,6,DATA_SIZE);
						//m_outDataStream.writeInt(channel);
						//m_outDataStream.writeShort(channelMode);
						//m_outDataStream.write(outBuff2,0,DATA_SIZE);
						//Send it...
						sendMixPacket();
						//JAPAnonService.increaseNrOfBytes(len);
						//if(entry!=null&&entry.bIsSuspended)
						//	return E_CHANNEL_SUSPENDED;
					}
				catch(Exception e)
					{
						e.printStackTrace();
						m_Log.log(LogLevel.ERR,LogType.NET,"JAPMuxSocket:send() Exception (should never be here...)!: "+e.getMessage());
						return ErrorCodes.E_UNKNOWN;
					}
				return ErrorCodes.E_SUCCESS;
			}


		public long getTimeLastPacketSend()
			{
				return m_TimeLastPacketSend;
			}

		private int getCurrentTimestamp()
			{
				// Assume 366 days per year to be on the safe side with leap years.
				long seconds_per_year = 60 * 60 * 24 * 366;
				long now=System.currentTimeMillis();
				m_scalendarGMT.setTime(new Date());

				int aktYear=m_scalendarGMT.get(Calendar.YEAR);
				m_scalendarGMT.clear();
				m_scalendarGMT.set(aktYear,0,0,1,0,0);
				long diff=(now-m_scalendarGMT.getTime().getTime())/1000;

				// timestamp = (millis_passed_in_this_year / millis_per_year) * 2^16
				// That is 0x0000 on January 1, 0:00; 0x0001 on January 1, 0:10; 0xFFFF on December 31, 23:59 (leap year)
				return (int) ((((double) (diff)) / ((double) seconds_per_year)) * 0xFFFF);

			}
	}

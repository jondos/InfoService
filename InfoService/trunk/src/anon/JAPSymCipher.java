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
import Rijndael.Rijndael_Algorithm;
public final class JAPSymCipher
	{
	/** Start LOGI-CRYPT
	 	
			
	//		BlowfishKey bfEnc;//,bfDec;
	//		public JAPSymCipher()
//				{
//					bfEnc=null;
//				}
			
			public int setEncryptionKeyL(byte[] key)
				{
					bfEnc=new BlowfishKey(key);
					return 0;
				}
			
			public int setDecryptionKeyL(byte[] key)
				{
					bfDec=new BlowfishKey(key);
					return 0;
				}
			
			public int encryptL(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfEnc.encrypt(buff,i,buff,i);
				
					return 0;
				}
			
			public int encryptL(byte[] from,int ifrom,byte[] to,int ito,int len)
				{
					for(int i=0;i<len;i+=8)
						bfEnc.encrypt(from,i+ifrom,to,i+ito);
					return 0;
				}
/*
			public int decryptL(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfDec.decrypt(buff,i,buff,i);
					return 0;
				}
	*///	* END LOGICYRPT
	//	*/
	
 /** Start Cryptix...
 	
			private Cipher cipherEnc;
//			private Cipher cipherDec;
			public JAPSymCipher()
				{
					try
						{
							cipherEnc=Cipher.getInstance("Blowfish/ECB/None");
	//						cipherDec=Cipher.getInstance("Blowfish/ECB/None");
						}
					catch(Exception e)
						{
							e.printStackTrace();
						}
				}
			
			public int setEncryptionKey(byte[] key)
				{
					SecretKeySpec k=new SecretKeySpec(key,"Blowfish");
					try
						{
							cipherEnc.init(Cipher.ENCRYPT_MODE,k);
						}
					catch(Exception e)
						{
							e.printStackTrace();
						}
					return 0;
				}
			
			public int setDecryptionKey(byte[] key)
				{
					SecretKeySpec k=new SecretKeySpec(key,"Blowfish");
					try
						{
							cipherDec.init(Cipher.DECRYPT_MODE,k);
						}
					catch(Exception e)
						{
							e.printStackTrace();
						}
					return 0;
				}
		
			public int encrypt(byte[] buff)
				{
					try
						{
							cipherEnc.doFinal(buff,0,buff.length,buff,0);
						}
					catch(Exception e)
						{
							JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"encrypt(buff)"+e.toString());
							e.printStackTrace();
						}				
					return 0;
				}
			
			public int encrypt(byte[] from,int ifrom,byte[] to,int ito,int len)
				{
					try
						{
							cipherEnc.update(from,ifrom,len,to,ito);
							//cipherEnc.doFinal();
							//There is a bug in cryptix so that
							//cipherEnc.doFinal(from,ifrom,8,to,ito);
							//wouldn't work.
						}
					catch(Exception e)
						{
							JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"encrypt(from,ifrom,to,ito,len)"+e.toString());
						}
					return 0;
				}

			public int decrypt(byte[] buff)
				{
					try
						{
							cipherDec.doFinal(buff,0,buff.length,buff,0);
						}
					catch(Exception e)
						{
							JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"decrypt(buff)"+e.toString());
						}
					return 0;
				}
//			END CRYPTIX*/

///*Start AES
			Object oKey;
			byte[] iv=null;
			
			public JAPSymCipher()
				{
					oKey=null;
					iv=new byte[16];
					for(int i=0;i<16;i++)
						iv[i]=0;
				}
			
			public int setEncryptionKeyAES(byte[] key)
				{
					try
						{
							oKey=Rijndael_Algorithm.makeKey(key);
							return 0;
						}
					catch(Exception e)
						{
							return -1;
						}
				}
			
			public int encryptAES(byte[] buff)
				{
					int i=0;
					int len=buff.length;
					while(i<len-15)
						{
							Rijndael_Algorithm.blockEncrypt(iv,iv,oKey);
							buff[i++]^=iv[0];
							buff[i++]^=iv[1];
							buff[i++]^=iv[2];
							buff[i++]^=iv[3];
							buff[i++]^=iv[4];
							buff[i++]^=iv[5];
							buff[i++]^=iv[6];
							buff[i++]^=iv[7];
							buff[i++]^=iv[8];
							buff[i++]^=iv[9];
							buff[i++]^=iv[10];
							buff[i++]^=iv[11];
							buff[i++]^=iv[12];
							buff[i++]^=iv[13];
							buff[i++]^=iv[14];
							buff[i++]^=iv[15];
						}
					if(i<len)
						{
							Rijndael_Algorithm.blockEncrypt(iv,iv,oKey);
							len-=i;
							for(int k=0;k<len;k++)
								buff[i++]^=iv[k];
						}
					return 0;
				}
			
			public int encryptAES(byte[] from,int ifrom,byte[] to,int ito,int len)
				{
					len=ifrom+len;
					while(ifrom<len-15)
						{
							Rijndael_Algorithm.blockEncrypt(iv,iv,oKey);
							to[ito++]=(byte)(from[ifrom++]^iv[0]);
							to[ito++]=(byte)(from[ifrom++]^iv[1]);
							to[ito++]=(byte)(from[ifrom++]^iv[2]);
							to[ito++]=(byte)(from[ifrom++]^iv[3]);
							to[ito++]=(byte)(from[ifrom++]^iv[4]);
							to[ito++]=(byte)(from[ifrom++]^iv[5]);
							to[ito++]=(byte)(from[ifrom++]^iv[6]);
							to[ito++]=(byte)(from[ifrom++]^iv[7]);
							to[ito++]=(byte)(from[ifrom++]^iv[8]);
							to[ito++]=(byte)(from[ifrom++]^iv[9]);
							to[ito++]=(byte)(from[ifrom++]^iv[10]);
							to[ito++]=(byte)(from[ifrom++]^iv[11]);
							to[ito++]=(byte)(from[ifrom++]^iv[12]);
							to[ito++]=(byte)(from[ifrom++]^iv[13]);
							to[ito++]=(byte)(from[ifrom++]^iv[14]);
							to[ito++]=(byte)(from[ifrom++]^iv[15]);
						}
					if(ifrom<len)
						{
							Rijndael_Algorithm.blockEncrypt(iv,iv,oKey);
							len-=ifrom;
							for(int k=0;k<len;k++)
								to[ito++]=(byte)(from[ifrom++]^iv[k]);
						}
					return 0;
				}
			
			
//	END AES		*/

}

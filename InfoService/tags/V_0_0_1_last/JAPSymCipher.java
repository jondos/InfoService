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
import org.logi.crypto.keys.BlowfishKey;
//import javax.crypto.Cipher;
//import cryptix.jce.provider.key.RawSecretKey;

public final class JAPSymCipher
	{
		
		//	private Cipher cipherEnc;
		//	private Cipher cipherDec;
			BlowfishKey bfEnc,bfDec;
			public JAPSymCipher()
				{
					bfEnc=bfDec=null;
			//	try{cipherEnc=Cipher.getInstance("Blowfish/ECB/None");
			//			cipherDec=Cipher.getInstance("Blowfish/ECB/None");}
			//	catch(Exception e)
			//	{
			//		e.printStackTrace();
			//	}
				}
			
			public int setEncryptionKey(byte[] key)
				{
			//		RawSecretKey k=new RawSecretKey("Blowfish",key);
		//			try{cipherEnc.init(Cipher.ENCRYPT_MODE,k);}
	//				catch(Exception e){e.printStackTrace();}
					bfEnc=new BlowfishKey(key);
					return 0;
				}
			
			public int setDecryptionKey(byte[] key)
				{
		//			RawSecretKey k=new RawSecretKey("Blowfish",key);
	//				try{cipherEnc.init(Cipher.ENCRYPT_MODE,k);}
//					catch(Exception e){e.printStackTrace();}
					bfDec=new BlowfishKey(key);
					return 0;
				}
			
			public int encrypt(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfEnc.encrypt(buff,i,buff,i);
	//			try{cipherEnc.doFinal(buff);}
		//			catch(Exception e){e.printStackTrace();}
				
					return 0;
				}
			
			public int encrypt(byte[] from,int ifrom,byte[] to,int ito,int len)
				{
					for(int i=0;i<len;i+=8)
						bfEnc.encrypt(from,i+ifrom,to,i+ito);
			/*	try{cipherEnc.doFinal(from,ifrom,len,to,ito);}
					catch(Exception e){
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"e1");
						e.printStackTrace();}
				*/	return 0;
				}

			public int decrypt(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfDec.decrypt(buff,i,buff,i);
				//try{cipherDec.doFinal(buff);}
					//catch(Exception e){e.printStackTrace();}
					return 0;
				}
			
	}

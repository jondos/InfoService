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
						System.out.println("e1");
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

import org.logi.crypto.keys.BlowfishKey;

public class JASymCipher
	{
			private BlowfishKey bfEnc,bfDec;
			public JASymCipher()
				{
					bfEnc=bfDec=null;
				}
			
			public int setEncryptionKey(byte[] key)
				{
					bfEnc=new BlowfishKey(key);
					return 0;
				}
			
			public int setDecryptionKey(byte[] key)
				{
					bfDec=new BlowfishKey(key);
					return 0;
				}
			
			public int encrypt(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfEnc.encrypt(buff,i,buff,i);
					return 0;
				}
			
			public int decrypt(byte[] buff)
				{
					for(int i=0;i<buff.length;i+=8)
						bfDec.decrypt(buff,i,buff,i);
					return 0;
				}
			
	}

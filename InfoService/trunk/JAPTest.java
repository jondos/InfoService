import java.security.SecureRandom;
import java.io.FileOutputStream;
final public class JAPTest
{
	public static void main(String argc[])
		{
			int MAX=128000;
			JAPASymCipher oRSA=new JAPASymCipher();
			byte[] buff=new byte[MAX];
			byte[] out=new byte[MAX];
			System.out.println("Init Random..");
			SecureRandom sr=new SecureRandom(SecureRandom.getSeed(20));
			System.out.println("Filling Buff..");
			sr.nextBytes(buff);
			System.out.println("Encrypting..");
			for(int i=0;i<MAX;i+=128)
				{
					buff[i]=(byte)(buff[i]&0x7F);
					oRSA.encrypt(buff,i,out,i);
				}
			System.out.println("done..");
			try{
			FileOutputStream io=new FileOutputStream("plain.bytes");
			io.write(buff);
			io.flush();
			io.close();
			io=new FileOutputStream("crypt.bytes");
			io.write(out);
			io.flush();
			io.close();}
			catch(Exception z)
			{z.printStackTrace();}
			System.exit(0);
		}
}

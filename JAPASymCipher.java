import java.math.BigInteger;

final public class JAPASymCipher
{
	private BigInteger n;
	private BigInteger e;
	private byte[] tmpP;
	
	public JAPASymCipher()
		{
			n=new BigInteger("146045156752988119086694783791784827226235382817403930968569889520448117142515762490154404168568789906602128114569640745056455078919081535135223786488790643345745133490238858425068609186364886282528002310113020992003131292706048279603244985126945363695371250073851319256901415103802627246986865697725280735339");
			e=new BigInteger("65537");
			tmpP=new byte[128];
		}
	
	public int encrypt(byte[] from,int ifrom,byte[] to,int ito)
		{
			BigInteger P=null;
			if(from.length==128)
				P = new BigInteger(1,from);
			else
				{
					System.arraycopy(from,ifrom,tmpP,0,128);
					P = new BigInteger(1,tmpP);					
				}
			BigInteger C = P.modPow(e,n);
			byte[] r=C.toByteArray();
			if(r.length==128)
				System.arraycopy(r,0,to,ito,128);
			else if(r.length==129)
				System.arraycopy(r,1,to,ito,128);
			else
				{
					for(int k=0;k<128-r.length;k++)
						to[ito+k]=0;
					System.arraycopy(r,0,to,ito+128-r.length,r.length);
				}
			return 128;
		}

}

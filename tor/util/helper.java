/*
 * Created on Mar 25, 2004
 *
 */
package tor.util;

/**
 * @author stefan
 *
 *some usefull utilities
 */
public class helper {
	/**
	 * Concatenates two byte arrays
	 * @param b1
	 * @param b2
	 * @return
	 */	
	public static byte[]  conc(byte[] b1,byte[] b2)
	{
		if(b1.length==0)
		{
			return b2;
		}
		byte[] b= new byte[b1.length+b2.length];
		for(int i=0;i<b1.length;i++)
		{
			b[i]=b1[i];
		}
		if(b2.length>0)
		{
			for(int i=0;i<b2.length;i++)
			{
				b[i+b1.length]=b2[i];
			}
		}
		return b;
	}
	
	/**
	 * Converts a long to a array of bytes
	 * @param l
	 * @param length
	 * @return
	 */
	public static byte[] inttobyte(long l,int length)
	{
		byte[] b=new byte[length];
		for(int i=0;i<length;i++)
		{
			b[length-i-1]=(byte)( ( l & (0xff << (i*8)) ) >> (i*8));
		}
		return b;
	}
	
	/**
	 * copy some bytes from a array of bytes
	 * @param bytes array of bytes
	 * @param index startposition
	 * @param length length
	 * @return copied bytes
	 */
	public static byte[] copybytes(byte[] bytes,int index,int length)
	{
		byte[] b = new byte[length];
		for(int i=0;i<length;i++)
		{
			b[i] = bytes[index+i];
		}
		return b;
	}

}

/*
 * Created on Mar 25, 2004
 *
 */
package anon.tor.util;
import java.util.StringTokenizer;
/**
 * @author stefan
 *
 *some usefull utilities
 */
public class helper {


	public static byte[]  conc(byte[] b1,byte[] b2)
	{
		return conc(b1,b2,b2.length);
	}

	/**
	 * Concatenates two byte arrays
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static byte[]  conc(byte[] b1,byte[] b2,int b2_len)
	{
		if(b1==null||b1.length==0)
		{
			return copybytes(b2,0,b2_len);
		}
		byte[] b= new byte[b1.length+b2_len];
		for(int i=0;i<b1.length;i++)
		{
			b[i]=b1[i];
		}
		if(b2.length>0)
		{
			for(int i=0;i<b2_len;i++)
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
	public static boolean isIPAddress(String addr)
	{
		StringTokenizer st = new StringTokenizer(addr, ".");
		int i = 0;
		int c;
		while (st.hasMoreTokens())
		{
			String s = st.nextToken();
			try
			{
				c = Integer.parseInt(s);
			}
			catch (Exception e)
			{
				return false;
			}
			if (c < 0 || c > 255)
			{
				return false;
			}
			i++;
			if (i > 4)
			{
				return false;
			}
		}
		return true;
	}

}

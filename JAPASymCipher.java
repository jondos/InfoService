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
import java.math.BigInteger;

final public class JAPASymCipher
{
	private BigInteger n;
	private BigInteger e;
	private byte[] tmpP;
	
	public JAPASymCipher()
		{
			n=null;//new BigInteger("146045156752988119086694783791784827226235382817403930968569889520448117142515762490154404168568789906602128114569640745056455078919081535135223786488790643345745133490238858425068609186364886282528002310113020992003131292706048279603244985126945363695371250073851319256901415103802627246986865697725280735339");
			e=null;//new BigInteger("65537");
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
	
	public int setPublicKey(BigInteger modulus,BigInteger exponent)
		{
			n=modulus;
			e=exponent;
			return 0;
		}

}

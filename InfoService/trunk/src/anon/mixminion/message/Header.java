/*
 Copyright (c) 2004, The JAP-Team
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

package anon.mixminion.message;

import anon.mixminion.mmrdescription.MMRList;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.crypto.MyAES;
import anon.crypto.MyRSA;
import anon.crypto.MyRSAPublicKey;

import java.security.SecureRandom;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.ByteArrayUtil;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author Stefan Rönisch
 */
public class Header {
	
	//Constants
	private int HEADER_LEN = 2048;
	private int OAEP_OVERHEAD = 42;
	private int MIN_SUBHEADER_LEN = 42;
	private int PK_ENC_LEN = 256;
	private int PK_OVERHEAD_LEN = 42;
	private int PK_MAX_DATA_LEN = 214;
	private int HASH_LEN=20;
	private int MIN_SH = 42;
	byte[] VERSION_MAJOR = {0x00,0x03}; //Version Minor, Version Major
	
	private byte[] m_header;
	
/**
 * CONSTRUCTOR
 * @param hops
 * @param recipient
 */
	public Header(int hops, Vector recipient) {
		m_header = buildHeader(hops, recipient);
		
	}
	
	/**
	 * 
	 * builds a header with the specified hops
	 * @param hops int
	 * @param recipient String
	 * @return Vector with byte[]-Objects
	 */
	private byte[] buildHeader(int hops, Vector recipient) {

//Build Secrets
		Vector secrets = new Vector(); secrets.add(0,null);
		SecureRandom sr = new SecureRandom();
		byte rand[] = new byte[16];
		
		for (int i=1; i<hops+1; i++) {
			sr.nextBytes(rand);
			secrets.add(i, rand);
		}
		
//build with the number of hops a List with mmrdescriptions, last one is an exit-node
		MMRList mmrlist = new MMRList(new InfoServiceMMRListFetcher());
		mmrlist.updateList();
		Vector routers = mmrlist.getByRandom(hops);
		MMRDescription r = (MMRDescription)routers.lastElement();
		routers.remove(r);
		
//get the needed Information from a MMRDescription
	    Vector headerKey = new Vector(); headerKey.add(0,null); // Vector with the Header-Keys
	    Vector routingInformation = new Vector(); routingInformation.add(0,null);//Vector with the RoutingInformation
	    Vector junkKeys = new Vector(); junkKeys.add(0,null);//Vector with the Junk-Keys
	    Vector routingType = new Vector(); routingType.add(0,null); //Vector with the Routing Types
	    Vector k = new Vector(); k.add(0,null);//Subkey of the Secret Keys
	    	    
	    for (int i=1; i<hops+1 ;i++) {
	        MMRDescription mmrd = null;
	    	if (i == hops-1) 
	        	mmrd = r;
	        else
	           mmrd = (MMRDescription)routers.get(i-1); 
	        //Headerkeys, k und junkKeys
	    	headerKey.add(i,mmrd.getPacketKey());   
	    	junkKeys.add(i,(subKey((byte[]) secrets.get(i), "RANDOM JUNK")));
	    	k.add(i,(subKey((byte[]) secrets.get(i), "HEADER SECRET KEY")));
	    	//Routing Information
	    	Vector rin = new Vector();
	    	if (i==hops-1) rin = mmrd.getExitInformation(recipient);
	    	else rin = mmrd.getRoutingInformation();
	    	routingInformation.add(i,(byte[]) rin.get(1));
	    	routingType.add(i,(byte[]) rin.get(0));
	    }
	    
//Length of all subheaders
	    int totalsize = 0;
	    
	    for(int i=1; i<hops+1;i++) {
	    	byte[] rinf = (byte[]) routingInformation.get(i);
	    	totalsize = totalsize + rinf.length + MIN_SH + PK_OVERHEAD_LEN; 
	    }
	    
//Length of padding needed for the header
	    int paddingLen = HEADER_LEN - totalsize;
	    if (totalsize > HEADER_LEN) {
	    	LogHolder.log(LogLevel.ERR, LogType.MISC,
				  "[Calculating HEADERSIZE]: Subheaders don't fit into HEADER_LEN ");
	    }

// Calculate the junk
	    Vector junkSeen = new Vector();
	    Vector stream = new Vector();
	    //at position 0 there is no junk
	    stream.add(0,"");
	    junkSeen.add(0,("").getBytes());

	    //calculating for the rest
	    for (int i=1; i<hops; i++) {
	    	byte[] lastJunk =  (byte[]) junkSeen.get(i-1);
	    	junkSeen.add(i, ByteArrayUtil.conc(lastJunk,(prng((byte[])junkKeys.get(i), ((byte[])routingInformation.get(i)).length))));
	    	stream.add(i,prng((byte[])k.get(i), 2048 + ((byte[])routingInformation.get(i)).length));
	    	int offset = HEADER_LEN - PK_ENC_LEN - lastJunk.length;
	    	byte[] nextJunk = xor((byte[])junkSeen.get(i), ByteArrayUtil.copy((byte[])stream.get(i),offset,((byte[])junkSeen.get(i)).length));
	    	junkSeen.set(i, nextJunk) ;
	    	}
	    
//We start with the padding.
	    Vector header = new Vector();
	    header.setSize(hops+1);
	    
	    byte[] padding = new byte[paddingLen];
	    sr.nextBytes(padding); 
	    header.add(hops+1,padding);

//Now, we build the subheaders, iterating through the nodes backwards.
	    for (int i = hops; i>=1 ; i--) {
	        	//initial the actual routingInformation and routingType
	        	byte[] rt = (byte[])routingType.get(i);
	        	byte[] ri = (byte[])routingInformation.get(i);
	        	//build the Subheader without the digest
	      		byte[] sh0 = shs(VERSION_MAJOR, (byte[]) secrets.get(i), z(HASH_LEN), ByteArrayUtil.inttobyte(ri.length, 2), rt, ri);
	      		int sh_len = sh0.length;
		        //concatenate with the last Subheader
		        byte[] h0 = ByteArrayUtil.conc(sh0,(byte[])header.get(i+1));
		        //take the rest...
		        byte[] rest = ByteArrayUtil.copy(h0,PK_MAX_DATA_LEN, h0.length - PK_MAX_DATA_LEN);
		        //...and encrypt it
		        byte[] erest = xor(prng((byte[]) k.get(i), rest.length), rest);
		    	//digest of the encrypted rest		        
		        byte[] digest = hash(ByteArrayUtil.conc(erest , (byte[])junkSeen.get(i-1)));
		        //build the subheader with the digest
		        byte[] sh = shs(VERSION_MAJOR, (byte[]) secrets.get(i), digest, ByteArrayUtil.inttobyte(ri.length, 2), rt, ri);
		        //calculate the underflow	        
		        int underflow = max(PK_MAX_DATA_LEN - sh_len, 0);
		        //take the part needed to encrypt with the publiuc key
		        byte[] rsa_part = ByteArrayUtil.conc(sh, ByteArrayUtil.copy(h0,PK_MAX_DATA_LEN - underflow, underflow));
		        //encrypt it
		        byte[] esh = pk_encrypt((MyRSAPublicKey)headerKey.get(i), rsa_part); 
		        //adden
		  		header.add(i, ByteArrayUtil.conc(esh,erest)); 
	 

	    }
	    //last one is the final header
	   return (byte[])header.get(1);
	}
	
	/**
	 * Gives back the header as byteArray
	 * @return header byte[]
	 */
	public byte[] getAsByteArray() {
		return m_header;		
	}
	
//------------------HELPER-Methods---------------------------
	
	/**
	 * Subkey 
	 * @param Secret Key byte[]
	 * @param phrase String
	 * @return the subkey byte[]
	 */
	private byte[] subKey(byte[] secretKey, String phrase) {
		return ByteArrayUtil.copy(hash(ByteArrayUtil.conc(secretKey, phrase.getBytes())),0,16);
	}
	
	/**
	 * fixed-size part of the subheader structure 
	 * @param version byte[]
	 * @param secretkey byte[]
	 * @param digest byte[]
	 * @param routingsize byte[]
	 * @param routingtype byte[]
	 * @return part byte[]
	 */
	private byte[] fshs(byte[] v, byte[] sk, byte[] d, byte[] rs, byte[] rt) {
		
		return ByteArrayUtil.conc(v,sk,d,rs,rt);
	}
	
	/**
	 * entire subheader 
	 * @param Version byte[]
	 * @param SecretKey byte[]
	 * @param Digest byte[]
	 * @param RoutingSize byte[]
	 * @param RoutingType byte[]
	 * @param RoutingInformation byte[]
	 * @return entire subheader byte[]
	 */
	private byte[] shs(byte[] V, byte[] SK,byte[] D,byte[] RS,byte[] RT,byte[] RI) {
		
		return ByteArrayUtil.conc(fshs(V,SK,D,RS,RT), RI);
	}
	
	/**
	 * Creates an octet-array filles with zeros 
	 * @param len int
	 * @return the array byte[]
	 */
	private byte[] z(int len) {
		byte[] ret = new byte[len];
		for (int i=0; i<len;i++) {
			ret[i] = new Integer(0).byteValue();
		}
		return ret;
		
	}
	
	/**
	 * Creates a octet-array using Cryptographic Stream generator
	 * @param key byte[]
	 * @param len int
	 * @return the array byte[]
	 */
	private byte[] prng(byte[] key, int len) {
		byte[] zerovector = z(16);
		byte[] erg=null;
		
		for (int i=0; i<len; i++) {
			zerovector[15] = new Integer(i).byteValue();
			erg = ByteArrayUtil.conc(erg,aes_ctr(key, zerovector));
		}
		erg = ByteArrayUtil.copy(erg,0,len);
		return erg;
	}
	
	/**
	 * encrypts a given array with aes in counter mode
	 * @param key byte[]
	 * @param plain byte[]
	 * @return the ciphered array byte[]
	 */
	private byte[] aes_ctr(byte[] key, byte[] plain) {
		MyAES engine = new MyAES();
		
		byte[] data = new byte[plain.length];
		try {
			engine.init(true,key);
			engine.processBytesCTR(plain,0,data,0,16);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return data;
		
	}

	/**
	 * Compares two int values and returns the larger one 
	 * @param first int
	 * @param second int
	 * @return the larger one int
	 */
	private int max(int first, int second) {
		if (first<second) first=second;
		return first;
	}
	
	/**
	 * xor of two byte[] 
	 * @param one byte[]
	 * @param two byte[]
	 * @return the xor, if not the same length: null
	 */
	private byte[] xor(byte[] one, byte[] two) {
		if (one.length != two.length) return null;
		else {
			byte[] result = new byte[one.length];
			short n =(short) 0;
			while (n<one.length) {
				result[n] = (byte) (one[n]^two[n]);//((new BigInteger(new Byte(one[n]).toString())).xor(new BigInteger(new Byte(two[n]).toString()))).byteValue();
				n++;
			}
		
		return result;
		}
	}
	
	/**
	 * SHA1Digest of x 
	 * @param x byte[]
	 * @return the digest byte[]
	 */
	private byte[] hash(byte[] x) {
		SHA1Digest myDigest = new SHA1Digest();
		myDigest.update(x, 0, x.length);
		byte[] ret = new byte[myDigest.getDigestSize()];
		myDigest.doFinal(ret, 0);
		return ret;
	}

	/**
	 * PublicKey Encryption of 
	 * @param key MyRSAPublicKey
	 * @param m byte[]
	 * @return the digest byte[]
	 */
	private byte[] pk_encrypt(MyRSAPublicKey key, byte[] m) {
		
		byte[] result = new byte[m.length];
		MyRSA engine = new MyRSA();
		try {
			engine.init(key);
			result = engine.processBlockOAEP(m, 0, m.length); 
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}

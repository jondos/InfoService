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
import anon.mixminion.message.MixMinionCryptoUtil;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.crypto.MyRSA;
import anon.crypto.MyRSAPublicKey;
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
    private final int HEADER_LEN = 2048;
    private final int OAEP_OVERHEAD = 42;
    private final int MIN_SUBHEADER_LEN = 42;
    private final int PK_ENC_LEN = 256;
    private final int PK_OVERHEAD_LEN = 42;
    private final int PK_MAX_DATA_LEN = 214;
    private final int HASH_LEN = 20;
    private final int MIN_SH = 42;
    private byte[] VERSION_MAJOR = {0x00,0x03}; //Version Minor, Version Major
    
    //stuff
    private Vector m_secrets;
    private MMRDescription m_firstrouter;
    private byte[] m_header;
    
    
    
/**
 * CONSTRUCTOR
 * @param hops
 * @param recipient
 */
    public Header(int hops, Vector recipient) {
        System.out.println("Neuer Header wird gebaut");
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
//Variables
        //Adresses of the intermediate nodes
    	Vector routingInformation = new Vector(); // Vector with the RoutingInformation
    	Vector routingType = new Vector(); // Vector with the Routing Types
    	//Public Keys of the intermediate nodes
    	Vector publicKey = new Vector(); // Vector with the Public-Keys
    	//Secret Keys to be shared with the intermediate Nodes
    	m_secrets = new Vector();
    	//junkKeys
    	Vector junkKeys = new Vector(); // Vector with the Junk-Keys
    	//Subsecrets
    	Vector subSecret = new Vector(); // Subkeys of the Secret Keys
    	//Anzahl der intermediate Nodes
    	int internodes = hops;
    	//Delivery Router
    	MMRDescription lastrouter;
    	//Padding Sizes
    	int[] size = new int[internodes+1];
    	
//Initialisation of the needed data

        //set at index 0 a null
        publicKey.add(null);
        routingInformation.add(null);
        junkKeys.add(null);
        routingType.add(null);
        subSecret.add(null);
        m_secrets.add(null);
        
    	
        

    	
    	//Fill routingInformation, routing Type and secrets
    	MMRList mmrlist = new MMRList(new InfoServiceMMRListFetcher());
        mmrlist.updateList();
        Vector routers = mmrlist.getByRandom(hops);
        m_firstrouter = (MMRDescription)routers.get(0);
        lastrouter = (MMRDescription)routers.get(hops-1); 

        for (int i = 1; i <= internodes ; i++) 
        {	
        	m_secrets.add(MixMinionCryptoUtil.randomArray(16));
            MMRDescription mmrd = (MMRDescription)routers.get(i-1); 
            
            //Headerkeys, k und junkKeys
            publicKey.add(mmrd.getPacketKey());
            junkKeys.add((subKey((byte[]) m_secrets.get(i), "RANDOM JUNK")));
            subSecret.add((subKey((byte[]) m_secrets.get(i), "HEADER SECRET KEY")));
            
            //Routing Information
            Vector rin = new Vector();
            if (i == internodes)
            {
            	rin = mmrd.getExitInformation(recipient);
            }
            else
            {
            	rin = mmrd.getRoutingInformation();
            }
            routingInformation.add((byte[]) rin.get(1));

            //First Routing Type must be an swp-fwd, except only one hop
            if ((i == 1) && (hops > 1)) 
                {
                routingType.add(ByteArrayUtil.inttobyte(4,2));
                }
            else
                {
                routingType.add((byte[]) rin.get(0));   
                }
            
        }
        
        //sizes berechnen
        int totalsize = 0; // Length of all Subheaders
        for (int i=1; i <= internodes; i++) {
        	byte[] ri;
        	if (i == internodes) {
        		ri=(byte[])lastrouter.getExitInformation(recipient).get(1);
        	}
        	else {
        		ri = (byte[])routingInformation.get(i+1);
        	}
        	size[i] = ((MIN_SH + PK_OVERHEAD_LEN + ri.length));
        	//Length of all subheaders
        	totalsize = totalsize + size[i];
         }
        

        
 
        
//Length of padding needed for the header
        int paddingLen = HEADER_LEN - totalsize;
        if (totalsize > HEADER_LEN) 
        {
            LogHolder.log(LogLevel.ERR, LogType.MISC,
                  "[Calculating HEADERSIZE]: Subheaders don't fit into HEADER_LEN ");
        }

// Calculate the junk
        Vector junkSeen = new Vector();
        Vector stream = new Vector();
        //at position 0 there is no junk
        stream.add(null);
        junkSeen.add(("").getBytes());

        //calculating for the rest
        for (int i = 1; i <= internodes; i++)
        {
        	byte[] lastJunk =  (byte[]) junkSeen.get(i-1);
            byte[] temp = ByteArrayUtil.conc(lastJunk,(MixMinionCryptoUtil.createPRNG((byte[])junkKeys.get(i), size[i])));
            stream.add(MixMinionCryptoUtil.createPRNG((byte[])subSecret.get(i), 2048 + size[i] ));
            int offset = HEADER_LEN - PK_ENC_LEN - lastJunk.length;
            junkSeen.add(MixMinionCryptoUtil.xor(temp, ByteArrayUtil.copy((byte[])stream.get(i),offset,(temp.length))));
         }
        
//We start with the padding.
        Vector header = new Vector(); //Vector for cumulating the subheaders
        header.setSize(internodes+2);
        byte[] padding = MixMinionCryptoUtil.randomArray(paddingLen);
        header.setElementAt(padding,internodes+1);

//Now, we build the subheaders, iterating through the nodes backwards.
        for (int i = internodes; i >= 1 ; i--) {
                //initial the actual routingInformation and routingType
        		
        		byte[] rt;
        		byte[] ri;
        		if (i==internodes) {
                	rt = (byte[])lastrouter.getExitInformation(recipient).get(0);
                	ri = (byte[])lastrouter.getExitInformation(recipient).get(1);
                	
        		}
                else {
                	rt = (byte[])routingType.get(i+1);
                	ri = (byte[])routingInformation.get(i+1);
                	
                }
        		        		
                //build the Subheader without the digest
                byte[] sh0 = makeSHS(VERSION_MAJOR, (byte[]) m_secrets.get(i), MixMinionCryptoUtil.zeroArray(HASH_LEN), ByteArrayUtil.inttobyte(ri.length, 2), rt, ri);
                int sh_len = sh0.length;
                //concatenate with the last Subheader
                byte[] h0 = ByteArrayUtil.conc(sh0,(byte[])header.get(i+1));
               //take the rest...
                byte[] rest = ByteArrayUtil.copy(h0,PK_MAX_DATA_LEN, h0.length - PK_MAX_DATA_LEN);
                //...and encrypt it
                byte[] erest = MixMinionCryptoUtil.Encrypt((byte[]) subSecret.get(i), rest);
                //digest of the encrypted rest 
                byte[] digest = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(erest , (byte[])junkSeen.get(i-1)));
                //build the subheader with the digest
                byte[] sh = makeSHS(VERSION_MAJOR, (byte[]) m_secrets.get(i), digest, ByteArrayUtil.inttobyte(ri.length, 2), rt, ri);
                //calculate the underflow           
                int underflow = max(PK_MAX_DATA_LEN - sh_len, 0);
                //take the part needed to encrypt with the publiuc key
                byte[] rsa_part = ByteArrayUtil.conc(sh, ByteArrayUtil.copy(h0,PK_MAX_DATA_LEN - underflow, underflow));
                //encrypt it
                byte[] esh = pk_encrypt((MyRSAPublicKey)publicKey.get(i), rsa_part); 
                //adden
                header.setElementAt(ByteArrayUtil.conc(esh,erest),i); 
     

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
    
    /**
     * Gives back the secrets Vector with byte[]
     * @return secrets Vector
     */
    public Vector getSecrets() {
        Vector ret = new Vector();
        for(int i=0; i<m_secrets.size()-1;i++) {
            ret.add((byte[])m_secrets.get(i+1));
        }
        return ret;
    }
    
    /**
     * Get the first Router as MMRDescription
     * @return firstrouter MMRDescription
     */
    public MMRDescription getRoute() {
    	return m_firstrouter;
    }
    
//------------------HELPER-Methods---------------------------
    
    /**
     * Subkey 
     * @param Secret Key byte[]
     * @param phrase String
     * @return the subkey byte[]
     */
    private byte[] subKey(byte[] secretKey, String phrase) {
        return ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(secretKey, phrase.getBytes())),0,16);
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
    private byte[] makeFSHS(byte[] v, byte[] sk, byte[] d, byte[] rs, byte[] rt) {
        
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
    private byte[] makeSHS(byte[] V, byte[] SK,byte[] D,byte[] RS,byte[] RT,byte[] RI) {
        
        return ByteArrayUtil.conc(makeFSHS(V,SK,D,RS,RT), RI);
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
     * PublicKey Encryption of 
     * @param key MyRSAPublicKey
     * @param m byte[]
     * @return the digest byte[]
     */
    private byte[] pk_encrypt(MyRSAPublicKey key, byte[] m) {
		byte[] sp = "He who would make his own liberty secure, must guard even his enemy from oppression.".getBytes(); 
    	SHA1Digest digest = new SHA1Digest();
		digest.update(sp,0,sp.length);
    	MyRSA engine = new MyRSA(digest);
    	try {
    	   engine.init(key);
    	   return engine.processBlockOAEP(m, 0, m.length);
    	   } catch (Exception e) {
    	     e.printStackTrace();
    	     return null;
    	   	}
    } 
}

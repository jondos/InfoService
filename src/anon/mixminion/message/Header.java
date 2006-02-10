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
import anon.util.Base64;

/**
 * @author Stefan Rönisch
 */
public class Header
{

	//Constants
	private final int HEADER_LEN = 2048;
	private final int OAEP_OVERHEAD = 42;
	private final int MIN_SUBHEADER_LEN = 42;
	private final int PK_ENC_LEN = 256;
	private final int PK_OVERHEAD_LEN = 42;
	private final int PK_MAX_DATA_LEN = 214;
	private final int HASH_LEN = 20;
	private final int MIN_SH = 42;
	private byte[] VERSION_MAJOR =
		{
		0x00, 0x03}; //Version Minor, Version Major

	//stuff
	private Vector m_secrets;
	private MMRDescription m_firstrouter;
	private byte[] m_header;

	/**
	 * CONSTRUCTOR
	 * @param hops
	 * @param recipient
	 */
	public Header(int hops, Vector recipient)
	{
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
	private byte[] buildHeader(int hops, Vector recipient)
	{
//Variables
		//Adresses of the intermediate nodes
		Vector routingInformation = new Vector(); // Vector with the RoutingInformation
		//Vector routingType = new Vector(); // Vector with the Routing Types
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
		int[] size = new int[internodes + 1];

//Initialisation of the needed data

		//set at index 0 a null
		publicKey.addElement(null);
		routingInformation.addElement(null);
		junkKeys.addElement(null);
		subSecret.addElement(null);
		m_secrets.addElement(null);

		//Fill routingInformation, routing Type and secrets
		/*MMRList mmrlist = new MMRList(new InfoServiceMMRListFetcher());
		mmrlist.updateList();
		//FIXME wasn mit fragmenten?
		Vector routers = mmrlist.getByRandom(hops, false);
		m_firstrouter = (MMRDescription) routers.elementAt(0);
		lastrouter = (MMRDescription) routers.elementAt(hops - 1);
*/

		MMRDescription mmdescr=new MMRDescription("141.76.46.90", "JAP", 48099,

														Base64.decode("JzZdlwSPXEfncFH/tl+A5CZhGN4="),

														Base64.decode("HR6d+Li3vbUBu9NKm500wadrnng="),
														true, false);

		mmdescr.setIdentityKey(Base64.decode("MIIBCgKCAQEA1XZi7436861AxxYgNEbnVyobQg+wPbuRZNlFJ5x2pDyRNFOQrLG2+pK8Z/AzVxMbEPvLNCIn5nAvuRZGn/aGryhy0bzlX/D0CHG44qagoVb36kGeXv4jvLy/aYu4nxLUrgoEdp0t+J3kWQScnOsOiBwF60l4Acc5+51T/YBctYvSO2OY8VpexB7S2+1pIGjT7rsXVXCK18G0Xms4dt/qPKK/2fzPw/kR06Ggf006JCyFKEDMfDrbZ6gvLS3BiUVZV7ZYACAeznv5Hj/dwRJtT7QGqvJyr6zrZi7epSD41J3Uqh8oYABu5g3cQEtdMc33WLBXdhmjAmTG0wcSZxGyeQIDAQAB"));

		mmdescr.setPacketKey(Base64.decode("MIIBCgKCAQEAkoQcO+eFjs3blj9v1rzCXDjRPJc3pC/R7XyXaYGsqv9ps2KB92mSyFxpSp3XTGE2AWW463AKAV5nz3DksUfhuQ2I0ILccVza0Uey/zvLEI0HCdI52fLopyr9u5+m0zWuGonY7IZYxOcJnNBbeiZIuxK1lRXQwz1r2UGyjewpfb9Zwb7fG7WLVq9mo1EDcewNop2fuA3wy049168SZFWFOd7QrtbnBsRVeVo3ZS/FOVF7PjNU7lGc3uVIWMxaMdY1Y+XjDD8oD+xOXp5jad2qyeqbKbUHZS1CdWt8MmfOMcZ3df+43U4s/q3+1YeyADlRBPOdoo7ZCnY6QVvayXFUBQIDAQAB"));
lastrouter=mmdescr;
		mmdescr=new MMRDescription("141.76.46.90", "JAP", 48099,

														Base64.decode("JzZdlwSPXEfncFH/tl+A5CZhGN4="),

														Base64.decode("HR6d+Li3vbUBu9NKm500wadrnng="),
														true, false);

		mmdescr.setIdentityKey(Base64.decode("MIIBCgKCAQEA1XZi7436861AxxYgNEbnVyobQg+wPbuRZNlFJ5x2pDyRNFOQrLG2+pK8Z/AzVxMbEPvLNCIn5nAvuRZGn/aGryhy0bzlX/D0CHG44qagoVb36kGeXv4jvLy/aYu4nxLUrgoEdp0t+J3kWQScnOsOiBwF60l4Acc5+51T/YBctYvSO2OY8VpexB7S2+1pIGjT7rsXVXCK18G0Xms4dt/qPKK/2fzPw/kR06Ggf006JCyFKEDMfDrbZ6gvLS3BiUVZV7ZYACAeznv5Hj/dwRJtT7QGqvJyr6zrZi7epSD41J3Uqh8oYABu5g3cQEtdMc33WLBXdhmjAmTG0wcSZxGyeQIDAQAB"));

		mmdescr.setPacketKey(Base64.decode("MIIBCgKCAQEAkoQcO+eFjs3blj9v1rzCXDjRPJc3pC/R7XyXaYGsqv9ps2KB92mSyFxpSp3XTGE2AWW463AKAV5nz3DksUfhuQ2I0ILccVza0Uey/zvLEI0HCdI52fLopyr9u5+m0zWuGonY7IZYxOcJnNBbeiZIuxK1lRXQwz1r2UGyjewpfb9Zwb7fG7WLVq9mo1EDcewNop2fuA3wy049168SZFWFOd7QrtbnBsRVeVo3ZS/FOVF7PjNU7lGc3uVIWMxaMdY1Y+XjDD8oD+xOXp5jad2qyeqbKbUHZS1CdWt8MmfOMcZ3df+43U4s/q3+1YeyADlRBPOdoo7ZCnY6QVvayXFUBQIDAQAB"));
m_firstrouter=mmdescr;
		for (int i = 1; i <= internodes; i++)
		{
			//secrets
			m_secrets.addElement(MixMinionCryptoUtil.randomArray(16));

			//Headerkeys, k und junkKeys
			mmdescr=new MMRDescription("141.76.46.90", "JAP", 48099,

														Base64.decode("JzZdlwSPXEfncFH/tl+A5CZhGN4="),

														Base64.decode("HR6d+Li3vbUBu9NKm500wadrnng="),
														true, false);

	mmdescr.setIdentityKey(Base64.decode("MIIBCgKCAQEA1XZi7436861AxxYgNEbnVyobQg+wPbuRZNlFJ5x2pDyRNFOQrLG2+pK8Z/AzVxMbEPvLNCIn5nAvuRZGn/aGryhy0bzlX/D0CHG44qagoVb36kGeXv4jvLy/aYu4nxLUrgoEdp0t+J3kWQScnOsOiBwF60l4Acc5+51T/YBctYvSO2OY8VpexB7S2+1pIGjT7rsXVXCK18G0Xms4dt/qPKK/2fzPw/kR06Ggf006JCyFKEDMfDrbZ6gvLS3BiUVZV7ZYACAeznv5Hj/dwRJtT7QGqvJyr6zrZi7epSD41J3Uqh8oYABu5g3cQEtdMc33WLBXdhmjAmTG0wcSZxGyeQIDAQAB"));

	mmdescr.setPacketKey(Base64.decode("MIIBCgKCAQEAkoQcO+eFjs3blj9v1rzCXDjRPJc3pC/R7XyXaYGsqv9ps2KB92mSyFxpSp3XTGE2AWW463AKAV5nz3DksUfhuQ2I0ILccVza0Uey/zvLEI0HCdI52fLopyr9u5+m0zWuGonY7IZYxOcJnNBbeiZIuxK1lRXQwz1r2UGyjewpfb9Zwb7fG7WLVq9mo1EDcewNop2fuA3wy049168SZFWFOd7QrtbnBsRVeVo3ZS/FOVF7PjNU7lGc3uVIWMxaMdY1Y+XjDD8oD+xOXp5jad2qyeqbKbUHZS1CdWt8MmfOMcZ3df+43U4s/q3+1YeyADlRBPOdoo7ZCnY6QVvayXFUBQIDAQAB"));

			publicKey.addElement(mmdescr.getPacketKey());
			junkKeys.addElement( (subKey( (byte[]) m_secrets.elementAt(i), "RANDOM JUNK")));
			subSecret.addElement( (subKey( (byte[]) m_secrets.elementAt(i), "HEADER SECRET KEY")));

			//Routing Information
			RoutingInformation ri=mmdescr.getRoutingInformation();
			if ( (i == 1) && (hops > 1))/*(i==internodes)*/
			{
				//First Routing Type must be an swp-fwd, except only one hop
				ri.m_Type=RoutingInformation.TYPE_SWAP_FORWARD_TO_HOST;
			}
			routingInformation.addElement( ri);

		}

		//sizes berechnen
		int totalsize = 0; // Length of all Subheaders
		for (int i = 1; i <= internodes; i++)
		{
			if (i == internodes)
			{
				size[i]= lastrouter.getExitInformation(recipient).m_Content.length;
			}
			else
			{
				size[i]=((RoutingInformation) routingInformation.elementAt(i + 1)).m_Content.length
					;
			}
			size[i]+= MIN_SH + PK_OVERHEAD_LEN;
			//Length of all subheaders
			totalsize+=size[i];
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
		stream.addElement(null);
		junkSeen.addElement( ("").getBytes());

		//calculating for the rest
		for (int i = 1; i <= internodes; i++)
		{
			byte[] lastJunk = (byte[]) junkSeen.elementAt(i - 1);
			byte[] temp = ByteArrayUtil.conc(lastJunk,
											 (MixMinionCryptoUtil.createPRNG( (byte[]) junkKeys.elementAt(i),
				size[i])));
			stream.addElement(MixMinionCryptoUtil.createPRNG( (byte[]) subSecret.elementAt(i), 2048 + size[i]));
			int offset = HEADER_LEN - PK_ENC_LEN - lastJunk.length;
			junkSeen.addElement(MixMinionCryptoUtil.xor(temp,
				ByteArrayUtil.copy( (byte[]) stream.elementAt(i), offset, (temp.length))));
		}

//We start with the padding.
		Vector header = new Vector(); //Vector for cumulating the subheaders
		header.setSize(internodes + 2);
		byte[] padding = MixMinionCryptoUtil.randomArray(paddingLen);
		header.setElementAt(padding, internodes + 1);

//Now, we build the subheaders, iterating through the nodes backwards.
		for (int i = internodes; i >= 1; i--)
		{
			//initial the actual routingInformation and routingType

			ForwardInformation ri;
			if (i == internodes)
			{
				ri=(ForwardInformation)lastrouter.getExitInformation(recipient);

			}
			else
			{
				ri = (ForwardInformation) routingInformation.elementAt(i + 1);

			}

			//build the Subheader without the digest
			byte[] sh0 = makeSHS(VERSION_MAJOR, (byte[]) m_secrets.elementAt(i),
								 new byte[HASH_LEN],
								 ByteArrayUtil.inttobyte(ri.m_Content.length, 2), ri.m_Type, ri.m_Content);
			int sh_len = sh0.length;
			//concatenate with the last Subheader
			byte[] h0 = ByteArrayUtil.conc(sh0, (byte[]) header.elementAt(i + 1));
			//take the rest...
			byte[] rest = ByteArrayUtil.copy(h0, PK_MAX_DATA_LEN, h0.length - PK_MAX_DATA_LEN);
			//...and encrypt it
			byte[] erest = MixMinionCryptoUtil.Encrypt( (byte[]) subSecret.elementAt(i), rest);
			//digest of the encrypted rest
			byte[] digest = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(erest,
				(byte[]) junkSeen.elementAt(i - 1)));
			//build the subheader with the digest
			byte[] sh = makeSHS(VERSION_MAJOR, (byte[]) m_secrets.elementAt(i), digest,
								ByteArrayUtil.inttobyte(ri.m_Content.length, 2), ri.m_Type, ri.m_Content);
			//calculate the underflow
			int underflow = max(PK_MAX_DATA_LEN - sh_len, 0);
			//take the part needed to encrypt with the publiuc key
			byte[] rsa_part = ByteArrayUtil.conc(sh,
												 ByteArrayUtil.copy(h0, PK_MAX_DATA_LEN - underflow, underflow));
			//encrypt it
			byte[] esh = pk_encrypt( (MyRSAPublicKey) publicKey.elementAt(i), rsa_part);
			//adden
			header.setElementAt(ByteArrayUtil.conc(esh, erest), i);

		}
		//last one is the final header

		return (byte[]) header.elementAt(1);
	}

	/**
	 * Gives back the header as byteArray
	 * @return header byte[]
	 */
	public byte[] getAsByteArray()
	{
		return m_header;
	}

	/**
	 * Gives back the secrets Vector with byte[]
	 * @return secrets Vector
	 */
	public Vector getSecrets()
	{
		Vector ret = new Vector();
		for (int i = 0; i < m_secrets.size() - 1; i++)
		{
			ret.addElement( (byte[]) m_secrets.elementAt(i + 1));
		}
		return ret;
	}

	/**
	 * Get the first Router as MMRDescription
	 * @return firstrouter MMRDescription
	 */
	public MMRDescription getRoute()
	{
		return m_firstrouter;
	}

//------------------HELPER-Methods---------------------------

	/**
	 * Subkey
	 * @param Secret Key byte[]
	 * @param phrase String
	 * @return the subkey byte[]
	 */
	private byte[] subKey(byte[] secretKey, String phrase)
	{
		return ByteArrayUtil.copy(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(secretKey, phrase.getBytes())),
								  0, 16);
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
	private byte[] makeFSHS(byte[] v, byte[] sk, byte[] d, byte[] rs, short rt)
	{

		return ByteArrayUtil.conc(v, sk, d, rs,ByteArrayUtil.inttobyte(rt,2));
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
	private byte[] makeSHS(byte[] V, byte[] SK, byte[] D, byte[] RS, short RT, byte[] RI)
	{

		return ByteArrayUtil.conc(makeFSHS(V, SK, D, RS, RT), RI);
	}

	/**
	 * Compares two int values and returns the larger one
	 * @param first int
	 * @param second int
	 * @return the larger one int
	 */
	private int max(int first, int second)
	{
		if (first < second)
		{
			first = second;
		}
		return first;
	}

	/**
	 * PublicKey Encryption of
	 * @param key MyRSAPublicKey
	 * @param m byte[]
	 * @return the digest byte[]
	 */
	private byte[] pk_encrypt(MyRSAPublicKey key, byte[] m)
	{
		byte[] sp = "He who would make his own liberty secure, must guard even his enemy from oppression.".
			getBytes();
		SHA1Digest digest = new SHA1Digest();
		digest.update(sp, 0, sp.length);
		MyRSA engine = new MyRSA(digest);
		try
		{
			engine.init(key);
			return engine.processBlockOAEP(m, 0, m.length);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}

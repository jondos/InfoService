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
/*
 * Created on Mar 29, 2004
 *
 */
package anon.tor.tinytls;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import anon.tor.tinytls.util.PRF;
import anon.tor.tinytls.util.hash;
import anon.tor.util.helper;
import anon.crypto.JAPCertificate;
import logging.*;
/**
 * @author stefan
 *
 * Diffie Hellman Key Exchange with a RSA signed Certificate
 */
public class DHE_RSA_Key_Exchange extends Key_Exchange{

	//maximum length for keymaterial (3DES_EDE_CBC_SHA)
	//see RFC2246 for more information
	private final static int MAXKEYMATERIALLENGTH = 104;

	private final static byte[] CLIENTFINISHEDLABEL = ("client finished").getBytes();
	private final static byte[] SERVERFINISHEDLABEL = ("server finished").getBytes();
	private final static byte[] KEYEXPANSION = ("key expansion").getBytes();
	private final static byte[] MASTERSECRET = ("master secret").getBytes();

	private DHParameters m_dhparams;
	private DHPublicKeyParameters m_dhserverpub;
	private byte[] m_premastersecret;
	private byte[] m_mastersecret;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;

	/**
	 * Decode the server keys and check the certificate
	 * @param bytes server keys
	 * @param clientrandom clientrandom
	 * @param serverrandom serverrandom
	 * @param servercertificate servercertificate
	 * @throws TLSException
	 */
	public void serverKeyExchange(byte[] bytes,int bytes_offset,int bytes_len,
								  byte[] clientrandom, byte[] serverrandom,JAPCertificate servercertificate) throws TLSException
	{
		this.m_clientrandom = clientrandom;
		this.m_serverrandom = serverrandom;
		int counter  = 0;
		BigInteger dh_p;
		BigInteger dh_g;
		BigInteger dh_ys;
		byte[] dummy;
		byte[] b=helper.copybytes(bytes,counter+bytes_offset,2);
		counter+=2;
		int length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter+bytes_offset,length);
		counter+=length;
		dh_p = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_P = "+dh_p.toString());

		b=helper.copybytes(bytes,counter+bytes_offset,2);
		counter+=2;
		length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter+bytes_offset,length);
		counter+=length;
		dh_g = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_G = "+dh_g.toString());

		b=helper.copybytes(bytes,counter+bytes_offset,2);
		counter+=2;
		length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter+bytes_offset,length);
		counter+=length;
		dh_ys = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_Ys = "+dh_ys.toString());

		this.m_dhparams = new DHParameters(dh_p,dh_g);
		this.m_dhserverpub = new DHPublicKeyParameters(dh_ys,this.m_dhparams);


		//-----------------------------------------

		byte[] serverparams = helper.copybytes(bytes,0+bytes_offset,counter);

		byte[] expectedSignature = helper.conc(
			hash.md5(new byte[][]
				 {clientrandom, serverrandom, serverparams}),
			hash.sha(new byte[][]
				{clientrandom, serverrandom, serverparams}));

		byte[] recievedSignature;

		try
		{
			SubjectPublicKeyInfo pki = servercertificate.getSubjectPublicKeyInfo();
			RSAPublicKeyStructure rsa_pks = new RSAPublicKeyStructure( (DERConstructedSequence)pki.getPublicKey());
			BigInteger modulus = rsa_pks.getModulus();
			BigInteger exponent = rsa_pks.getPublicExponent();
			AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
			rsa.init(false, new RSAKeyParameters(false,modulus,exponent));
			byte[] hash = helper.copybytes(bytes,counter+2+bytes_offset,bytes_len-counter-2);
			recievedSignature = rsa.processBlock(hash,0,hash.length);
		} catch(Exception e)
		{
			throw new TLSException("Cannot decode Signature");
		}
		for(int i=0;i<expectedSignature.length;i++)
		{
			if(expectedSignature[i]!=recievedSignature[i])
			{
				throw new TLSException("wrong Signature");
			}
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] Signature ok");
	}

	/**
	 * checks the server finished message
	 * @param b server finished message
	 * @throws TLSException
	 */
	public void serverFinished(byte[] b,int len,byte[] handshakemessages) throws TLSException {
		PRF prf = new PRF(this.m_mastersecret,SERVERFINISHEDLABEL,helper.conc(hash.md5(new byte[][]{handshakemessages}),hash.sha(new byte[][]{handshakemessages})));
		byte[] c = prf.calculate(12);
		if(b[0]==20&&b[1]==0&&b[2]==0&&b[3]==12)
		{
			for(int i=0;i<c.length;i++)
			{
				if(c[i]!=b[i+4])
				{
					throw new TLSException("wrong Server Finished message recieved");
				}
			}
			return;
		}
		throw new TLSException("wrong Server Finished message recieved");
	}

	/**
	 * generates the client key exchange message (see RFC2246)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public byte[] clientKeyExchange() throws TLSException {
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), this.m_dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);

		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters)pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters)pair.getPrivate();

		DHBasicAgreement dha = new DHBasicAgreement();
		dha.init(dhpriv);
		this.m_premastersecret = dha.calculateAgreement(this.m_dhserverpub).toByteArray();
		if(this.m_premastersecret[0]==0)
		{
			this.m_premastersecret = helper.copybytes(this.m_premastersecret,1,this.m_premastersecret.length-1);
		}
		PRF prf = new PRF(this.m_premastersecret,MASTERSECRET,helper.conc(this.m_clientrandom,this.m_serverrandom));
		this.m_mastersecret = prf.calculate(48);
		this.m_premastersecret = null;
		return  dhpub.getY().toByteArray();
	}

	/**
	 * generate the client finished message (see RFC2246)
	 * @param handshakemessages all handshakemessages that have been send before this
	 * @return client finished message
	 */
	public byte[] clientFinished(byte[] handshakemessages) throws TLSException {
		PRF prf = new PRF(this.m_mastersecret,CLIENTFINISHEDLABEL,helper.conc(hash.md5(new byte[][]{handshakemessages}),hash.sha(new byte[][]{handshakemessages})));
		return prf.calculate(12);
	}

	/**
	 * calculates the key material (see RFC2246 TLS Record Protocoll)
	 * @return key material
	 */
	public byte[] calculateKeys() {
		PRF prf = new PRF(this.m_mastersecret,KEYEXPANSION,helper.conc(this.m_serverrandom,this.m_clientrandom));
		return prf.calculate(MAXKEYMATERIALLENGTH);
	}

}

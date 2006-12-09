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
package anon.tor.tinytls.keyexchange;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.MyRSAPrivateKey;
import anon.tor.tinytls.TLSException;
import anon.tor.tinytls.util.PRF;
import anon.tor.tinytls.util.hash;
import anon.tor.util.helper;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author stefan
 *
 * Diffie Hellman Key Exchange with a RSA signed Certificate
 */
public class DHE_RSA_Key_Exchange extends Key_Exchange
{

	//maximum length for keymaterial (3DES_EDE_CBC_SHA)
	//see RFC2246 for more information
	private final static int MAXKEYMATERIALLENGTH = 104;

	private final static byte[] CLIENTFINISHEDLABEL = ("client finished").getBytes();

	private final static byte[] SERVERFINISHEDLABEL = ("server finished").getBytes();

	private final static byte[] KEYEXPANSION = ("key expansion").getBytes();

	private final static byte[] MASTERSECRET = ("master secret").getBytes();

	private final static BigInteger SAFEPRIME = new BigInteger(
		"00FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08" +
		"8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B" +
		"302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9" +
		"A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6" +
		"49286651ECE65381FFFFFFFFFFFFFFFF", 16);
	private final static DHParameters DH_PARAMS = new DHParameters(SAFEPRIME, new BigInteger("2"));

	private DHParameters m_dhparams;
	private DHPublicKeyParameters m_dhserverpub;
	private byte[] m_premastersecret;
	private byte[] m_mastersecret;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;

	private DHBasicAgreement m_dhe = null;

	public byte[] generateServerKeyExchange(IMyPrivateKey key, byte[] clientrandom, byte[] serverrandom) throws
		TLSException
	{
		if (! (key instanceof MyRSAPrivateKey))
		{
			throw new TLSException("wrong key type (cannot cast to MyRSAPrivateKey)");
		}
		MyRSAPrivateKey rsakey = (MyRSAPrivateKey) key;
		this.m_clientrandom = clientrandom;
		this.m_serverrandom = serverrandom;
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), DH_PARAMS);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);
		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters) pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters) pair.getPrivate();
		m_dhe = new DHBasicAgreement();
		m_dhe.init(dhpriv);

		byte[] dh_p = dhpub.getParameters().getP().toByteArray();
		dh_p = helper.conc(helper.inttobyte(dh_p.length, 2), dh_p);
		byte[] dh_g = dhpub.getParameters().getG().toByteArray();
		dh_g = helper.conc(helper.inttobyte(dh_g.length, 2), dh_g);
		byte[] dh_y = dhpub.getY().toByteArray();
		dh_y = helper.conc(helper.inttobyte(dh_y.length, 2), dh_y);
		byte[] message = helper.conc(dh_p, dh_g, dh_y);

		byte[] signature = helper.conc(
			hash.md5(clientrandom, serverrandom, message),
			hash.sha(clientrandom, serverrandom, message));
		BigInteger modulus = rsakey.getModulus();
		BigInteger exponent = rsakey.getPrivateExponent();
		AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
		rsa.init(true, new RSAKeyParameters(true, modulus, exponent));
		byte[] signature2;
		try
		{
			signature2 = rsa.processBlock(signature, 0, signature.length);
		}
		catch (InvalidCipherTextException ex)
		{
			throw new TLSException("cannot encrypt signature", 2, 80);
		}
		message = helper.conc(message, signature2, helper.inttobyte(signature2.length, 2), signature2);

		return message;
	}

	public void processServerKeyExchange(byte[] bytes, int bytes_offset, int bytes_len,
										 byte[] clientrandom, byte[] serverrandom,
										 JAPCertificate servercertificate) throws TLSException
	{
		this.m_clientrandom = clientrandom;
		this.m_serverrandom = serverrandom;
		int counter = 0;
		BigInteger dh_p;
		BigInteger dh_g;
		BigInteger dh_ys;
		byte[] dummy;
		byte[] b = helper.copybytes(bytes, counter + bytes_offset, 2);
		counter += 2;
		int length = ( (b[0] & 0xFF) << 8) | (b[1] & 0xFF);
		dummy = helper.copybytes(bytes, counter + bytes_offset, length);
		counter += length;
		dh_p = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_P = " + dh_p.toString());

		b = helper.copybytes(bytes, counter + bytes_offset, 2);
		counter += 2;
		length = ( (b[0] & 0xFF) << 8) | (b[1] & 0xFF);
		dummy = helper.copybytes(bytes, counter + bytes_offset, length);
		counter += length;
		dh_g = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_G = " + dh_g.toString());

		b = helper.copybytes(bytes, counter + bytes_offset, 2);
		counter += 2;
		length = ( (b[0] & 0xFF) << 8) | (b[1] & 0xFF);
		dummy = helper.copybytes(bytes, counter + bytes_offset, length);
		counter += length;
		dh_ys = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_Ys = " + dh_ys.toString());

		this.m_dhparams = new DHParameters(dh_p, dh_g);
		this.m_dhserverpub = new DHPublicKeyParameters(dh_ys, this.m_dhparams);

		//-----------------------------------------

		byte[] serverparams = helper.copybytes(bytes, 0 + bytes_offset, counter);

		byte[] expectedSignature = helper.conc(
			hash.md5(clientrandom, serverrandom, serverparams),
			hash.sha(clientrandom, serverrandom, serverparams));

		byte[] recievedSignature;

		try
		{
			SubjectPublicKeyInfo pki = servercertificate.getSubjectPublicKeyInfo();
			RSAPublicKeyStructure rsa_pks = new RSAPublicKeyStructure( (DERConstructedSequence) pki.
				getPublicKey());
			BigInteger modulus = rsa_pks.getModulus();
			BigInteger exponent = rsa_pks.getPublicExponent();
			AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
			rsa.init(false, new RSAKeyParameters(false, modulus, exponent));
			byte[] hash = helper.copybytes(bytes, counter + 2 + bytes_offset, bytes_len - counter - 2);
			recievedSignature = rsa.processBlock(hash, 0, hash.length);
		}
		catch (Exception e)
		{
			throw new TLSException("Cannot decode Signature", 1, 0);
		}
		for (int i = 0; i < expectedSignature.length; i++)
		{
			if (expectedSignature[i] != recievedSignature[i])
			{
				throw new TLSException("wrong Signature", 2, 21);
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] Signature ok");
	}

	public byte[] calculateServerFinished(byte[] handshakemessages)
	{
		PRF prf = new PRF(this.m_mastersecret, SERVERFINISHEDLABEL,
						  helper.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		return prf.calculate(12);
	}

	public void processServerFinished(byte[] b, int len, byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, SERVERFINISHEDLABEL,
						  helper.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		byte[] c = prf.calculate(12);
		if (b[0] == 20 && b[1] == 0 && b[2] == 0 && b[3] == 12)
		{
			for (int i = 0; i < c.length; i++)
			{
				if (c[i] != b[i + 4])
				{
					throw new TLSException("wrong Server Finished message recieved", 2, 20);
				}
			}
			return;
		}
		throw new TLSException("wrong Server Finished message recieved", 2, 10);
	}

	public void processClientKeyExchange(BigInteger dh_y)
	{
		DHPublicKeyParameters dhclientpub = new DHPublicKeyParameters(dh_y, DH_PARAMS);
		this.m_premastersecret = m_dhe.calculateAgreement(dhclientpub).toByteArray();
		if (this.m_premastersecret[0] == 0)
		{
			this.m_premastersecret = helper.copybytes(this.m_premastersecret, 1,
				this.m_premastersecret.length - 1);
		}
		PRF prf = new PRF(this.m_premastersecret, MASTERSECRET,
						  helper.conc(this.m_clientrandom, this.m_serverrandom));
		this.m_mastersecret = prf.calculate(48);
		this.m_premastersecret = null;
	}

	public byte[] calculateClientKeyExchange() throws TLSException
	{
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), this.m_dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);

		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters) pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters) pair.getPrivate();

		DHBasicAgreement dha = new DHBasicAgreement();
		dha.init(dhpriv);
		this.m_premastersecret = dha.calculateAgreement(this.m_dhserverpub).toByteArray();
		if (this.m_premastersecret[0] == 0)
		{
			this.m_premastersecret = helper.copybytes(this.m_premastersecret, 1,
				this.m_premastersecret.length - 1);
		}
		PRF prf = new PRF(this.m_premastersecret, MASTERSECRET,
						  helper.conc(this.m_clientrandom, this.m_serverrandom));
		this.m_mastersecret = prf.calculate(48);
		this.m_premastersecret = null;
		return dhpub.getY().toByteArray();
	}

	public void processClientFinished(byte[] verify_data, byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, CLIENTFINISHEDLABEL,
						  helper.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
	}

	public byte[] calculateClientFinished(byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, CLIENTFINISHEDLABEL,
						  helper.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		return prf.calculate(12);
	}

	public byte[] calculateKeys()
	{
		PRF prf = new PRF(this.m_mastersecret, KEYEXPANSION,
						  helper.conc(this.m_serverrandom, this.m_clientrandom));
		return prf.calculate(MAXKEYMATERIALLENGTH);
	}

}
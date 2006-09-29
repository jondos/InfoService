/*
 * Created on Mar 29, 2004
 *
 */
package tor.tinytls;

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

import tor.tinytls.util.PRF;
import tor.tinytls.util.hash;
import tor.util.helper;
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

	private final static byte[] FINISHEDLABEL = ("client finished").getBytes();
	private final static byte[] KEYEXPANSION = ("key expansion").getBytes();
	private final static byte[] MASTERSECRET = ("master secret").getBytes();

	private DHParameters dhparams;
	private DHPublicKeyParameters dhserverpub;
	private byte[] premastersecret;
	private byte[] mastersecret;
	private byte[] clientrandom;
	private byte[] serverrandom;

	/**
	 * Decode the server keys and check the certificate
	 * @param bytes server keys
	 * @param clientrandom clientrandom
	 * @param serverrandom serverrandom
	 * @param servercertificate servercertificate
	 * @throws TLSException
	 */
	public void serverKeyExchange(byte[] bytes, byte[] clientrandom, byte[] serverrandom,JAPCertificate servercertificate) throws TLSException
	{
		this.clientrandom = clientrandom;
		this.serverrandom = serverrandom;
		int counter  = 0;
		BigInteger dh_p;
		BigInteger dh_g;
		BigInteger dh_ys;
		byte[] dummy;
		byte[] b=helper.copybytes(bytes,counter,2);
		counter+=2;
		int length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter,length);
		counter+=length;
		dh_p = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_P = "+dh_p.toString());

		b=helper.copybytes(bytes,counter,2);
		counter+=2;
		length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter,length);
		counter+=length;
		dh_g = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_G = "+dh_g.toString());

		b=helper.copybytes(bytes,counter,2);
		counter+=2;
		length =((b[0] & 0xFF) <<8)|(b[1] & 0xFF);
		dummy = helper.copybytes(bytes,counter,length);
		counter+=length;
		dh_ys = new BigInteger(1,dummy);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[SERVER_KEY_EXCHANGE] DH_Ys = "+dh_ys.toString());

		dhparams = new DHParameters(dh_p,dh_g);
		dhserverpub = new DHPublicKeyParameters(dh_ys,dhparams);


		//-----------------------------------------

		byte[] serverparams = helper.copybytes(bytes,0,counter);

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
			byte[] hash = helper.copybytes(bytes,counter+2,bytes.length-counter-2);
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
	public void serverFinished(byte[] b) throws TLSException {
		//TODO : server finished auswerten
	}

	/**
	 * generates the client key exchange message (see RFC2246)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public byte[] clientKeyExchange() throws TLSException {
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);

		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters)pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters)pair.getPrivate();

		DHBasicAgreement dha = new DHBasicAgreement();
		dha.init(dhpriv);
		this.premastersecret = dha.calculateAgreement(dhserverpub).toByteArray();
		if(this.premastersecret[0]==0)
		{
			this.premastersecret = helper.copybytes(this.premastersecret,1,this.premastersecret.length-1);
		}
		PRF prf = new PRF(this.premastersecret,MASTERSECRET,helper.conc(clientrandom,serverrandom));
		this.mastersecret = prf.calculate(48);
		this.premastersecret = null;
		return  dhpub.getY().toByteArray();
	}

	/**
	 * generate the client finished message (see RFC2246)
	 * @param handshakemessages all handshakemessages that have been send before this
	 * @return client finished message
	 */
	public byte[] clientFinished(byte[] handshakemessages) throws TLSException {
		PRF prf = new PRF(this.mastersecret,FINISHEDLABEL,helper.conc(hash.md5(new byte[][]{handshakemessages}),hash.sha(new byte[][]{handshakemessages})));
		return prf.calculate(12);
	}

	/**
	 * calculates the key material (see RFC2246 TLS Record Protocoll)
	 * @return key material
	 */
	public byte[] calculateKeys() {
		PRF prf = new PRF(this.mastersecret,KEYEXPANSION,helper.conc(this.serverrandom,this.clientrandom));
		return prf.calculate(MAXKEYMATERIALLENGTH);
	}

}
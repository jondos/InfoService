package tor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import tor.crypto.CTRBlockCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.encoders.Hex;

import tor.tinytls.util.hash;
import tor.cells.Cell;
import tor.cells.CreateCell;
import tor.cells.RelayCell;
import tor.ordescription.ORDescription;
import tor.util.helper;
import logging.*;
/**
 * @author stefan
 *
 */
public class OnionRouter {

	private final byte[] SAFEPRIME= Hex.decode(	"00FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08"+
 																								"8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B"+
																				    			"302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9"+
     																							"A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6"+
																				    			"49286651ECE65381FFFFFFFFFFFFFFFF");
	private DHParameters dhparams;
	private ORDescription description;
	private DHBasicAgreement dhe;
	private byte[] keyKf;
	private byte[] keyKb;
	private CTRBlockCipher encryptionEngine;
	private CTRBlockCipher decryptionEngine;
	private OnionRouter nextOR;
	private int circID;
	private SHA1Digest digestDf;
	private SHA1Digest digestDb;
	private boolean extended;

	public OnionRouter(int circID,ORDescription description) throws IOException
	{
		this.description = description;
		this.circID = circID;
		this.dhparams = new DHParameters(new BigInteger(this.SAFEPRIME),new BigInteger(new byte[]{2}));
		this.nextOR = null;
		this.extended = false;
	}

	public ORDescription getDescription()
	{
		return this.description;
	}

	public RelayCell encryptCell(RelayCell cell)
	{
		RelayCell c;;
		if(this.nextOR != null)
		{
			c = this.nextOR.encryptCell(cell);
		} else
		{
			c = cell;
			c.generateDigest(this.digestDf);
			System.out.print("gesendet :");
			for(int i=0;i<c.getCellData().length;i++)
			{
				System.out.print(" "+c.getCellData()[i]);
			}
			System.out.println();
		}
		c.doCryptography(this.encryptionEngine);
		c = cell;
		System.out.print("gesendet :");
		for(int i=0;i<c.getCellData().length;i++)
		{
			System.out.print(" "+c.getCellData()[i]);
		}
		System.out.println();
		return c;
	}

	public RelayCell decryptCell(RelayCell cell) throws Exception
	{
		RelayCell c = cell;
		c.doCryptography(this.decryptionEngine);
		if(this.nextOR != null)
		{
			c = this.nextOR.decryptCell(c);
		} else
		{
			c.checkDigest(this.digestDb);
		}
		return c;
	}


	public CreateCell createConnection() throws Exception
	{
		CreateCell cell = new CreateCell(this.circID);
		cell.setPayload(this.createExtendPayload());
		return cell;
	}

	public void checkCreatedCell(Cell cell) throws Exception
	{
		this.checkExtendParameters(helper.copybytes(cell.getPayload(),0,148));
	}

	private RelayCell extendConnection(String address,int port) throws IOException,InvalidCipherTextException
	{
		RelayCell cell;
		byte[] payload = InetAddress.getByName(address).getAddress();
		payload = helper.conc(payload,helper.inttobyte(port,2));
		payload = helper.conc(payload,this.createExtendPayload());
		cell = new RelayCell(this.circID,RelayCell.RELAY_EXTEND,0,payload);
		return cell;
	}

	public RelayCell extendConnection(ORDescription description) throws IOException,InvalidCipherTextException
	{
		RelayCell cell;
		if(this.nextOR==null)
		{
			this.nextOR = new OnionRouter(this.circID,description);
			cell = this.nextOR.extendConnection(description.getAddress(),description.getPort());
			cell.generateDigest(this.digestDf);
		} else
		{
			cell = this.nextOR.extendConnection(description);
		}
		cell.doCryptography(this.encryptionEngine);
		return cell;
	}

	public void checkExtendedCell(RelayCell cell) throws Exception
	{
		if(this.nextOR==null)
		{
			this.checkExtendParameters(helper.copybytes(cell.getPayload(),11,148));
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Circuit '"+this.circID+"' Extended");
		} else
		{
			cell.doCryptography(this.decryptionEngine);
			if(!this.extended)
			{
				cell.checkDigest(this.digestDb);
				this.extended = true;
			}
			this.nextOR.checkExtendedCell(cell);
		}
	}

	private byte[] createExtendPayload() throws IOException,InvalidCipherTextException
	{
		byte[] rsaencrypted;
		byte[] aesencrypted;

		//generate AES Key and Engine
		SICBlockCipher aes = new SICBlockCipher(new AESFastEngine());
		SecureRandom random = new SecureRandom();
		byte[] keyparam = new byte[16];
		random.nextBytes(keyparam);
		//initialize aes-ctr. with keyparam and an IV=0
		aes.init(true,new ParametersWithIV(new KeyParameter(keyparam),new byte[aes.getBlockSize()]));

		//generate DH Parameters and Agreement
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);
		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters)pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters)pair.getPrivate();
		this.dhe = new DHBasicAgreement();
		this.dhe.init(dhpriv);

		byte[] dhpubY = dhpub.getY().toByteArray();

		if(dhpubY[0]==0)
		{
			dhpubY =helper.copybytes(dhpubY,1,dhpubY.length-1);
		}

		byte[] rsaunencrypted = helper.conc(keyparam,helper.copybytes(dhpubY,0,70));

		//generate RSA encrypted part
		DERInputStream dIn = new DERInputStream(new ByteArrayInputStream(this.description.getOnionKey()));
		RSAPublicKeyStructure key = new RSAPublicKeyStructure(ASN1Sequence.getInstance((DERConstructedSequence)dIn.readObject()));
		AsymmetricBlockCipher rsa = new OAEPEncoding(new RSAEngine());
		BigInteger modulus = key.getModulus();
		BigInteger exponent = key.getPublicExponent();
		rsa.init(true,new RSAKeyParameters(false,modulus,exponent));

		rsaencrypted = rsa.processBlock(rsaunencrypted,0,rsaunencrypted.length);

		aesencrypted = helper.conc(helper.copybytes(dhpubY,70,dhpubY.length-70),new byte[aes.getBlockSize()- ( (dhpubY.length-70) % aes.getBlockSize())]);

		//gernerate AES encrypted part
		for(int i=0;i<aesencrypted.length;i+=aes.getBlockSize())
		{
			aes.processBlock(aesencrypted,i,aesencrypted,i);
		}
		return helper.conc(rsaencrypted,aesencrypted);
	}

	private void checkExtendParameters(byte[] param) throws Exception
	{
		DHPublicKeyParameters dhserverpub;
		dhserverpub = new DHPublicKeyParameters(new BigInteger(helper.conc(new byte[]{0},helper.copybytes(param,0,128))),dhparams);
		byte[] agreement = this.dhe.calculateAgreement(dhserverpub).toByteArray();
		if(agreement[0]==0)
		{
			agreement = helper.copybytes(agreement,1,agreement.length-1);
		}
		byte[] kh = hash.sha(new byte[][]{agreement,new byte[]{0x00}});
		for(int i=0;i<kh.length;i++)
		{
			if(kh[i]!=param[i+128])
			{
				throw new Exception("wrong derivative key");
			}
		}
		byte[] keydata = 	helper.conc(hash.sha(new byte[][]{agreement,new byte[]{0x01}}),
											helper.conc(hash.sha(new byte[][]{agreement,new byte[]{0x02}}),
											helper.conc(hash.sha(new byte[][]{agreement,new byte[]{0x03}}),
																	hash.sha(new byte[][]{agreement,new byte[]{0x04}}))));
		this.digestDf = new SHA1Digest();
		this.digestDf.reset();
		this.digestDf.update(keydata,0,20);
		this.digestDb = new SHA1Digest();
		this.digestDb.reset();
		this.digestDb.update(keydata,20,20);
		this.keyKf = helper.copybytes(keydata,40,16);
		this.keyKb = helper.copybytes(keydata,56,16);
		this.decryptionEngine = new CTRBlockCipher(new AESFastEngine());
		this.decryptionEngine.init(true,new ParametersWithIV(new KeyParameter(this.keyKb),new byte[this.decryptionEngine.getBlockSize()]));
		this.encryptionEngine = new CTRBlockCipher(new AESFastEngine());
		this.encryptionEngine.init(false,new ParametersWithIV(new KeyParameter(this.keyKf),new byte[this.encryptionEngine.getBlockSize()]));
	}

}

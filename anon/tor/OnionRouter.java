package anon.tor;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;

import anon.tor.crypto.CTRBlockCipher;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.encoders.Hex;

import anon.tor.tinytls.util.hash;
import anon.tor.cells.Cell;
import anon.tor.cells.CreateCell;
import anon.tor.cells.RelayCell;
import anon.tor.ordescription.ORDescription;
import anon.tor.util.helper;
import logging.*;
import anon.crypto.*;
import java.io.*;
import org.bouncycastle.asn1.*;
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
	private DHParameters m_dhparams;
	private ORDescription m_description;
	private DHBasicAgreement m_dhe;
	private byte[] m_keyKf;
	private byte[] m_keyKb;
	private CTRBlockCipher m_encryptionEngine;
	private CTRBlockCipher m_decryptionEngine;
	private OnionRouter m_nextOR;
	private int m_circID;
	private SHA1Digest m_digestDf;
	private SHA1Digest m_digestDb;
	private boolean m_extended;

	/**
	 * Constructor
	 * @param circID
	 * circID of the circuit where it is used
	 * @param description
	 * ORDescription of the onionrouter
	 * @throws IOException
	 */
	public OnionRouter(int circID,ORDescription description) throws IOException
	{
		this.m_description = description;
		this.m_circID = circID;
		this.m_dhparams = new DHParameters(new BigInteger(this.SAFEPRIME),new BigInteger(new byte[]{2}));
		this.m_nextOR = null;
		this.m_extended = false;
	}

	/**
	 * returns a description of this router
	 * @return
	 * ORDescription
	 */
	public ORDescription getDescription()
	{
		return this.m_description;
	}

	/**
	 * encrypts a RelayCell
	 * @param cell
	 * unencrypted cell
	 * @return
	 * encrypted cell
	 */
	public synchronized RelayCell encryptCell(RelayCell cell)
	{
		RelayCell c;
		if(this.m_nextOR != null)
		{
			c = this.m_nextOR.encryptCell(cell);
		} else
		{
			c = cell;
			c.generateDigest(this.m_digestDf);
		}
		c.doCryptography(this.m_encryptionEngine);
		c = cell;
		byte[]b=c.getCellData();
		String s="";
		for(int i=0;i<b.length;i++)
		{
			s+=b[i]+",";
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"Tor sent: "+s);
		return c;
	}

	/**
	 * decrypts a RelayCell
	 * @param cell
	 * encrypted cell
	 * @return
	 * decrypted cell
	 * @throws Exception
	 */
	public synchronized RelayCell decryptCell(RelayCell cell) throws Exception
	{
		RelayCell c = cell;
		c.doCryptography(this.m_decryptionEngine);
		if(this.m_nextOR != null)
		{
			c = this.m_nextOR.decryptCell(c);
		} else
		{
			c.checkDigest(this.m_digestDb);
		}
		return c;
	}

	/**
	 * create cell
	 *
	 * this cell is needed to connect to the first OR.
	 * after the connection is established extendConnection is used
	 * @return
	 * createCell
	 * @throws Exception
	 */
	public CreateCell createConnection() throws Exception
	{
		CreateCell cell = new CreateCell(this.m_circID);
		cell.setPayload(this.createExtendPayload());
		return cell;
	}

	/**
	 * checks the created cell if the answer was right
	 * @param cell
	 * createdcell
	 * @throws Exception
	 */
	public void checkCreatedCell(Cell cell) throws Exception
	{
		byte[] a = new byte[148];
		System.arraycopy(cell.getPayload(),0,a,0,148);
		this.checkExtendParameters(a);
	}

	/**
	 * extends the connection to another OR
	 * @param address
	 * address of the OR
	 * @param port
	 * port
	 * @return
	 * RelayCell with the data that is needed to connect to the new OR
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	private RelayCell extendConnection(String address,int port) throws IOException,InvalidCipherTextException
	{
		RelayCell cell;
		
		byte[] payload = InetAddress.getByName(address).getAddress();
		payload = helper.conc(payload,helper.inttobyte(port,2));
		payload = helper.conc(payload,this.createExtendPayload());

		MyRSAPublicKey key=m_description.getSigningKey();
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		DEROutputStream dout=new DEROutputStream(out);
		dout.writeObject(key.getAsSubjectPublicKeyInfo().getPublicKey());
		dout.flush();
		byte[] b = out.toByteArray();
		byte[] hash1 = hash.sha(new byte[][]{b});

		payload = helper.conc(payload,hash1);

		cell = new RelayCell(this.m_circID,RelayCell.RELAY_EXTEND,0,payload);
		return cell;
	}

	/**
	 * extends the connction to another OR and encrypts the data
	 * @param description
	 * ORDescription
	 * @return
	 * cell that is needed to extend the connection
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	public RelayCell extendConnection(ORDescription description) throws IOException,InvalidCipherTextException
	{
		RelayCell cell;
		if(this.m_nextOR==null)
		{
			this.m_nextOR = new OnionRouter(this.m_circID,description);
			cell = this.m_nextOR.extendConnection(description.getAddress(),description.getPort());
			cell.generateDigest(this.m_digestDf);
		} else
		{
			cell = this.m_nextOR.extendConnection(description);
		}
		cell.doCryptography(this.m_encryptionEngine);
		return cell;
	}

	/**
	 * checks if the extendedcell has the right parameters and hash
	 * @param cell
	 * cell
	 * @throws Exception
	 */
	public void checkExtendedCell(RelayCell cell) throws Exception
	{
		if(this.m_nextOR==null)
		{
			byte[] a = new byte[148];
			System.arraycopy(cell.getPayload(),11,a,0,148);
			this.checkExtendParameters(a);
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Circuit '"+this.m_circID+"' Extended");
		} else
		{
			cell.doCryptography(this.m_decryptionEngine);
			if(!this.m_extended)
			{
				cell.checkDigest(this.m_digestDb);
				this.m_extended = true;
			}
			this.m_nextOR.checkExtendedCell(cell);
		}
	}

	/**
	 * creates the payload for a extendcell
	 * @return
	 * payload
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	private byte[] createExtendPayload() throws IOException,InvalidCipherTextException
	{
		byte[] rsaencrypted;
		byte[] aesencrypted;
		byte[] a;

		//generate AES Key and Engine
		CTRBlockCipher aes = new CTRBlockCipher(new AESFastEngine());
		SecureRandom random = new SecureRandom();
		byte[] keyparam = new byte[16];
		random.nextBytes(keyparam);
		//initialize aes-ctr. with keyparam and an IV=0
		aes.init(true,new ParametersWithIV(new KeyParameter(keyparam),new byte[aes.getBlockSize()]));

		//generate DH Parameters and Agreement
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), this.m_dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);
		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters)pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters)pair.getPrivate();
		this.m_dhe = new DHBasicAgreement();
		this.m_dhe.init(dhpriv);

		byte[] dhpubY = dhpub.getY().toByteArray();

		if(dhpubY[0]==0)
		{
			a = new byte[dhpubY.length-1];
			System.arraycopy(dhpubY,1,a,0,a.length);
			dhpubY = a;
		}

		a = new byte[70];
		System.arraycopy(dhpubY,0,a,0,70);
		byte[] rsaunencrypted = helper.conc(keyparam,a);

		AsymmetricBlockCipher rsa = new OAEPEncoding(new RSAEngine());
		rsa.init(true,m_description.getOnionKey().getParams());

		rsaencrypted = rsa.processBlock(rsaunencrypted,0,rsaunencrypted.length);
		a=new byte[dhpubY.length-70];
		System.arraycopy(dhpubY,70,a,0,a.length);

		aesencrypted = new byte[a.length];

		//generate AES encrypted part
		aes.processBlock(a,0,aesencrypted,0,a.length);
		byte[] temp=helper.conc(rsaencrypted,aesencrypted);

		return temp;
	}

	/**
	 * checks the parameters of a extend cell and calculate the secrets
	 * @param param
	 * parameters
	 * @throws Exception
	 */
	private void checkExtendParameters(byte[] param) throws Exception
	{
		DHPublicKeyParameters dhserverpub;
		byte[] a = new byte[128];
		System.arraycopy(param,0,a,0,128);
		dhserverpub = new DHPublicKeyParameters(new BigInteger(helper.conc(new byte[]{0},a)),this.m_dhparams);
		byte[] agreement = this.m_dhe.calculateAgreement(dhserverpub).toByteArray();
		if(agreement[0]==0)
		{
			a = new byte[agreement.length-1];
			System.arraycopy(agreement,1,a,0,a.length);
			agreement = a;
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
		this.m_digestDf = new SHA1Digest();
		this.m_digestDf.reset();
		this.m_digestDf.update(keydata,0,20);
		this.m_digestDb = new SHA1Digest();
		this.m_digestDb.reset();
		this.m_digestDb.update(keydata,20,20);
		a = new byte[16];
		System.arraycopy(keydata,40,a,0,16);
		this.m_keyKf = a;
		a = new byte[16];
		System.arraycopy(keydata,56,a,0,16);
		this.m_keyKb = a;
		this.m_decryptionEngine = new CTRBlockCipher(new AESFastEngine());
		this.m_decryptionEngine.init(true,new ParametersWithIV(new KeyParameter(this.m_keyKb),new byte[this.m_decryptionEngine.getBlockSize()]));
		this.m_encryptionEngine = new CTRBlockCipher(new AESFastEngine());
		this.m_encryptionEngine.init(false,new ParametersWithIV(new KeyParameter(this.m_keyKf),new byte[this.m_encryptionEngine.getBlockSize()]));
	}

}

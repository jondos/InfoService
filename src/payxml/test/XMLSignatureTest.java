package payxml.test;
import payxml.*;

// Bouncycastle Crypto imports
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.generators.*;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.params.*;

import java.math.BigInteger;
import java.security.SecureRandom;


class XMLSignatureTest
{
    public static void main(String[] args) {

	XMLSignature sig = null;

	// this is our xml sample
	String myXMLString =
	    "<?xml version=\"1.0\"?>\n"+
	    "  <body>\n"+
	    "    <any>xml</any>\n"+
	    "    <example>document</example>\n"+
	    "  </body>\n";
	System.out.print("Our Sample:\n"+myXMLString+"\n\n\n");

	// generate rsa key pair
	System.out.print("Generating key pair. Plz standby ... ");
	RSAKeyPairGenerator  pGen = new RSAKeyPairGenerator();
	RSAKeyGenerationParameters  genParam =
	    new RSAKeyGenerationParameters(BigInteger.valueOf(0x11), new SecureRandom(), 512, 25);
	pGen.init(genParam);
	AsymmetricCipherKeyPair  pair = pGen.generateKeyPair();

	RSAPrivateCrtKeyParameters privkey = (RSAPrivateCrtKeyParameters)pair.getPrivate();
	RSAKeyParameters pubkey = (RSAKeyParameters)pair.getPublic();
	System.out.println("[Ok]");

	// sign our xml sample
	String signedXML="AIJ";
	try {
	    sig = new XMLSignature(myXMLString.getBytes());
	    System.out.println("sign1");
	    sig.initSign( privkey );
	    System.out.println("sign2");
	    signedXML = sig.signXML();
	    System.out.println("sign3");
	}
	catch(java.security.SignatureException e) {
	    e.printStackTrace();
	}
	catch(java.lang.Exception e) {
	    e.printStackTrace();
	}

	System.out.print("Our Sample signed:\n"+signedXML+"\n\n\n");

    	// check signature
	String mySignedXMLString;
	try {
	    sig = new XMLSignature( signedXML.getBytes() );
	    sig.initVerify( pubkey );
	    if( sig.verifyXML() )
		System.out.println("Signature OK :-)");
	    else
		System.out.println("Signature invalid!!!!!!!!!!!!!!!!!!!!!!");
	}
	catch(java.security.InvalidKeyException e) {
	    e.printStackTrace();
	}
	catch(java.lang.Exception e) {
	    e.printStackTrace();
	}
    }

}

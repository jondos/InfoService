package infoservice;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.GregorianCalendar;

import anon.crypto.DSAKeyPair;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
public class KeyGenTest
{
	private static String ms_strInfoServiceName="InfoService";


	public static void generateKeys() throws IOException
	{
		PKCS12 ownCertificate;
		String strPasswd="";
		System.out.print("Please enter a name for the InfoService: ");
		BufferedReader din=new BufferedReader(new InputStreamReader(System.in));
		ms_strInfoServiceName=din.readLine();
		System.out.print("Please enter a password to protect the private key of the InfoService: ");
		strPasswd=din.readLine();
		System.out.println("Key generation started!");
		DSAKeyPair keyPair = DSAKeyPair.getInstance(new SecureRandom(), 1024, 80);
		FileOutputStream out1 = new FileOutputStream("private.pfx");
		FileOutputStream out2 = new FileOutputStream("public.cer");
		ownCertificate = new PKCS12(new X509DistinguishedName(ms_strInfoServiceName), keyPair, new Validity(new GregorianCalendar(),5));
		ownCertificate.store(out1, strPasswd.toCharArray());
		ownCertificate.getX509Certificate().store(out2);
		out1.close();
		out2.close();
		System.out.println("Key generation finished!");
	}
}

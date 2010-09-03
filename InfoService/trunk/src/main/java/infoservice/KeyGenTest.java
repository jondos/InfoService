package infoservice;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import anon.crypto.DSAKeyPair;
import anon.crypto.PKCS10CertificationRequest;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509Extensions;
import anon.crypto.X509SubjectKeyIdentifier;

public class KeyGenTest
{

	public static void generateKeys(String isName, String passwd) throws IOException
	{
		PKCS12 ownCertificate;
		String strInfoServiceName;
		String strCountry = "";
		String strOrganization = "";
		String strEMail = "";
		
		if (isName != null && isName.length() > 0)
		{
			strInfoServiceName = isName;
		}
		else
		{
			strInfoServiceName = "";
			while (strInfoServiceName.trim().length() == 0)
			{
				System.out.println("Please enter a name for the InfoService: ");
				BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
				strInfoServiceName = din.readLine();
			}
		}

		while (strCountry.trim().length() != 2)
		{
			System.out.println("Please enter the ISO 2 country code of the country where your InfoService is located: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strCountry = din.readLine();
		}
		
		while (strOrganization.trim().length() == 0)
		{
			System.out.println("Please enter the name of your organization: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strOrganization = din.readLine();
		}
		
		while (strEMail.trim().length() == 0)
		{
			System.out.println("Please enter the e-mail address of your organization: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strEMail = din.readLine();
		}
		
		
		String strPasswd = "";
		if (passwd == null)
		{
			
			/*
			System.out.println("Please enter a password to protect the private key of the InfoService: ");
			BufferedReader din = new BufferedReader(new InputStreamReader(System.in));
			strPasswd = din.readLine();
			*/
		}
		else
		{
			strPasswd = passwd;
		}
		System.out.println("Key generation started!");
		DSAKeyPair keyPair = DSAKeyPair.getInstance(new SecureRandom(), 1024, 80);
		FileOutputStream out1 = new FileOutputStream("private.pfx");
		FileOutputStream out2 = new FileOutputStream("public.cer");
		FileOutputStream out3 = new FileOutputStream("public.csr");
		X509Extensions extensions = new X509Extensions(new X509SubjectKeyIdentifier(keyPair.getPublic()));
		Hashtable<String, String> hashtable = new Hashtable<String, String>();
		hashtable.put(X509DistinguishedName.IDENTIFIER_CN, strInfoServiceName);
		hashtable.put(X509DistinguishedName.IDENTIFIER_C, strCountry);
		hashtable.put(X509DistinguishedName.IDENTIFIER_E, strEMail);
		hashtable.put(X509DistinguishedName.IDENTIFIER_O, strOrganization);
		
		
		X509DistinguishedName dn = new X509DistinguishedName(hashtable);
		ownCertificate = new PKCS12(dn, keyPair,
									new Validity(new GregorianCalendar(), 5), extensions);
		PKCS10CertificationRequest csrInfo = new PKCS10CertificationRequest(dn, keyPair, extensions);
		
		
		ownCertificate.store(out1, strPasswd.toCharArray());
		ownCertificate.getX509Certificate().store(out2);
		csrInfo.toOutputStream(out3, true);
		
		out1.close();
		out2.close();
		out3.close();
		System.out.println("Key generation finished!");
	}
}

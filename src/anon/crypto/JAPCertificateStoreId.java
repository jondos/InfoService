package anon.crypto;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DERBitString;
import java.util.Enumeration;

public class JAPCertificateStoreId
{
		private String id;	// using signature, hashcode of the certificate leads nowhere ... 
			
		public static String getId(JAPCertificate cert)
		{
			String issuer = "";
			Enumeration enum = cert.getIssuer().getValues().elements();
			while (enum.hasMoreElements())
			{
				issuer = issuer + (String) enum.nextElement();
			}
			return issuer + cert.getStartDate().toGMTString() + cert.getEndDate().toGMTString();
			// return cert.getSignature();
		} 

		
}
	
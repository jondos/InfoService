/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.crypto; 

import java.util.Date;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.security.SecureRandom;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;

/**
 * This class implements the generation of a DSA key pair.
 */
public class JAPDsaKeyGenerator {
  
  /**
   * Stores the DSA public key.
   */
  private MyDSAPublicKey m_dsaPublicKey;
  
  /**
   * Stores the DSA private key.
   */
  private MyDSAPrivateKey m_dsaPrivateKey;
  
  /**
   * This creates a new instance of JAPDsaKeyGenerator. It is exactly one DSA key pair generated.
   * The keys can be obtained by a call of getDsaPublicKey() or getDsaPrivateKey().
   *
   * @param a_secureRandom A random number as initialization for the key generator.
   * @param a_keyLength The length of the key in bits. For the current implementation of
   *                    bouncycastle it must be a number between 512 and 1024 which is a multiple
   *                    of 64.
   * @param a_keyParamCertainity Measure of robustness of prime. For FIPS 186-2 compliance this
   *                             value should be at least 80.
   */
  public JAPDsaKeyGenerator(SecureRandom a_secureRandom, int a_keyLength, int a_keyParamCertainity) {
    DSAParametersGenerator dsaParametersGenerator = new DSAParametersGenerator();
    dsaParametersGenerator.init(a_keyLength, a_keyParamCertainity, a_secureRandom);
    DSAKeyPairGenerator dsaKeyPairGenerator = new DSAKeyPairGenerator();
    dsaKeyPairGenerator.init(new DSAKeyGenerationParameters(a_secureRandom, dsaParametersGenerator.generateParameters()));
    AsymmetricCipherKeyPair asymmetricCipherKeyPair = dsaKeyPairGenerator.generateKeyPair();
    m_dsaPublicKey = new MyDSAPublicKey((DSAPublicKeyParameters)asymmetricCipherKeyPair.getPublic());
    m_dsaPrivateKey = new MyDSAPrivateKey((DSAPrivateKeyParameters)asymmetricCipherKeyPair.getPrivate());
  }
  
  /**
   * Returns the generated public DSA key.
   *
   * @return The public key.
   */
  public DSAPublicKey getDsaPublicKey() {
    return m_dsaPublicKey;
  }
  
  /**
   * Returns the generated private DSA key.
   *
   * @return The private key.
   */
  public DSAPrivateKey getDsaPrivateKey() {
    return m_dsaPrivateKey;
  }
  
  /**
   * Writes the DSA private key as an PKCS12 certificate to an OutputStream.
   *
   * @param a_outputStream The OutputStream for writing the certificate.
   * @param a_password A password for encrypting the certificate. It must be entered when the
   *                   certificate is loaded.
   * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
   * @param a_validFrom The date from which the certificate is valid.
   * @param a_validTo The date until which the certificate is valid.
   */
  public void storeDsaPrivateKeyAsPkcs12(OutputStream a_outputStream, String a_password, String a_ownerAlias, Date a_validFrom, Date a_validTo) throws IOException {
    X509CertGenerator v3CertGen = new X509CertGenerator();
    v3CertGen.setStartDate(new DERUTCTime(a_validFrom));
    v3CertGen.setEndDate(new DERUTCTime(a_validTo));
    v3CertGen.setSerialNumber(new DERInteger(1));
    v3CertGen.setSubject(new X509Name("CN=" + a_ownerAlias));
    v3CertGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence)(new DERInputStream(new ByteArrayInputStream(m_dsaPublicKey.getEncoded()))).readObject()));
    X509CertificateStructure x509Cert = v3CertGen.sign(new X509Name("CN=" + a_ownerAlias), m_dsaPrivateKey);
    PKCS12 pkcs12 = new PKCS12(a_ownerAlias, m_dsaPrivateKey, x509Cert);
    pkcs12.store(a_outputStream, a_password.toCharArray());
  }
  
  /**
   * Writes the DSA public key as an X509 certificate to an OutputStream.
   *
   * @param a_outputStream The OutputStream for writing the certificate.
   * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
   * @param a_validFrom The date from which the certificate is valid.
   * @param a_validTo The date until which the certificate is valid.
   */
  public void storeDsaPublicKeyAsX509(OutputStream a_outputStream, String a_ownerAlias, Date a_validFrom, Date a_validTo) throws IOException {
    X509CertGenerator v3CertGen = new X509CertGenerator();
    v3CertGen.setStartDate(new DERUTCTime(a_validFrom));
    v3CertGen.setEndDate(new DERUTCTime(a_validTo));
    v3CertGen.setSerialNumber(new DERInteger(1));
    v3CertGen.setSubject(new X509Name("CN=" + a_ownerAlias));
    v3CertGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence)(new DERInputStream(new ByteArrayInputStream(m_dsaPublicKey.getEncoded()))).readObject()));
    X509CertificateStructure x509Cert = v3CertGen.sign(new X509Name("CN=" + a_ownerAlias), m_dsaPrivateKey);
    DEROutputStream derOutputStream = new DEROutputStream(a_outputStream);
    derOutputStream.writeObject(x509Cert);
  }
        
}

    
    
    
package pay;

import payxml.*;
import pay.util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import java.math.*;
import pay.crypto.tinyssl.TinySSL;
import pay.crypto.tinyssl.RootCertificates;
import pay.crypto.Signer;

import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;

/**
 * Hauptklasse für die Verbindung von Pay zur BI kümmert sich inhaltlich um die Kommunikation
 * also In welcher Reihenfolge Die Challenge-Response abläuft etc.
 * @author Grischan Glänzel
 */

public class PayInstance
{
    private String host;
    private int port;
    private Socket socket;
    private HttpClient httpClient;
    private boolean sslOn;

    /**
     * Konstruktor.
     *
     * @param host Hostname der Bezahlinstanz
     * @param port Port der Bezahlinstanz
     * @param sslOn SSL ein?
     */
    public PayInstance(String host, int port, boolean sslOn)
    {
        this.host=host;
        this.port=port;
        this.sslOn=sslOn;
    }

    /**
     * Baut eine Verbindung zur Bezahlinstanz auf.
     *
     * @throws IOException Wenn Fehler beim Verbindungsaufbau
     */
    public void connect() throws IOException
    {
		try{
        	if(sslOn==false) socket = new Socket (host,port);
        	else socket = new TinySSL(host,port);
        	httpClient=new HttpClient(socket);
		}catch(Exception ex){
			Log.log(this,PayText.get("piServerError"),Log.INFO);
		}
    }

    /**
     * Schließt die Verbindung zur Bezahlinstanz.
     *
     * @throws IOException Wenn Fehler beim Verbindungsabbau
     */
    public void disconnect() throws IOException
    {
        httpClient.close();
    }

    /**
     * Aufladen des Kontos mit einer Geldüberweisung.
     *
     * @param accountcert Kontozertifikat
     * @param privKey geheimer Schlüssel des Kontos
     * @return Transfer-Zertifikat
     * @throws IOException
     */
    public XMLTransCert chargeBankTransfer(String accountcert, RSAKeyParameters privKey) throws IOException
    {
        String type="<ChargeMethod>Banktransfer</ChargeMethod>";
        return charge(accountcert, privKey, type);
    }

    /**
     * Aufladen des Kontos mit Kreditkarte.
     *
     * @param accountcert Kontozertifikat
     * @param privKey geheimer Schlüssel
     * @param amount Geldbetrag
     * @param number Kreditkartennummer
     * @param valid Gültigkeitsdatum der Kreditkarte
     * @return Transfer-Zertifikat
     * @throws IOException
     */
    public XMLTransCert chargeCreditCard(String accountcert, RSAKeyParameters privKey, int amount, String number, Date valid) throws IOException
    {
        String type="<ChargeMethod>Creditcard<CreditCard><Number>"+
            number+"</Number><ValidDate>"+valid+"</ValidDate>"+
            "</CreditCard><Amount>"+amount+"</Amount></ChargeMethod>";
        return charge(accountcert, privKey, type);
    }

    private XMLTransCert charge(String accountcert, RSAKeyParameters privKey, String type) throws IOException
    {
        try
        {
            authenticate(accountcert,privKey);
            httpClient.writeRequest("POST","charge", type);
            String answer = httpClient.readAnswer();

            XMLTransCert xmltrcert = new XMLTransCert(answer.getBytes());

            RootCertificates rootCerts = new RootCertificates();
            rootCerts.init();
            RSAKeyParameters testkey = rootCerts.getPublicKey("test server");
            XMLSignature xmlSig = new XMLSignature(xmltrcert.getXMLString(false).getBytes());
            xmlSig.initVerify(testkey);
            if (xmlSig.verifyXML())
            {
                return xmltrcert;
            }
            else throw new Exception("invalid signature");
        }
        catch(Exception e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Liefert den Kontostand zum angebenen  Konto.
     *
     * @param accountcert Kontozertifikat
     * @param privKey geheimer Schlüssel des Kontos
     * @return Guthaben und Kostenbestätigungen (in XMLBalConf gekapselt)
     * @throws IOException
     */
    public XMLBalConf getBalance(String accountcert, RSAKeyParameters privKey) throws IOException
    {
        //XMLBalance xmlBalance;
        //XMLCostConfirmation[] xmlConfirms;
        String answer;
        XMLBalConf conf = null;
        try
        {
            authenticate(accountcert,privKey);
            httpClient.writeRequest("GET","balance", null);
            answer = httpClient.readAnswer();
			conf = new XMLBalConf(answer);

            // passiert jetzt in XMLBalConf
            /*
            int index = answer.indexOf(CostConfirmations.docStartTag);
            if (index>0)
            {
                xmlBalance = new XMLBalance(answer.substring(0,index));

                String confirms=answer.substring(index);
                int begin, end;
                Vector vector = new Vector();
                while((begin=confirms.indexOf("<CostConfirmation>"))!=-1)
                {
                    if((end=confirms.indexOf("</CostConfirmation>"))>begin)
                    {
                        vector.add(new XMLCostConfirmation(confirms.substring(begin,end+19)));
                    }
                }
                if(vector.isEmpty()) xmlConfirms=null;
                else xmlConfirms = (XMLCostConfirmation[]) vector.toArray();
            }
            else
            {
                xmlBalance = new XMLBalance(answer);
                xmlConfirms=null;
            }
            */

            RootCertificates rootCerts = new RootCertificates();
            rootCerts.init();
            RSAKeyParameters testkey = rootCerts.getPublicKey("test server");

            XMLSignature sig = new XMLSignature(conf.balance.getXMLString(false).getBytes());
            sig.initVerify(testkey);
            if (!sig.verifyXML()) throw new Exception("invalid signature");
        }
        catch(Exception e)
        {
            throw new IOException(e.getMessage());
        }
        return conf;
    }

    private void authenticate(String accountcert, RSAKeyParameters privKey) throws Exception
    {
        try
        {
			//Log.log(this,accountcert,Log.TEST);
            httpClient.writeRequest("POST","authenticate",accountcert);
            String answer = httpClient.readAnswer();
            XMLChallenge xmlchallenge=new XMLChallenge(answer);
            byte[] challenge = xmlchallenge.getChallenge();

            Signer signer = new Signer();
            signer.init(true, privKey);
            signer.update(challenge);
            byte[] sig = signer.generateSignature();

            XMLResponse xmlResponse = new XMLResponse(sig);
            String response = xmlResponse.getXMLString(true);

            httpClient.writeRequest("POST","response",response);
            answer = httpClient.readAnswer();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }

    }
/*
	public AICert connectAI(long accountNumber,RSAKeyParameters pubKey) throws Exception
    {
		try{
        	AIHello xmlhello = new AIHello(accountNumber, new XMLJapPublicKey(pubKey));
			//PrintWriter pw = new PrintWriter(socket.getOutputStream());
  			//pw.print(pay.test.PayAITest.head+"hello"); pw.flush();
        	//httpClient.writeRequest("GET","hello","");//xmlhello.getXMLString(true));
        	//String answer = httpClient.readAnswer();
		}
		catch(Exception ex){
						System.out.println("PayInstance: Fehler in connectAI");
		}

        XMLChallenge xmlchallenge=new XMLChallenge(answer);
        byte[] challenge = xmlchallenge.getChallenge();

        Signer signer = new Signer();
        signer.init(true, privKey);
        signer.update(challenge);
        byte[] sig = signer.generateSignature();

        XMLResponse xmlResponse = new XMLResponse(sig);
        String response = xmlResponse.getXMLString(true);

        httpClient.writeRequest("POST","response",response);
        answer = httpClient.readAnswer();

        RootCertificates rootCerts = new RootCertificates();
        rootCerts.init();
        RSAKeyParameters testkey = rootCerts.getPublicKey("test server");

        XMLCertificate xmlCert = new XMLCertificate(answer);
        XMLSignature xmlSig = new XMLSignature(answer.getBytes());
        xmlSig.initVerify(testkey);
        if (xmlSig.verifyXML()&&
                xmlCert.getPublicKey().getModulus().equals(pubKey.getModulus())&&
                xmlCert.getPublicKey().getPublicExponent().equals(pubKey.getExponent()))
        {
            return xmlCert;
        }
        else throw new Exception("wrong signatur on accountcertificate or wrong key");
 		return new AICert();
    }
*/
    /**
     * Eröffnet ein neues Konto bei der Bezahlinstanz.
     *
     * @param pubKey öffentlicher Schlüssel
     * @param privKey geheimer Schlüssel
     * @return Kontozertifikat
     * @throws Exception
     */
    public XMLCertificate register(RSAKeyParameters pubKey, RSAKeyParameters privKey) throws Exception
    {
        XMLJapPublicKey xmlPubKey = new XMLJapPublicKey(pubKey);
        String xmlkey = xmlPubKey.getXMLString(true);
        if (xmlkey == null) return null;

        httpClient.writeRequest("POST","register",xmlkey);
        String answer = httpClient.readAnswer();

        XMLChallenge xmlchallenge=new XMLChallenge(answer);
        byte[] challenge = xmlchallenge.getChallenge();

        Signer signer = new Signer();
        signer.init(true, privKey);
        signer.update(challenge);
        byte[] sig = signer.generateSignature();

        XMLResponse xmlResponse = new XMLResponse(sig);
        String response = xmlResponse.getXMLString(true);

        httpClient.writeRequest("POST","response",response);
        answer = httpClient.readAnswer();

        RootCertificates rootCerts = new RootCertificates();
        rootCerts.init();
        RSAKeyParameters testkey = rootCerts.getPublicKey("test server");

		Log.log(this,answer,Log.SHORT_DEBUG);
        XMLCertificate xmlCert = new XMLCertificate(answer);
        XMLSignature xmlSig = new XMLSignature(answer.getBytes());
        xmlSig.initVerify(testkey);
        if (xmlSig.verifyXML()&&
                xmlCert.getPublicKey().getModulus().equals(pubKey.getModulus())&&
                xmlCert.getPublicKey().getExponent().equals(pubKey.getExponent()))
        {
            return xmlCert;
        }
        else throw new Exception("wrong signatur on accountcertificate or wrong key");

    }
}

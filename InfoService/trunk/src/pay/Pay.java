package pay;
import pay.xml.*;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;

import java.math.*;
import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.security.SecureRandom;

public class Pay
{
    private static Pay pay;
    private PayInstance payInstance;
    private PayAccountFile accountFile;
    private String host;
    private int port; 
    private boolean sslOn;
    private String filename;

    /** 
     * make default constructor private: singleton
     */
    private Pay(){}

    /** 
     * Erzeugt eine Instanz der Klasse Pay 
     * 
     * @return Instanz der Klasse Pay
     */
    public static Pay create()
    {
        if (pay==null)
            pay=new Pay();
        return pay;
    }

    /** 
    * Initialisiert die Pay Klasse. Ruft {@link #setPayInstance} und 
    * {@link #openAccountFile}
    * auf. Die Kontendatei wird eingelesen und entschlüsselt. Hostname und
    * Hostport der Bezahlinstanz werden gesetzt. 
    *  
    * @param filename Dateiname der Kontendatei
    * @param password Passwort zur Ver- bzw. Entschlüsselung der Kontendatei 
    * @param host Hostname der Bezahlinstanz 
    * @param port Hostport der Bezahlinstanz 
    * @param sslOn schaltet SSL ein oder aus (Debug-stuff) 
    * @return -1 wenn noch keine Kontendatei unter filename existiert, 0 sonst 
    * @throws Exception Wenn ein Fehler beim Dateizugriff oder der
    * Entschlüsselung auftrat
    */
    public int init(String filename, String password, String host, int port, boolean sslOn) throws Exception
    {
        setPayInstance(host, port, sslOn);
        return openAccountFile(filename, password);
    }

    /** 
     * Hostname und Hostport der Bezahlinstanz werden gesetzt.
     * 
     * @param host Hostname der Bezahlinstanz 
     * @param port Hostport der Bezahlinstanz
     * @param sslOn SSL ein / aus (Debug-Stuff)
     */
    public void setPayInstance(String host, int port, boolean sslOn)
    {
        this.host=host;
        this.port=port;
        this.sslOn=sslOn;
    }

    /** 
     * Einlesen und entschlüsseln der Kontendatei.
     * 
     * @param filename Dateiname
     * @param password Passwort
     * @return -1 wenn Kontendatei neu angelegt wurde, 0 wenn bestehende 
     * Kontendatei geöffnet wurde.
     * @throws Exception Wenn ein Fehler beim Dateizugriff oder der 
     * Entschlüsselung auftrat.
     */
    public int openAccountFile(String filename, String password) throws Exception
    {
        accountFile = new PayAccountFile();
        try
        {
            return accountFile.init(filename, password);
        }
        catch(Exception e)
        {
            accountFile=null;
            throw e;
        }
    }

    /** 
     * Liefert alle gespeicherten Konten.
     * 
     * @return Enumeration von {@link PayAccount}s
     * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
     */
    public Enumeration getAccounts() throws IllegalStateException
    {
        if(accountFile==null) throw new IllegalStateException("accountfile not set");
        else return accountFile.getAccounts();
    }

    /** 
     * Liefert die Kontoinformation des Kontos mit der angegeben Kontonummer.
     * 
     * @param accountNumber accountnumber
     * @return Kontodaten 
     * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
     */
    public PayAccount getAccount(long accountNumber) throws IllegalStateException
    {
        if(accountFile==null) throw new IllegalStateException("accountfile not set");
        else return accountFile.getAccount(accountNumber);
    }

    /** 
     * Löscht das Konto mit der angebenen Kontonummer.
     * 
     * @param accountNumber Kontonummer
     * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
     */
    public void deleteAccount(long accountNumber) throws IllegalStateException
    {
        try
        {
            accountFile.deleteAccount(accountNumber);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** 
     * Aufladen des Kontos mit angebener Kontonummer.
     * Die Bezahlinstanz wird kontaktiert und ein Kontozertifikat angefragt.
     * Das Kontozertifikat wird den Kontoinformationen hinzugefügt und
     * gespeichert.
     * 
     * @param accountNumber Kontonummer 
     * @return Überweisungsnummer (Transaktionspeudonym) 
     * @throws IllegalStateException Wenn die Kontendatei oder die 
     * Bezahlinstanz nicht gesetzt sind.
     */
    public long chargeAccount(long accountNumber) throws IllegalStateException, IOException
    {
        try
        {
            PayAccount account = accountFile.getAccount(accountNumber);
            payInstance = new PayInstance(host,port,sslOn);
            payInstance.connect();
            XMLTransCert transcert= 
            payInstance.chargeBankTransfer(account.getAccountCertificate(),account.getPrivateKey());
            payInstance.disconnect();
            account.addTransCert(transcert);

            accountFile.storeModifiedAccount(account);
            return transcert.getTransferNumber();
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    /** 
     * Abfrage des Kontostandes des Kontos mit angebener Kontonummer.
     * Die Bezahlinstanz wird kontaktiert und der Kontostand angefragt.
     * Dar übermittelte Kontoschnappschuss und die Kostenbestätigungen werden
     * den Kontoinformationen hinzugefügt und gespeichert.
     * 
     * @param accountNumber Kontonummer
     * @return Kontoschnappschuss und Kostenbestätigungen 
     */
    public XMLBalConf getBalance(long accountNumber) throws IllegalStateException
    {
        try
        {
            XMLBalConf balConf;

            PayAccount account = accountFile.getAccount(accountNumber);
            payInstance = new PayInstance(host,port,sslOn);
            payInstance.connect(); 
            balConf=payInstance.getBalance(account.getAccountCertificate(),account.getPrivateKey());
            payInstance.disconnect();

            account.setBalance(balConf.balance);

            accountFile.storeModifiedAccount(account);
            return balConf; 
        }
        catch (Exception e)
        {
            return null;
        }
    }


    /** 
     * Funktion zur Kontoeröffnung. Ein RSA-Schlüsselpaar wird generiert und
     * der öffentliche Schlüssel an die Bezahlinstanz übermittelt. Das 
     * erhaltene Kontozertifikat wird im neuen Kontodatensatz lokal 
     * gespeichert.
     */
    public void addAccount() throws IllegalStateException
    {
        RSAKeyPairGenerator  pGen = new RSAKeyPairGenerator();
        RSAKeyGenerationParameters  genParam = new RSAKeyGenerationParameters(
                BigInteger.valueOf(0x11), new SecureRandom(), 512, 25);
        pGen.init(genParam);

        AsymmetricCipherKeyPair  pair = pGen.generateKeyPair();
        
        RSAPrivateCrtKeyParameters rsaprivkey = (RSAPrivateCrtKeyParameters)pair.getPrivate();
        RSAKeyParameters rsapubkey = (RSAKeyParameters)pair.getPublic();
        
        try
        {
        payInstance = new PayInstance(host,port,sslOn);
        payInstance.connect();
        XMLCertificate xmlCert=payInstance.register(rsapubkey, rsaprivkey);
        payInstance.disconnect();

        PayAccount newAccount= new PayAccount(xmlCert, rsaprivkey);
        accountFile.storeNewAccount(newAccount);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                payInstance.disconnect();
            }
            catch(Exception ne)
            {
                ne.printStackTrace();
            }
        }
    }
}

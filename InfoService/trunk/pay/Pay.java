package pay;
import payxml.*;
import pay.event.*;
import pay.util.*;
import pay.data.*;

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

/**
 *  Dies ist die Hauptklasse für die Payfunktionaltiät hier werden funktionen zum kommunizieren mit der BI
 *  dem speichern der Konten angeboten.
 *	Pay ist ausserdem ein ModelListener des Pay Bereiches sobald sich etwas an Pay oder den von Pay verwalteten Klassen ändert
 *  wird eine ModelEvent geworfen.
 *	Auf PayAccountFile und PayInstance und PayAccountsControl sollte aus diesem Grund nur über Pay zugegriffen werden.
 *  * @author Andreas Mueller, Grischan Glänzel
 */


public class Pay
{
    private static Pay pay;

    private UserDaten user;

    private PayInstance payInstance;
    private PayAccountFile accountFile;
    private PayAccountsControl accountsControl;
    private String host;
    private int port;
    private boolean sslOn;
    private String filename;
	private boolean connectedWithPi = false;
	private boolean accountFileOpened = false;

	private ModelEventFirerer fire = new ModelEventFirerer(this);


    /**
     * make default constructor private: singleton
     */
    private Pay(){
		accountsControl = new PayAccountsControl();
	}

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
    * Gucken ob die aufrufe von {@link #setPayInstance} und
    * {@link #openAccountFile} erfolgreich absolviert wurden
    *
    * @return true wenn initialisierung erfolgreich verlaufen ist
    */

	 public boolean initDone(){
	 	return (connectedWithPi&&accountFileOpened);
	 }
	 public boolean connectedWithPi(){
	 	 	return connectedWithPi;
	 }
	 public boolean accountFileOpened(){
	 	 	return accountFileOpened;
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
    public void init(String host, int port, boolean sslOn) throws Exception{
        setPayInstance(host, port, sslOn);
        openAccountFile();
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
        connectedWithPi=true;
    }

    /**
     * Einlesen und entschlüsseln der Kontendatei.
     *
     * @param filename Dateiname
     * @param password Passwort
     * @return 0 wenn Kontendatei neu angelegt wurde, 1 wenn bestehende
     * Kontendatei geöffnet wurde. -1 Wenn ein Fehler beim Dateizugriff oder der
     * Entschlüsselung auftrat. 2 wenn dieses AccountFile bereits geöffnet ist
     */
    public void openAccountFile(){
		if(	(accountFile==null)){
			try{
				accountFile = accountsControl.open();
				accountFileOpened=true;
				fire.fireModelEvent();
			}catch(PayAccountsControl.WrongPasswordException wp){
				Log.log(this,"öffnen des AccountFiles ging nicht => falsches Passwort angegeben "+wp,Log.SHORT_DEBUG);
			}
			catch(IOException io){
				Log.log(this,"öffnen des AccountFiles ging nicht => allgemiener IOFehler! "+io,Log.SHORT_DEBUG);
			}
		}
    }

    public boolean accountFileHasUsedAccount(){
		return (accountFileOpened() && accountFile.hasUsedAccount());
	}

    public void storeAccountFile(){
		try{
			accountsControl.store(accountFile);
		}catch(Exception ex){
			Log.log(this,"speichern des Accout FIles ging nicht",Log.SHORT_DEBUG);
		}
	}

    public void changeAccountFileEncryptMode(){
		fire.fireModelEvent();
		accountsControl.changeEncryptMode(accountFile);
	}
	public boolean isAccountFileEncrypted(){
		return accountFile.getPassword()!=null;
	}
    public void exportAccountFile(String filename){
		try{
			PayAccountFile accounts = new PayAccountFile(accountFile.getXML(),accountFile.getPassword());
			accountsControl.store(accounts, filename);
		}catch(IOException io){
			Log.log(this,"exportAccountFile: speichern geht nicht: IOException",Log.INFO);
		}
	}
	public void importAccountFile(String filename){
		Log.log(this,"importAccountFile start",Log.TEST);
		try{
			PayAccountFile accounts = accountsControl.open(filename);
			Enumeration en = accounts.getAccounts();

			while(en.hasMoreElements()){
				Log.log(this,"en.hasMoreElements()",Log.TEST);
				PayAccount account = (PayAccount)en.nextElement();
				if(accountFile.getAccount(account.getAccountNumber())==null){
					try{
						accountsControl.store(accountFile.makeNewAccount(account));
					}
					catch (Exception ex){
						Log.log(this,"account No: "+account.getAccountNumber()+" bereits vorhanden",Log.INFO);
					}
				}
			}
		}catch(Exception ex){
			Log.log(this, "importAccountFile ging nicht");
		}
		fire.fireModelEvent();
	}


	public boolean changeAccountFilePassword(){
		fire.fireModelEvent();
		return accountsControl.changePW(accountFile);
	}


	 /**
	 * Setzt das aktuell zum Bezahlen benutze Konto
	 **/
  	 public void setUsedAccount(long accountNumber){
	 	if(accountFile==null) throw new IllegalStateException("accountfile not set");
	 	try {
		   accountFile.setUsedAccount(accountNumber);
		   accountsControl.store(accountFile);
		   fire.fireModelEvent();
		} catch (Exception ex){
		   Log.log(this,"Setzen des Benutzten Accounts geht nicht",Log.INFO);
		}
	 }
	 /**
	 * Liefert Das aktuell zum Bezahlen benutze Konto
	 **/
	 public long getUsedAccount(){
	 	  if(accountFile==null) throw new IllegalStateException("accountfile not set");
		  return accountFile.getUsedAccount();
	 }

	 /**
     * Liefert alle gespeicherten Konten.
     *
     * @return Vector von {@link PayAccount}s
     * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
     */
	 public Vector getAccountVec(){
		 if(accountFile==null) throw new IllegalStateException("accountfile not set");
   	 else return accountFile.getAccountVec();
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
    public boolean deleteAccount(long accountNumber) throws IllegalStateException
    {
        try
        {
            if(!accountFile.deleteAccount(accountNumber)) return false;
            accountsControl.store(accountFile);
            fire.fireModelEvent();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
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
            accountsControl.store(accountFile.modifyAccount(account));
            fire.fireModelEvent();
            return transcert.getTransferNumber();
        }
        catch (Exception e)
        {
			Log.log(this,"Fehler: "+e.getMessage(),Log.SHORT_DEBUG);
            return -1;
        }
    }
    /**
	 * Für jede in der aktuelle KontoDatei gespeichert Konten wird getBalance aufgerufen
     */

    public void updateAllBalance(){
	    try{
		    Enumeration accounts = getAccounts();
		    while(accounts.hasMoreElements()){
				PayAccount ac = (PayAccount)accounts.nextElement();
				pay.updateBalance(ac.getAccountNumber());
		  	}
		}catch (IllegalStateException ise){
			Log.log(this,"getAllBalance kunktioniert nicht",Log.SHORT_DEBUG);
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
    public XMLBalConf updateBalance(long accountNumber) throws IllegalStateException
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
            account.setCostConfirms(balConf.confirmations);

            accountsControl.store(accountFile.modifyAccount(account));
            fire.fireModelEvent();
            return balConf;
        }
        catch (Exception e)
        {
            return null;
        }
    }
    public XMLBalConf getBalance(long accountNumber) throws IllegalStateException
	{
		try
		{
			PayAccount account = accountFile.getAccount(accountNumber);
			return new XMLBalConf(account.getBalance(),account.getCostConfirms());
		}
		catch (Exception e)
		{
			return null;
		}
    }

    public XMLEasyCC addCosts(String aiName,long accountNumber,long plusCosts) throws IllegalStateException{
			try
			{
				PayAccount account = accountFile.getAccount(accountNumber);
				XMLEasyCC cc = account.getCostConfirms().updateCC(aiName,accountNumber,plusCosts);
				accountsControl.store(accountFile.modifyAccount(account));
				return cc;
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
        accountsControl.store(accountFile.makeNewAccount(newAccount));
        fire.fireModelEvent();

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
	/**
     * Funktion zum hinzufügen eines ModelListeners.
     * An diesen wird ein ModelEvent gefeuert wenn sich das am
     * PayAccountFile Object oder an der speicherung deselben durch PayAccountsControl etwas geändert hat
     *
     */
    public void addModelListener(ModelListener ml){
		fire.addModelListener(ml);
	}
	/**
	 * Funktion zum löschen eines ModelListeners.
	 */
	public void removeModelListener(ModelListener ml){
		fire.removeModelListener(ml);
	}
}

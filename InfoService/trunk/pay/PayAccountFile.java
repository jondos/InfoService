package pay;
import pay.xml.*;
import java.io.*;
import java.util.*;

import pay.crypto.CryptFile;

public class PayAccountFile
{
    protected CryptFile cryptFile;
    protected byte[] accountsBytes;
    protected Vector accounts;
    protected String filename;
    protected String password;

    public PayAccountFile()
    {
        accounts = new Vector();
    }

    /** 
     * Liest die Daten aus der Kontendatei und entschlüsselt sie mit Hilfe
     * des angegeben Passwortes.
     * 
     * @param filename Name der Kontendatei
     * @param password Passwort
     * @return -1 wenn neue Kontendatei erzeugt wurde, 0 wenn vorhandene
     * Kontendatei gelesen wurde
     * @throws Exception Wenn Fehler beim Lesen der Kontendatei, Fehler bei
     * der Entschlüsselung insbesondere falsches Passwort
     */
    public int init(String filename, String password) throws Exception
    {
        this.filename=filename;
        this.password=password;
        try
        {
            cryptFile = new CryptFile();
            accountsBytes = cryptFile.readAndDecrypt(filename, password);
            xmlToVector();
            return 0;
        }
        catch(FileNotFoundException e)
        {
            accountsBytes=null;
            return -1;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    /** 
     * Liefert PayAccount zur angegebenen Kontonummer.
     * 
     * @param accountNumber Kontonummer
     * @return {@link PayAccount} oder null, wenn kein Konto unter der angebenen
     * Kontonummer vorhanden ist
     */
    public PayAccount getAccount(long accountNumber)
    {
        PayAccount tmp;
        Enumeration enum = accounts.elements();
        while(enum.hasMoreElements())
        {
            tmp = (PayAccount) enum.nextElement();
            if (tmp.getAccountNumber()==accountNumber)
                return tmp;
        }
        return null;
    } 

    /** 
     * Löscht das Konto mit der angebenen Kontonummer aus der Kontendatei.
     * 
     * @param accountNumber Kontonummer 
     * @throws Exception Wenn ein Fehler bei Dateizugriff auftrat
     */
    public void deleteAccount(long accountNumber) throws Exception
    {
        PayAccount tmp;
        for(int i=0; i<accounts.size(); i++)
        {
            tmp = (PayAccount) accounts.get(i);
            if (tmp.getAccountNumber()==accountNumber)
            {
                accounts.remove(i);
            }
        }
        store(); 
    } 

    /** 
     * Liefert alle Kontennummern der Kontendatei.
     * 
     * @return Enumeration von Long-Werten
     */
    public Enumeration getAccountNumbers()
    {
        PayAccount tmpAccount;
        Vector tmp = new Vector();
        Enumeration enum = accounts.elements();
        while(enum.hasMoreElements())
        {
            tmpAccount = (PayAccount)enum.nextElement();
            tmp.add(new Long(tmpAccount.getAccountNumber()));
        }
        return tmp.elements();    
    }

    /** 
     * Liefert alle Konten der Kontendatei.
     * 
     * @return Enumeration von {@link PayAccount}s
     */
    public Enumeration getAccounts()
    {
        return accounts.elements();    
    }

    /** 
     * Schreibt ein neues Konto in die Kontendatei.
     * 
     * @param account zu speicherndes Konto 
     * @throws Exception Wenn ein Konto unter der Kontonummer schon in der
     * Kontendatei enthalten ist. Wenn ein Dateizugriffsfehler auftrat.
     */
    public void storeNewAccount(PayAccount account) throws Exception
    {
        PayAccount tmp;
        Enumeration enum = accounts.elements();
        while(enum.hasMoreElements())
        {
            tmp = (PayAccount) enum.nextElement();
            if (tmp.getAccountNumber()==account.getAccountNumber())
                throw new IllegalArgumentException();
        }
        accounts.add(account);
        store(); 
    }

    /** 
     * Schreibt ein geändertes Konto in die Kontodatei.
     * 
     * @param account zu speicherndes Konto 
     * @throws Exception Wenn ein Dateizugriffsfehler auftrat
     */
    public void storeModifiedAccount(PayAccount account) throws Exception
    {
        PayAccount tmp;
        for(int i=0; i<accounts.size(); i++)
        {
            tmp = (PayAccount) accounts.get(i);
            if (tmp.getAccountNumber()==account.getAccountNumber())
            {
                accounts.setElementAt(account,i);
            }
        }
        store(); 

    }

    /** 
     * Verschlüsseln und speichern der Kontodaten in die Kontendatei. 
     * 
     * @throws Exception Wenn ein Dateizugriffsfehler auftrat.
     */
    private void store() throws Exception
    {
        StringBuffer tmp = new StringBuffer();
        tmp.append("<Accounts>");
        Enumeration enum = accounts.elements();

        PayAccount account;
        while(enum.hasMoreElements())
        {
            account = (PayAccount)enum.nextElement();
            tmp.append(account.getXMLString(false));
        }
        tmp.append("</Accounts>");
        System.out.println("tmp: "+tmp);
        cryptFile.encryptAndWrite(filename,password,tmp.toString().getBytes());
    }

    /** 
     * Liest das in accountsBytes gespeicherte XML-Dokument und erzeugt ein 
     * Vector von {@link PayAccount}s.
     * 
     * @throws Exception Wenn das XML-Dokument keine korrekten Kontodaten
     * enthält. Wahrscheinliche Ursachen: Beim Entschlüsseln der Kontendatei
     * ein falsches Passwort verwendet oder die Kontendatei ist fehlerhaft.
     */
    private void xmlToVector() throws Exception
    {
        String sub;
        String tmp = new String(accountsBytes);
        System.out.println(tmp);
        int first, second, off;
        off = 0;

        if(tmp.indexOf("<Accounts>")==-1) 
            throw new Exception("wrong password or corrupt accountfile");

        while((first=tmp.indexOf("<Account>", off))!=-1)
        {
            second = tmp.indexOf("</Account>", off)+10;
            off=second;
            sub = tmp.substring(first,second);
            System.out.println("account: "+sub);
            try
            {
                PayAccount xmlAccount = new PayAccount(sub.getBytes());
                accounts.add(xmlAccount);
            }
            catch(Exception e)
            {
                throw new Exception("wrong password or corrupt accountfile");
            }
        }
    }
}


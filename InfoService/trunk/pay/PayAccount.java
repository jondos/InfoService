package pay;
import pay.xml.*;
import pay.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import java.math.*;
import java.util.*;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;

public class PayAccount extends XMLDocument
{
    private XMLCertificate certificate;
    private XMLBalance balance;
    private RSAPrivateCrtKeyParameters privateKey; 
    private Vector transCerts;
    private Vector costConfirms;

    /** 
     * Erzeugt ein PayAccount Objekt aus einem XML-Dokument.
     * 
     * @param xmlData XML-Dokument 
     * @throws Exception Wenn XML-Dokument fehlerhaft 
     */
    public PayAccount(byte[] xmlData) throws Exception
    {
        transCerts = new Vector();
        costConfirms = new Vector();
        initDocument(xmlData);
        setXMLCertificate();
        setXMLBalance();
        setRSAPrivateKey();
        setXMLTransCert();
        setXMLCostConfirms();
    }

    /** 
     * Erzeugt ein {@link PayAccount} Objekt aus einem Kontozertifikat und dem
     * zugehörigen geheimen Schlüssel.
     * 
     * @param certificate Kontozertifikat
     * @param privateKey geheimer Schlüssel 
     */
    public PayAccount(XMLCertificate certificate, RSAPrivateCrtKeyParameters privateKey)
    {
        this.certificate=certificate;
        this.privateKey=privateKey;
        this.transCerts= new Vector();
        this.costConfirms = new Vector();
    }

    /** 
     * Liefert die XML-Präsentation des Kontos. 
     * 
     * @param withHead Mit XML-Kopf? 
     * @return XML-Dokument als String 
     */
    public String getXMLString(boolean withHead)
    {
        StringBuffer buffer = new StringBuffer(512);
        buffer.append("<Account>"+certificate.getXMLString(false));
        buffer.append("<RSAPrivateKey><Modulus>");
        buffer.append(Base64.encode(privateKey.getModulus().toByteArray()));
        buffer.append("</Modulus><PublicExponent>");
        buffer.append(Base64.encode(privateKey.getPublicExponent().toByteArray()));
        buffer.append("</PublicExponent><PrivateExponent>"); 
        buffer.append(Base64.encode(privateKey.getExponent().toByteArray()));
        buffer.append("</PrivateExponent><P>"); 
        buffer.append(Base64.encode(privateKey.getP().toByteArray()));
        buffer.append("</P><Q>"); 
        buffer.append(Base64.encode(privateKey.getQ().toByteArray()));
        buffer.append("</Q><dP>"); 
        buffer.append(Base64.encode(privateKey.getDP().toByteArray()));
        buffer.append("</dP><dQ>"); 
        buffer.append(Base64.encode(privateKey.getDQ().toByteArray()));
        buffer.append("</dQ><QInv>"); 
        buffer.append(Base64.encode(privateKey.getQInv().toByteArray()));
        buffer.append("</QInv></RSAPrivateKey>"); 

        buffer.append("<TransferCertificates>\n");
        Enumeration enum = transCerts.elements();
        while(enum.hasMoreElements())
        {
            XMLTransCert cert = (XMLTransCert) enum.nextElement();
            buffer.append(cert.getXMLString(false));
        }
        buffer.append("</TransferCertificates>\n");
        
        if(balance!=null)
        {
          buffer.append(balance.getXMLString(false)); 
          buffer.append("<Confirmations>");
          enum = costConfirms.elements();
          while(enum.hasMoreElements())
          {
              XMLCostConfirmation confirm= (XMLCostConfirmation) enum.nextElement();
              buffer.append(confirm.getXMLString(false));
          }
          buffer.append("</Confirmations>");
        }

        buffer.append("</Account>");
        String xmlString =buffer.toString(); 

        if(withHead) return XML_HEAD+xmlString;
        return xmlString;
    }

    /** 
     * Hinzufügen eines Transfer-Zertifikats.
     * 
     * @param cert Transfer-Zertifikat 
     */
    public void addTransCert(XMLTransCert cert)
    {
        transCerts.add(cert);
    }

    /** 
     * Setzen des Kontoguthabens.
     * 
     * @param balance Kontoguthaben 
     */
    public void setBalance(XMLBalance balance)
    {
       this.balance=balance; 
    }

    private void setXMLCertificate() throws Exception
    {
        certificate = new XMLCertificate(xmlString.substring(xmlString.indexOf("<AccountCertificate>"),xmlString.indexOf("</AccountCertificate>")+21));
    }

    private void setXMLBalance() 
    {
        try
        {
            balance = new XMLBalance(xmlString.substring(xmlString.indexOf("<Balance>"),xmlString.indexOf("</Balance>")+10));
        }
        catch(Exception e)
        {
            balance=null;
        }
    }

    private void setXMLTransCert() throws Exception
    {
        String certsString;
        try
        {
            certsString = xmlString.substring(xmlString.indexOf("<TransferCertificates>")+23,xmlString.indexOf("</TransferCertificates>"));
        }
        catch(Exception e)
        {
            return;
        }

        int first, last, index=0;
        while((first=certsString.indexOf("<TransferCertificate>",index))!=-1)
        {
            last=certsString.indexOf("</TransferCertificate>",first)+22;
            String tmp = certsString.substring(first,last);
            index=last;
            try
            {
            XMLTransCert cert = new XMLTransCert(tmp);
            transCerts.add(cert);
            }
            catch(Exception e)
            {
                continue;
            }
        }
    }

    private void setXMLCostConfirms() throws Exception
    {
        String confirms;
        try
        {
            confirms= xmlString.substring(xmlString.indexOf("<CostConfirmations>")+19,xmlString.indexOf("</CostConfirmations>"));
        }
        catch(Exception e)
        {
            return;
        }

        int first, last, index=0;
        while((first=confirms.indexOf("<CC>",index))!=-1)
        {
            last=confirms.indexOf("</CC>",first)+5;
            String tmp = confirms.substring(first,last);
            index=last;
            try
            {
            XMLCC cc = new XMLCC(tmp.getBytes());
            costConfirms.add(cc);
            }
            catch(Exception e)
            {
                continue;
            }
        }
    }

    private void setRSAPrivateKey() throws Exception
    {
        Element element = domDocument.getDocumentElement();
        NodeList nl= element.getElementsByTagName("RSAPrivateKey");
        if (nl.getLength()<1) throw new Exception();
        element = (Element)nl.item(0);
        nl= element.getElementsByTagName("Modulus");
        if (nl.getLength()<1) throw new Exception();
        Element tmpElement=(Element)nl.item(0);
        CharacterData chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger modulus=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("PublicExponent");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger publicExponent=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("PrivateExponent");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger privateExponent=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("P");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger p=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("Q");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger q=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("dP");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger dP=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("dQ");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger dQ=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        nl= element.getElementsByTagName("QInv");
        if (nl.getLength()<1) throw new Exception();
        tmpElement=(Element)nl.item(0);
        chdata = (CharacterData)tmpElement.getFirstChild();
        BigInteger qInv=
        new BigInteger(Base64.decode(chdata.getData().toCharArray()));

        privateKey = new RSAPrivateCrtKeyParameters(modulus, publicExponent, privateExponent, p, q, dP, dQ, qInv);
        
    }

    /** 
     * Liefert Kontonummer des Kontos.
     * 
     * @return Kontonummer 
     */
    public long getAccountNumber()
    {
        return certificate.getAccountNumber();
    }

    /** 
     * Liefert das Kontozertifikat.
     * 
     * @return Kontozertifikat 
     */
    public String getAccountCertificate()
    {
        return certificate.getXMLString(false);
    }

    /** 
     * Liefert das Gültigkeitsdatum des Kontos.
     * 
     * @return Gültigkeitsende 
     */
    public Date getValidTime()
    {
        return certificate.getValidTime();
    }

    /** 
     * Liefert den geheimen Schlüssel des Kontos.
     * 
     * @return Geheimer Schlüssel 
     */
    public RSAKeyParameters getPrivateKey()
    {
        return privateKey;
    }

    /** 
     * Liefert den öffentlichen Schlüssel des Kontos.
     * 
     * @return Öffentlicher Schlüssel
     */
    public RSAKeyParameters getPublicKey()
    {
        return certificate.getPublicKey();
    }

    /** 
     * Liefert die Gesamtsumme des eingezahlten Geldes.
     * 
     * @return Gesamtsumme 
     */
    public long getCreditMax()
    {
        return balance.getCreditMax();
    }
    
    /** 
     * Liefert das noch verbleibene Guthaben.
     * 
     * @return Guthaben 
     */
    public long getCredit()
    {
        return balance.getCredit();
    }

    /** 
     * Liefert die Kostenbestätigungen.
     * 
     * @return Vector von CostConfirms 
     */
    public Vector getCostConfirms()
    {
        return costConfirms;
    }

    /** 
     * Liefert alle Transfer-Zertifikate.
     * 
     * @return Vector von {@link XMLTransCert} 
     */
    public Vector getTransCerts()
    {
        return transCerts;
    }
}

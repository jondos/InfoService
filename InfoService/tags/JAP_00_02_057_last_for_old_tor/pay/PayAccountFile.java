/*
 Copyright (c) 2000, The JAP-Team
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
package pay;

import java.util.Enumeration;
import java.util.Vector;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.util.PayText;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import anon.util.*;
import java.io.*;
/**
 *  Diese Klasse ist für die verwaltung aller Accounts zutändig, Dazu benutzt sie PayAcount's
 *  Die zugehörige XML Struktur zum speichern der Accounts ist wie folgend:
 * 	<PayAccount>
 * 		<UsedAccount>...</UseAccount> //Kontonummer desd aktiven Kontos
 * 		<Accounts>
 * 			<...>   //Konten gemaes PayAccount XML Struktur
 * 		</Accounts>
 * </PayAccount>
 *	* @author Andreas Mueller, Grischan Glänzel
 */

public class PayAccountFile
{
	//protected byte[] accountsBytes;
	protected Vector m_Accounts;

	protected String m_strPasswd = null;
	protected PayAccount m_MainAccount = null;

	public PayAccountFile(byte[] accountsBytes, String password)
	{
		m_strPasswd = password;
		m_Accounts = new Vector();
	//	this.accountsBytes = accountsBytes;
		try
		{
			setValues(accountsBytes);
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "PayAccountFile erfolgreich initialisiert+ mainAccountNr : " + getUsedAccount());
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  PayText.get("Pay AcconutFile lässt sich nicht initialisieren") + e);
		}

	}

	public PayAccountFile(byte[] accountsBytes)
	{
		this(accountsBytes, null);
	}

	// Achtung diese Methode legt nur den wert des Passwortes fest ohne das speicher des payAccountFiles kann es zu Wiederspüchen kommen
	public PayAccountFile(String password)
	{
		m_strPasswd = password;
		m_Accounts = new Vector();
	}

	public PayAccountFile()
	{
		m_Accounts = new Vector();
	}

	// Achtung diese Methode ändert nur die Passwort Variable ohne das
	// speicher des payAccountFiles kann es zu Wiederspüchen kommen
	public void setPassword(String password)
	{
		m_strPasswd = password;
	}

	public String getPassword()
	{
		return m_strPasswd;
	}

	/**
	 * Liest die Daten aus der Kontendatei und entschlüsselt sie mit Hilfe
	 * des angegeben Passwortes.
	 *
	 * @param filename Name der Kontendatei
	 * @param password Passwort
	 * @return 0 wenn neue Kontendatei erzeugt wurde, 1 wenn vorhandene
	 * Kontendatei gelesen wurde. -1 Wenn Fehler beim Lesen der Kontendatei, Fehler bei
	 * der Entschlüsselung insbesondere falsches Passwort
	 */

	public boolean hasUsedAccount()
	{
		return m_MainAccount != null;
	}

	public PayAccount getMainAccount()
	{
		return m_MainAccount;
	}

	protected void setNewMainAccount()
	{
		if (getAccounts().hasMoreElements())
		{
			m_MainAccount = (PayAccount) getAccounts().nextElement();
		}
		else
		{
			m_MainAccount = null;
		}
	}

	public void setUsedAccount(long accountNumber) throws Exception
	{
		m_MainAccount = getAccount(accountNumber);
		//store();
	}

	public long getUsedAccount()
	{
		if (m_MainAccount != null)
		{
			return m_MainAccount.getAccountNumber();
		}
		else
		{
			return -1;
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
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmp = (PayAccount) enum.nextElement();
			if (tmp.getAccountNumber() == accountNumber)
			{
				return tmp;
			}
		}
		return null;
	}

	/**
	 * Löscht das Konto mit der angebenen Kontonummer aus der Kontendatei.
	 * return das geänderte PayAccountFile oder null falls der Account nicht gelöscht
	 * werden konnte weil noch credits darauf waren.
	 *
	 * @param accountNumber Kontonummer
	 * @throws Exception Wenn ein Fehler bei Dateizugriff auftrat
	 */
	public boolean deleteAccount(long accountNumber) throws Exception
	{

		PayAccount tmp = getAccount(accountNumber);
		if (tmp.getCredit() > 0)
		{
			return false;
		}
		for (int i = 0; i < m_Accounts.size(); i++)
		{
			tmp = (PayAccount) m_Accounts.elementAt(i);
			if (tmp.getAccountNumber() == accountNumber)
			{
				m_Accounts.removeElementAt(i);
				if (m_MainAccount == tmp)
				{
					setNewMainAccount();
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "mainAccount==tmp");
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "mainAccount : " + m_MainAccount);
			}
		}
		return true;
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
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmpAccount = (PayAccount) enum.nextElement();
			tmp.addElement(new Long(tmpAccount.getAccountNumber()));
		}
		return tmp.elements();
	}

	/**
	 * Liefert alle Konten der Kontendatei.
	 *
	 * @return Vector von {@link PayAccount}s
	 */
	public Vector getAccountVec()
	{
		return m_Accounts;
	}

	/**
	 * Liefert alle Konten der Kontendatei.
	 *
	 * @return Enumeration von {@link PayAccount}s
	 */
	public Enumeration getAccounts()
	{
		return m_Accounts.elements();
	}

	/**
	 * Schreibt ein neues Konto in die Kontendatei.
	 *
	 * @param account zu speicherndes Konto
	 * @throws Exception Wenn ein Konto unter der Kontonummer schon in der
	 * Kontendatei enthalten ist. Wenn ein Dateizugriffsfehler auftrat.
	 */
	public PayAccountFile makeNewAccount(PayAccount account) throws Exception
	{
		PayAccount tmp;
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmp = (PayAccount) enum.nextElement();
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				throw new IllegalArgumentException();
			}
		}
		m_Accounts.addElement(account);
		if (m_MainAccount == null)
		{
			m_MainAccount = account;
		}
		return this;
	}

	/**
	 * Schreibt ein geändertes Konto in die Kontodatei.
	 *
	 * @param account zu speicherndes Konto
	 * @throws Exception Wenn ein Dateizugriffsfehler auftrat
	 */
	public PayAccountFile modifyAccount(PayAccount account)
	{
		PayAccount tmp;
		for (int i = 0; i < m_Accounts.size(); i++)
		{
			tmp = (PayAccount) m_Accounts.elementAt(i);
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				m_Accounts.setElementAt(account, i);
			}
		}
		return this;
	}

	/**
	 * Verschlüsseln und speichern der Kontodaten in die Kontendatei.
	 * @param filename Name und Pfad in dem die datei gespeichert werden soll
	 */



	/**
	 * Verschlüsseln und speichern der Kontodaten in die Kontendatei.
	 *
	 * @throws Exception Wenn ein Dateizugriffsfehler auftrat.
	 */

	public byte[] getXML() throws Exception
	{
		Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elemRoot=doc.createElement("PayAccount");
		doc.appendChild(elemRoot);
		elemRoot.setAttribute("version","1.0");
		Element elem=doc.createElement("UsedAccount");
		XMLUtil.setNodeValue(elem,Long.toString(getUsedAccount()));
		elemRoot.appendChild(elem);
		elem=doc.createElement("Accounts");
		elemRoot.appendChild(elem);
		for(int i=0;i<m_Accounts.size();i++)
		{
			PayAccount account=(PayAccount)m_Accounts.elementAt(i);
			Node n=XMLUtil.importNode(doc,account.getDomDocument().getDocumentElement(),true);
			elem.appendChild(n);
		}
		return XMLUtil.XMLDocumentToString(doc).getBytes();
	}


	/**
	 * Liest das in accountsBytes gespeicherte XML-Dokument und erzeugt ein
	 * Vector von {@link PayAccount}s.
	 *
	 * @throws Exception Wenn das XML-Dokument keine korrekten Kontodaten
	 * enthält. Wahrscheinliche Ursachen: Beim Entschlüsseln der Kontendatei
	 * ein falsches Passwort verwendet oder die Kontendatei ist fehlerhaft.
	 */
	private void setValues(byte[] xml) throws Exception
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(new ByteArrayInputStream(xml));
		Element elemRoot = doc.getDocumentElement();

		if(!elemRoot.getNodeName().equals("PayAccount"))
		{
			throw new Exception("Pay AccountFile: xmltoVector(): no <PayAccount>");
		}
		Element elem=(Element)XMLUtil.getFirstChildByName(elemRoot,"Accounts");
		Node n=elem.getFirstChild();
		while(n!=null)
		{
			PayAccount xmlAccount;
			try
			{
				xmlAccount = new PayAccount(n);
				m_Accounts.addElement(xmlAccount);

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "xmlToVector: Exception: ");
				e.printStackTrace();
				throw new Exception("wrong password or corrupt accountfile");
			}
			n=n.getNextSibling();
		}
		Element elemUsedAccount=(Element)XMLUtil.getFirstChildByName(elemRoot,"UsedAccount");
		setUsedAccount(Long.parseLong(XMLUtil.parseNodeString(elemUsedAccount,null)));
	}
}

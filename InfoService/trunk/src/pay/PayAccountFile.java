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
import pay.util.EasyXMLParser;
import pay.util.PayText;

/**
 *  Diese Klasse ist für die verwaltung aller Accounts zutändig, Dazu benutzt sie PayAcount's
 *	* @author Andreas Mueller, Grischan Glänzel
 */

public class PayAccountFile
{
	protected byte[] accountsBytes;
	protected Vector accounts;

	protected String password = null;
	protected PayAccount mainAccount = null;

	public static final String docStartTag = "<PayAccount>";
	public static final String docEndTag = "</PayAccount>";

	public PayAccountFile(byte[] accountsBytes, String password)
	{
		this.password = password;
		accounts = new Vector();
		this.accountsBytes = accountsBytes;
		try
		{
			xmlToVector();
			setUsedAccount(Long.parseLong(EasyXMLParser.getFirst(new String(accountsBytes), "UsedAccount")));
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
		this.password = password;
		accounts = new Vector();
	}

	public PayAccountFile()
	{
		accounts = new Vector();
	}

	// Achtung diese Methode ändert nur die Passwort Variable ohne das speicher des payAccountFiles kann es zu Wiederspüchen kommen
	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getPassword()
	{
		return password;
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
		return mainAccount != null;
	}

	public PayAccount getMainAccount()
	{
		return mainAccount;
	}

	protected void setNewMainAccount()
	{
		if (getAccounts().hasMoreElements())
		{
			mainAccount = (PayAccount) getAccounts().nextElement();
		}
		else
		{
			mainAccount = null;
		}
	}

	public void setUsedAccount(long accountNumber) throws Exception
	{
		mainAccount = getAccount(accountNumber);
		//store();
	}

	public long getUsedAccount()
	{
		if (mainAccount != null)
		{
			return mainAccount.getAccountNumber();
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
		Enumeration enum = accounts.elements();
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
		for (int i = 0; i < accounts.size(); i++)
		{
			tmp = (PayAccount) accounts.elementAt(i);
			if (tmp.getAccountNumber() == accountNumber)
			{
				accounts.removeElementAt(i);
				if (mainAccount == tmp)
				{
					setNewMainAccount();
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "mainAccount==tmp");
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "mainAccount : " + mainAccount);
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
		Enumeration enum = accounts.elements();
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
		return accounts;
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
	public PayAccountFile makeNewAccount(PayAccount account) throws Exception
	{
		PayAccount tmp;
		Enumeration enum = accounts.elements();
		while (enum.hasMoreElements())
		{
			tmp = (PayAccount) enum.nextElement();
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				throw new IllegalArgumentException();
			}
		}
		accounts.addElement(account);
		if (mainAccount == null)
		{
			mainAccount = account;
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
		for (int i = 0; i < accounts.size(); i++)
		{
			tmp = (PayAccount) accounts.elementAt(i);
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				accounts.setElementAt(account, i);
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

	public byte[] getXML()
	{
		byte[] all;
		StringBuffer tmp = new StringBuffer();
		tmp.append(docStartTag);
		tmp.append("<UsedAccount>" + getUsedAccount() + "</UsedAccount>");
		tmp.append("<Accounts>");
		appendBody(tmp);
		tmp.append("</Accounts>" + docEndTag);
		all = tmp.toString().getBytes();
		return all;
	}

	private void appendBody(StringBuffer tmp)
	{
		Enumeration enum = accounts.elements();
		PayAccount account;
		while (enum.hasMoreElements())
		{
			account = (PayAccount) enum.nextElement();
			tmp.append(account.getXMLString(false));
		}
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
		int first, second, off;
		off = 0;
		if (tmp.indexOf("<Accounts>") == -1)
		{
			throw new Exception("Pay AccountFile: xmltoVector(): no <Accounts>");
		}
		while ( (first = tmp.indexOf("<Account>", off)) != -1)
		{
			second = tmp.indexOf("</Account>", off) + 10;
			off = second;
			sub = tmp.substring(first, second);
			PayAccount xmlAccount;
			try
			{
				xmlAccount = new PayAccount(sub.getBytes());

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "xmlToVector: Exception: ");
				e.printStackTrace();
				throw new Exception("wrong password or corrupt accountfile");
			}
			accounts.addElement(xmlAccount);
		}
	}
}

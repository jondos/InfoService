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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.crypto.CryptFile;
import pay.data.Literals;

/**
 *  Diese Klasse ist für die Speicherung (verschlüsselt oder unverschlüsselt) von PayAcountFile zuständig. Ausserdem enthält sie
 *  die komplette Benutzerineraktion bezüglich des Passwortschutzes.
 *  D.h. wenn mittels PayAccountControl ein PayAcountFile geöffnet oder z.B. exportiert wird fragt es den Benutzer selbst nach dem Passwort
 *	oder auch ob ein Passwort angelegt werden soll.
 *	@author Grischan Glänzel
 */

public class PayAccountsControl
{

	protected CryptFile cryptFile;
	protected String filename = Literals.RES_PATH + Literals.ACCOUNT_FILE;
	protected String m_password;
	public static class NoPlainPayAccount extends Exception
	{}

	public static class WrongPasswordException extends Exception
	{}

	public PayAccountsControl()
	{
		cryptFile = new CryptFile();
	}

	private void setAccountFilePath(String filename)
	{
		this.filename = filename;
	}

	private String setPW(String titel, String msg, String error)
	{
		JPasswordField pass = new JPasswordField(12);
		JPasswordField pass2 = new JPasswordField(12);
		int result = JOptionPane.showOptionDialog(null, new JComponent[]
												  {new JLabel(msg), new JLabel("Password"), pass,
												  new JLabel("Password wiederholen"), pass2}
												  ,
												  titel, JOptionPane.YES_NO_OPTION,
												  JOptionPane.INFORMATION_MESSAGE, null, new Object[]
												  {"Ja", "Nein"}
												  , null);
		if (result != JOptionPane.YES_OPTION)
		{
			return null;
		}
		else
		{
			if (pass.getText().equals(pass2.getText()))
			{
				return pass.getText();
			}
			else
			{
				return setPW(titel, error, error);
			}
		}
	}

	public PayAccountFile open() throws WrongPasswordException, IOException,Exception
	{
		return open(this.filename);
	}

	public PayAccountFile open(String filename) throws WrongPasswordException, IOException, Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
				"PayAccountsControl.open(): filename='"+filename+"'."
			);
		byte[] all = null;
		try
		{  // try to open the accountfile
			all = cryptFile.read(filename);
		}
		catch (FileNotFoundException e)
		{
			// if the file does not exist, make a new one
			String pass = setPW("Passwort eingeben",
								"<html><h3>M&ouml;chten Sie die Kontoinformationen verschl&uuml;sselt speichern?</h3><br>" +
								"Dann geben sie bitte ein Password ein und dr&uuml;cken sie JA.<br>"+
								"Wenn nicht dr&uuml;cken sie NEIN.<br>" +
								"<strong>Achtung:</strong>Wenn sie die Kontodaten mit Password speichern,<br>"+
								"sollten sie <strong>das Passwort niemals vergessen</strong>,<br>" +
								"ihr Geld ist sonst unwiederbringlich verloren</html>",
								"Passw&ouml;rter stimmen nicht &uuml;berein, bitte nochmals eingeben!");
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PayAccountsControl.open(): eingegebenes passwort: " + pass);
			PayAccountFile accounts = new PayAccountFile(pass);
			store(accounts);
			return accounts;
		}
		try
		{
			return readPlain(all);
		}
		catch (NoPlainPayAccount no)
		{
			return readDecrypted(all);
		}
	}

	private PayAccountFile readPlain(byte[] all) throws NoPlainPayAccount
	{
		try{
				return new PayAccountFile(all);
		}
		catch(Exception e)
		{
			throw new NoPlainPayAccount();
		}
	}

	private PayAccountFile readDecrypted(byte[] all) throws WrongPasswordException
	{
		String pass = "";
		while (pass != null)
		{
			pass = checkPW("Bitte geben sie das Passwort fuer die Konten Datei ein", "Kontodaten öffnen");
			try
			{
				byte[] tmp = cryptFile.decrypt(all, pass);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "readDecrypted: Decrypten hat funktioniert");
				return new PayAccountFile(tmp, pass);
			}
			catch (CryptFile.DecryptException ee)
			{}
		}
		throw new WrongPasswordException();
	}

	/**
	 *	gibt das Password zurück welches der Benutzer eingegeben hat oder null wenn die eingabe abgebrochen wurde
	 */
	private String checkPW(String titel, String message)
	{
		JPasswordField pass = new JPasswordField(12);
		int result = JOptionPane.showOptionDialog(null, new JComponent[]
												  {new JLabel(message), pass}
												  ,
												  titel, JOptionPane.YES_NO_OPTION,
												  JOptionPane.INFORMATION_MESSAGE, null, new Object[]
												  {"Ja", "Nein"}
												  , null);
		if (result != JOptionPane.YES_OPTION)
		{
			return null;
		}
		else
		{
			return pass.getText();
		}
	}

	public boolean changePW(PayAccountFile accounts)
	{
		boolean changed = false;
		if (accounts.getPassword() != null)
		{
			String pass;
			pass = setPW("Passworteingabe", "bitte geben sie das neue Passwort ein",
						 "keine Übereinstimmung : bitte wiederholen");
			if (pass != null)
			{
				accounts.setPassword(pass);
				changed = true;
			}
		}
		try
		{
			store(accounts);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "geänderte Datei konnte nicht gespeichert werden");
		}
		return changed;
	}

	public PayAccountFile changeEncryptMode(PayAccountFile file) throws Exception
	{
		if (file.getPassword() == null)
		{
			String pass = setPW("Passworteingabe", "bitte geben sie das neue Passwort ein",
								"keine Übereinstimmung : bitte wiederholen");
			if (pass != null)
			{
				file.setPassword(pass);
			}
		}
		else
		{
			file.setPassword(null);
		}
		try
		{
			store(file);
		}
		catch (IOException ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "speichern des Accout Files ging nicht");
		}
		return file;
	}

	public void store(PayAccountFile accounts, String filename) throws Exception
	{
		byte[] all = accounts.getXML();
		String pass = accounts.getPassword();
		if (pass != null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "PayAccountsControl.store(): accountfile '"+filename+
							"' wird verschlüsselt, passwort: " + pass);
			all = cryptFile.encrypt(accounts.getXML(), pass);
		}
		FileOutputStream out = new FileOutputStream(filename);
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
				"PayAccountsControl.store(): now saving file '"+filename+"'!");
		out.write(all);
		out.flush();
		out.close();
	}

	public void store(PayAccountFile accounts) throws Exception
	{
		store(accounts, this.filename);
	}

}

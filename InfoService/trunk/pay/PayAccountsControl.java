package pay;

import pay.data.*;
import pay.util.*;
import pay.crypto.*;
import javax.swing.*;
import java.io.*;
/**
 *  Diese Klasse ist für die speicherung (verschlüsselt oder unverschlüsselt) von PayAcountFile zuständig. Ausserdem enthält sie
 *  die komplette Benutzerineraktion bezüglich des Passwortschutzes.
 *  D.h. Wenn mittels PayAccountControl ein PayAcountFile geöffnet oder zb exportiert wird fragt es den Bneutzer selbst nach dem Passwort
 *	oder auch ob ein Passwort angelegt werden soll.
 *	* @author Grischan Glänzel
 */

public class PayAccountsControl{

	protected CryptFile cryptFile;
	protected String filename = Literals.RES_PATH+Literals.ACCOUNT_FILE;
	protected String password;
	public static class NoPlainPayAccount extends Exception{}
	public static class WrongPasswordException extends Exception{}


	public PayAccountsControl(){
		cryptFile = new CryptFile();
	}

	private void setAccountFilePath(String filename){
			this.filename = filename;
	}

	private String setPW(String titel,String msg, String error){
		JPasswordField pass = new JPasswordField(12);
		JPasswordField pass2 = new JPasswordField(12);
		int result = JOptionPane.showOptionDialog(null,new JComponent[]{new JLabel(msg),new JLabel("Password"),pass,new JLabel("Password wiederholen"),pass2},
										titel,JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE,null,new Object[]{"Ja","Nein"},null);
		if(result!=JOptionPane.YES_OPTION) return null;
		else{
			if (pass.getText().equals(pass2.getText())) return pass.getText();
			else return setPW(titel,error,error);
		}
	}
	public PayAccountFile open() throws WrongPasswordException, IOException{
	return open(this.filename);
	}
	public PayAccountFile open(String filename) throws WrongPasswordException, IOException{
		byte[] all = null;
		try{
			all = cryptFile.read(filename);
		}
		catch(FileNotFoundException e)
		{
			String pass = setPW("Passwort eingeben","<html>Wollen sie die Konto Informationen verschlüsselt speichern ?\n<br>"+
			"Dann geben sie bitte ein Password ein und drücken sie JA. Wenn nicht drücken sie NEIN.\n<br>"+
			"Achtung: Wenn sie die Kontodaten mit Password speichern sollte sie dies unbedingt erinnern\n<br>"+
			"ihr Geld ist sonst unwiederbringlich verloren<html>", "Passwörter stimmt nicht überein nochmals eingeben");
			Log.log(this,"paswwort = "+pass,Log.TEST);
			PayAccountFile accounts = new PayAccountFile(pass);
			store(accounts);
			return accounts;
		}
		try{
			return readPlain(all);
		}catch(NoPlainPayAccount no){
			return readDecrypted(all);
		}
	}
    private PayAccountFile readPlain(byte[] all) throws NoPlainPayAccount{
		if((new String(all).indexOf(PayAccountFile.docStartTag))!=-1){
			Log.log(this,"readPlain : accountFile ist NOT decrypted",Log.SHORT_DEBUG);
			return new PayAccountFile(all);
		} else throw new NoPlainPayAccount();
	}

	private PayAccountFile readDecrypted(byte[] all) throws WrongPasswordException{
		String pass = "";
		while(pass!=null){
			pass = checkPW("Bitte geben sie das Passwort fuer die Konten Datei ein","Kontodaten öffnen");
			try{
				byte[] tmp = cryptFile.decrypt(all,pass);
				Log.log(this,"readDecrypted: Decrypten hat funktioniert",Log.SHORT_DEBUG);
				return new PayAccountFile(tmp, pass);
			}catch (CryptFile.DecryptException ee){}
		}
		throw new WrongPasswordException();
	}

	/**
	*	gibt das Password zurück welches der Benutzer eingegeben hat oder null wenn die eingabe abgebrochen wurde
	*/
	private String checkPW(String titel,String message){
		JPasswordField pass = new JPasswordField(12);
		int result = JOptionPane.showOptionDialog(null,new JComponent[]{new JLabel(message),pass},
										titel,JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE,null,new Object[]{"Ja","Nein"},null);
		if(result!=JOptionPane.YES_OPTION) return null;
		else return pass.getText();
	}


    public PayAccountFile changeEncryptMode(PayAccountFile file){
		if(file.getPassword()==null){
			String pass = setPW("Passworteingabe","bitte geben sie das neue Passwort ein", "keine Übereinstimmung : bitte wiederholen");
			if(pass!=null){
				file.setPassword(pass);
			}
		}
		else{
			file.setPassword(null);
		}
		try{
			store(file);
		}catch(IOException ex){
			Log.log(this,"speichern des Accout Files ging nicht",Log.SHORT_DEBUG);
		}
		return file;
	}
	public void store(PayAccountFile accounts, String filename) throws IOException{
		byte[] all =accounts.getXML();
		if(accounts.getPassword()!=null){
			all = cryptFile.encrypt(accounts.getXML(),accounts.getPassword());
			Log.log(this,"store(): accounts wird encrypted gespeichert pass: "+password,Log.SHORT_DEBUG);
		}
		FileOutputStream out = new FileOutputStream(filename);
		out.write(all);
		out.flush();
		out.close();
    }
    public void store(PayAccountFile accounts) throws IOException{
		store(accounts, this.filename);
	}
	public boolean changePW(PayAccountFile accounts){
		boolean changed = false;
		if(accounts.getPassword()!=null){
			String pass;
			pass = setPW("Passworteingabe","bitte geben sie das neue Passwort ein", "keine Übereinstimmung : bitte wiederholen");
			if(pass!=null){
				accounts.setPassword(pass);
				changed = true;
			}
		}
		try{
			store(accounts);
		}catch(Exception ex){Log.log(this,"geänderte Datei konnte nicht gespeichert werden",Log.SHORT_DEBUG);}
		return changed;
	}
}
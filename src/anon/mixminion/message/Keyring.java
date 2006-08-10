/*
Copyright (c) 2004, The JAP-Team
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
package anon.mixminion.message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Vector;

import anon.util.Base64;
import anon.util.ByteArrayUtil;

/**
 * @author Stefan Rönisch
 *
 */
public class Keyring {
	
	final int KEY_LEN =16;
	static final long KEY_LIFETIME = 3*30*24*60*60;
	private Vector m_mykeys;
	private String m_password;
	
		
	/**
	 * Constructor
	 * @param password
	 */
	public Keyring(String password) {
		m_mykeys = new Vector();
		m_password = password;
		
	//try to load the keyring
		String keyring=""; //data from the textfile
		String aktLine="";
	  	try 
		{
	  		FileReader freader = new FileReader("keyring.txt");
	  		BufferedReader in = new BufferedReader(freader);

	  		while((aktLine = in.readLine()) != null) 
	  		{
	  			keyring = keyring + "\n" +aktLine;
	  		}
	  	freader.close();
	  	}
	  		catch(Exception eve)
	  	{
	  		System.out.println("konnte datei nicht einlesen");
	  	}
        //If no Keyring exists, do nothing
	  	if (keyring == "") {
	  		
	  	}
	  	else {
	  		try {
				//decrypt the data	
	  			unpackKeyRing(keyring);
			} catch (IOException e) {
				e.printStackTrace();
			}
	  	}
	}
	
	/**
	 * 
	 * @return a Vector with all usersecrets
	 */
	public Vector getUserSecrets() {
		return m_mykeys;
	}
	
	/**
	 * 
	 * @return new secret as byte[]
	 */
	public byte[] getNewSecret() {
		return makeNewKey();
	}
	
	/**
	 * encrypts the m_data and brings it in a 
	 * ascii-armored base64 notation
	 * @return string with base64 keyring
	 */
	private String packKeyring() {
//		   The format of the actual data is as follows:
//
//						         KeyData ::= Item *
//						         Item ::= ItemType [1 octet]
//						                  ItemLen  [2 octets]
//						                  ItemVal  [ItemLen octets]
//
//						   Implementations MUST skip over items with unrecognized types, and
//						   preserve them when modifying the keyring.  Implementations MUST NOT
//						   depend on any order of items within the keyring.
//
//						   SURB keys have the following format:
//						         SURBKeyType    [00]
//						         SURBKeyLen     [2 octets]
//						         SURBKeyExpires [4 octets]
//						         SURBKeyName    [Variable; NUL-terminated]
//						         SURBKeySecret  [Variable]
		//convert all keys in m_mykeys to m_data
		byte[] itemdata = new byte[0];
		for(int i = 0; i < m_mykeys.size(); i++ ) {
			byte[] name = MixMinionCryptoUtil.randomArray(10); //TODO Naming?
			name[9] = 0x00;
			Calendar cal = Calendar.getInstance();
			cal.setTime(new java.util.Date());
			int day = cal.get(Calendar.DAY_OF_YEAR);
			int year = cal.get(Calendar.YEAR);
			byte[] expires = ByteArrayUtil.inttobyte(((((year-1970-1)*365)+day)*24*60*60)+KEY_LIFETIME,4);
			byte[] item = ByteArrayUtil.conc(expires, name,(byte[]) m_mykeys.get(i));
			item = ByteArrayUtil.conc(new byte[1],ByteArrayUtil.inttobyte(item.length,2),item);
			itemdata = ByteArrayUtil.conc(itemdata, item);	
		}
		
		String packedRing = null;
//		First, the keyring itself is stored with RFC2440-style ASCII armor,
//		   with header text "BEGIN TYPE III KEYRING" and an armor header
//		   "Version" with a value "0.1".  The contents to be encoded are:
//
//		         magic       [8 octets]
//		         format type [1 octet == 0]
//		         salt        [8 octets]
//		         encdata     [variable]
//
//		   Where 'magic' is "KEYRING2" [ 4B 45 59 52 49 4E 47 32 ], 'salt' is
//		   a randomly chosen octet sequence, and 'encdata' is computed from
//		   the actual identity data 'data' and a user-selected password
//		   'password' as follows:
		
		byte[] magic = "KEYRING2".getBytes();
		byte[] salt = {0x12, 0x08,0x20, 0x10, 0x34, 0x56, 0x07, 0x13};
		byte[] encdata;
		
//		         Let padding = Rand(1024*CEIL(LEN(data)/1024) - LEN(data))
//		         Let data' = Int(32, LEN(data)) | data | padding
//		         Let hash = H(data' | salt | magic)
//		         Let key = H(salt | password | salt)[0:KEY_LEN]
//		         Let encdata = Encrypt(key, data' | hash)
		
		byte[] padding = MixMinionCryptoUtil.randomArray(
						(1024*myceil(itemdata.length,1024))-itemdata.length);
				
		byte[] data2 = ByteArrayUtil.conc(
							ByteArrayUtil.inttobyte(itemdata.length, 4), itemdata, padding);
		byte[] hash = MixMinionCryptoUtil.hash(
							ByteArrayUtil.conc(data2, salt, magic));
		byte[] key = ByteArrayUtil.copy(
						MixMinionCryptoUtil.hash(
								ByteArrayUtil.conc(salt, m_password.getBytes(), salt)),0,KEY_LEN);
		encdata = MixMinionCryptoUtil.Encrypt(key, 
										ByteArrayUtil.conc(data2, hash));
		
		packedRing = "-----BEGIN TYPE III KEYRING-----\n" +
					 "Version: 0.1\n\n" +
					 Base64.encodeBytes(encdata) +
					 "\n-----END TYPE III KEYRING-----";
		
		return packedRing;
		
	}
	
	/*
	 * given a base64 keyring it decrypts it and filters out items and keys
	 */
	private void unpackKeyRing(String encodeddata) throws IOException {
		//get it out of the armor and decode it base64
		String enc ="";
		LineNumberReader reader = new LineNumberReader(new StringReader(encodeddata));
		reader.readLine();reader.readLine();reader.readLine(); //skip armor,version and \n
		String aktLine = reader.readLine();
		while(!aktLine.startsWith("-----END")) {
			enc += aktLine;
			aktLine = reader.readLine();
		}
		byte[] data = Base64.decode(enc);
		//decrypt it
		byte[] salt = {0x12, 0x08,0x20, 0x10, 0x34, 0x56, 0x07, 0x13};
		byte[] magic = "KEYRING2".getBytes();
		byte[] key = ByteArrayUtil.copy(
				MixMinionCryptoUtil.hash(
						ByteArrayUtil.conc(salt, m_password.getBytes(), salt)),0,KEY_LEN);
		data = MixMinionCryptoUtil.Encrypt(key, data);
		
		//test whether the hash is ok otherwise wrong password?!
		byte[] hash = ByteArrayUtil.copy(data, data.length-20,20);
		data = ByteArrayUtil.copy(data, 0, data.length-20);
				
		byte[] hash2 = MixMinionCryptoUtil.hash(
										ByteArrayUtil.conc(data, salt, magic));
		if (!ByteArrayUtil.equal(hash,hash2)) {
			System.out.println("falsches Passwort!");//TODO hier abbrechen...
		}
		//discard the padding
		byte[] l = ByteArrayUtil.copy(data,0,4);
		int datalength = byteToInt(l,0);
		data = ByteArrayUtil.copy(data,4,datalength);
		
		//get out the usersecrets
		int counter = 0;
		int schl=0;
		while(1==1) {
			if ((counter >= data.length)) {
				break;
			}
			if (data[counter] == 0x00) {
				byte[] actsecret = ByteArrayUtil.copy(data, counter+17,20); //TODO lenght, date usw?
				m_mykeys.addElement(actsecret);
				schl++;
				counter = counter+32;
			}
			counter = counter+1;
		}
		
	}
	
	/**
	 * Produces a new usersecret
	 * @return
	 */
	private byte[] makeNewKey() {

		//build new secret
		byte[] newsecret = MixMinionCryptoUtil.randomArray(20);
		//add it to the keyvector
		m_mykeys.addElement(newsecret);

//		save Keyring 
		saveKeyRing();
		return newsecret;
	}
	
	/**
	 * saves the keyring
	 *
	 */
	private void saveKeyRing() {
		try {
			   BufferedWriter out = new BufferedWriter(
	                    new OutputStreamWriter(
	                    new FileOutputStream( "keyring.txt" ) ) );
			   
			    String s = packKeyring();
			   	out.write( s, 0, s.length() );
			   	out.close();
			}
			catch (IOException e) {
				System.out.println("Konnte Keyring ne speichern");
				e.printStackTrace();
			}
	}
	
	/**
	 * Calculates the int value of a given ByteArray
	 * @param b
	 * @param offset
	 * @return int value of the bytearray
	 */
	private int byteToInt(byte[] b, int offset) {
		int value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
	}
	
	/**
	 * Behaves like Math.ceil, but gives minimum 1 as value
	 * @param a
	 * @param b
	 * @return
	 */
	private int myceil(double a, double b) {
		int c = (int) Math.ceil(a / b);
		if (c == 0) return 1;
			else return c;
	}
}

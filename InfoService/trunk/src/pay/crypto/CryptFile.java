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
package pay.crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Klasse zum Ver- und Entschlüsseln von Daten mit AES und persistente
 * Speicherung in einer Datei.
 */
public class CryptFile
{
	final static int CHUNK_SIZE = 1024 * 1024;
	private BlockCipher cipher;
	private int blockSize;

	public static class DecryptException extends Exception
	{};

	/**
	 * Liest und entschlüsselt eine Datei und überprüft die Integrität der
	 * Daten.
	 *
	 * @param filename Dateiname
	 * @param password Passwort
	 * @return entschlüsselter Dateiinhalt
	 * @throws Exception Wenn ein Dateizugriffsfehler auftrat, ein falsches
	 * Passwort verwendet wurde oder eine Integritätsverletzung der Daten
	 * auftrat
	 */
	public byte[] readAndDecrypt(String filename, String password) throws DecryptException, IOException
	{
		byte[] tmpAccounts = read(filename);
		byte[] accounts = decrypt(tmpAccounts, password);
		return accounts;
	}

	/**
	 * Verschlüsselt und speichert ein byte-Array.
	 *
	 * @param filename Dateiname
	 * @param password Passwort
	 * @param accounts zu verschlüsselnde und speichernde Daten
	 * @throws Exception Wenn ein Dateizugriffsfehler auftrat
	 */
	public void encryptAndWrite(String filename, String password, byte[] accounts) throws Exception
	{
		byte[] tmpAccounts = encrypt(accounts, password);
		write(filename, tmpAccounts);
	}

	public CryptFile()
	{
		cipher = new CBCBlockCipher(new AESFastEngine());
		blockSize = cipher.getBlockSize();
	}

	public byte[] read(String filename) throws IOException
	{
		int l = 1;
		int p = 0;
		byte[] accounts = new byte[l * CHUNK_SIZE];
		FileInputStream in = new FileInputStream(filename);
		int c = in.read(accounts);
		while (c != -1)
		{
			if (c == accounts.length - p)
			{
				byte[] tmp = new byte[accounts.length + CHUNK_SIZE];
				System.arraycopy(accounts, 0, tmp, 0, accounts.length);
				accounts = tmp;
			}
			p += c;
			c = in.read(accounts, p, accounts.length - p);
		}
		if (p < accounts.length)
		{
			byte[] tmp = new byte[p];
			System.arraycopy(accounts, 0, tmp, 0, tmp.length);
			accounts = tmp;
		}
		return accounts;
	}

	public void write(String filename, byte[] accounts) throws IOException
	{
		FileOutputStream out = new FileOutputStream(filename);
		out.write(accounts);
	}

	public byte[] encrypt(byte[] inBytes, String password)
	{
		SHA1Digest digest = new SHA1Digest();
		int digestLength = digest.getDigestSize();
		byte[] bytesDigest = new byte[digestLength];

		byte[] tmp;
		if ( (inBytes.length + digestLength) % blockSize == 0)
		{
			tmp = new byte[inBytes.length + digestLength + 2 * blockSize];
		}
		else
		{
			tmp = new byte[ ( (inBytes.length + digestLength) / blockSize + 2) * blockSize];
		}
		tmp[tmp.length - digestLength - 1] = new Integer(tmp.length - inBytes.length - blockSize).byteValue();
		byte iv[] = new byte[blockSize];
		Random rand = new Random(System.currentTimeMillis());
		rand.nextBytes(iv);
		System.arraycopy(iv, 0, tmp, 0, blockSize);
		System.arraycopy(inBytes, 0, tmp, blockSize, inBytes.length);

		digest.reset();
		digest.update(inBytes, 0, inBytes.length);
		digest.doFinal(bytesDigest, 0);

		System.arraycopy(bytesDigest, 0, tmp, tmp.length - digestLength, digestLength);

		return crypt(true, tmp, password);
	}

	public byte[] decrypt(byte[] inBytes, String password) throws DecryptException
	{
		SHA1Digest digest = new SHA1Digest();
		int digestLength = digest.getDigestSize();
		byte[] bytesDigest = new byte[digestLength];

		byte[] tmp = crypt(false, inBytes, password);
		int length = tmp.length - blockSize - tmp[tmp.length - digestLength - 1];
		byte[] plain = new byte[length];
		System.arraycopy(tmp, blockSize, plain, 0, length);

		digest.reset();
		digest.update(plain, 0, plain.length);
		digest.doFinal(bytesDigest, 0);

		byte[] tmpDigest = new byte[digestLength];
		System.arraycopy(tmp, tmp.length - digestLength, tmpDigest, 0, digestLength);
		if (bytesDigest == null || tmpDigest == null || bytesDigest.length != tmpDigest.length)
		{
			throw new DecryptException();
		}
		for (int i = 0; i < tmpDigest.length; i++)
		{
			if (bytesDigest[i] != tmpDigest[i])
			{
				throw new DecryptException();
			}
		}
		return plain;
	}

	private void showArray(byte[] array)
	{
		System.out.println();
		for (int i = 0; i < array.length; i++)
		{
			System.out.print(array[i] + " ");
		}
		System.out.println();
	}

	private byte[] crypt(boolean encrypt, byte[] inBytes, String password)
	{
		byte[] bytesPassword = password.getBytes();

		SHA1Digest digest = new SHA1Digest();
		byte[] bytesDigest = new byte[digest.getDigestSize()];
		digest.reset();
		digest.update(bytesPassword, 0, bytesPassword.length);
		digest.doFinal(bytesDigest, 0);

		cipher.init(encrypt, new ParametersWithIV(new KeyParameter(bytesDigest, 0, 16), inBytes, 0, blockSize));
		cipher.reset();

		byte[] decAccountBytes = new byte[inBytes.length];
		System.arraycopy(inBytes, 0, decAccountBytes, 0, blockSize);
		int in = blockSize;
		int out = blockSize;

		while (in < inBytes.length)
		{
			cipher.processBlock(inBytes, in, decAccountBytes, out);
			in += blockSize;
			out += blockSize;
		}

		return decAccountBytes;
	}
}

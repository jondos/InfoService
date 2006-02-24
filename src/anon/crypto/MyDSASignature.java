/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.Signature;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Implements the DSA algorithm for signatures.
 */
public final class MyDSASignature implements IMySignature
{
	private static final AlgorithmIdentifier ms_identifier =
		new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa_with_sha1);

	private Signature m_SignatureAlgorithm;
	/**
	 * The key with that this algorithm has been initialised.
	 */
	private Key m_initKey;

	public MyDSASignature()
	{
		try
		{
			m_SignatureAlgorithm = Signature.getInstance("DSA");
		}
		catch (Exception e)
		{
			m_SignatureAlgorithm = null;
		}
	}

	synchronized public void initVerify(IMyPublicKey k) throws InvalidKeyException
	{
		//if (m_initKey == null || m_initKey != k)
		//{
		m_SignatureAlgorithm.initVerify(k);
		m_initKey = k;
		//}
	}

	synchronized public void initSign(IMyPrivateKey ownPrivateKey) throws InvalidKeyException
	{
		//if (m_initKey == null || m_initKey != ownPrivateKey)
		//{
		m_SignatureAlgorithm.initSign(ownPrivateKey);
		m_initKey = ownPrivateKey;
		//}
	}

	synchronized public boolean verify(byte[] a_message, int message_offset, int message_len,
									   byte[] a_signature,
									   int signature_offset, int signature_len)
	{
		try
		{
			m_SignatureAlgorithm.update(a_message, message_offset, message_len);
			if (signature_offset == 0 && signature_len == a_signature.length)
			{
				boolean b=m_SignatureAlgorithm.verify(a_signature);
				LogHolder.log(LogLevel.DEBUG,LogType.CRYPTO,"MyDSASignature:verify() Result: "+b);
				return b;
			}
			byte[] theSig = new byte[signature_len];
			System.arraycopy(a_signature, signature_offset, theSig, 0, signature_len);
			return m_SignatureAlgorithm.verify(theSig);
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.DEBUG,LogType.CRYPTO,"MyDSASignature:verify() Exception!");
			LogHolder.log(LogLevel.DEBUG,LogType.CRYPTO,e);

			return false;
		}
	}

	synchronized public boolean verify(byte[] a_message, byte[] a_signature)
	{
		LogHolder.log(LogLevel.DEBUG,LogType.CRYPTO,"MyDSASignature:verify() try to verify a message...");
		return verify(a_message, 0, a_message.length, a_signature, 0, a_signature.length);
	}

	synchronized public byte[] sign(byte[] bytesToSign)
	{
		try
		{
			m_SignatureAlgorithm.update(bytesToSign);
			return m_SignatureAlgorithm.sign();
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	/**
	 * Returns the algorithm identifier (DSA with SHA1).
	 * @return the algorithm identifier (DSA with SHA1)
	 */
	public AlgorithmIdentifier getIdentifier()
	{
		return ms_identifier;
	}

	/**
	 * Encodes a signature in a way it meets the W3C standard for DSA XML signature values.
	 *
	 * Extracts the ASN.1 encoded values for r and s from a DER encoded byte array.
	 * ASN.1 Notation:
	 *   sequence {
	 *     integer r
	 *     integer s
	 *   }
	 * --> Der-Encoding
	 * byte   0x30    // Sequence
	 * byte   44 + x  // len in bytes (x = {0|1|2} depending on r and s
	 * byte   0x02    // integer
	 * byte   <= 21   // len of r (21: if first bit of r set, we need a leading 0 --> 20 + 1 bytes)
	 * byte[] ...     // value of r (with leading zero if necessary)
	 * byte   0x02    // integer
	 * byte   <= 21   // len of s (21: if first bit of s set, we need a leading 0 --> 20 + 1 bytes)
	 * byte[] ...     // value of s (with leading zero if necessary)
	 *
	 * @param a_signature an non-encoded signature in DER format
	 * @return the encoded signature in R-S-encoding or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-DSA
	 */
	public byte[] encodeForXMLSignature(byte[] a_signature)
	{
		byte rLength = a_signature[3];
		byte sLength = a_signature[3 + rLength + 2];
		byte[] rsBuff = new byte[40];
		for (int i = 0; i < 40; i++)
		{
			/* be sure that it is zero */
			rsBuff[i] = 0;
		}
		byte rOverLength = 0;
		if (rLength == 21)
		{
			rOverLength = 1;
			rLength = 20;
		}
		System.arraycopy(a_signature, 4 + rOverLength, rsBuff, 20 - rLength, rLength);
		rLength = (byte) (rLength + rOverLength);
		byte sOverLength = 0;
		if (sLength == 21)
		{
			sOverLength = 1;
			sLength = 20;
		}
		System.arraycopy(a_signature, 4 + rLength + 2 + sOverLength, rsBuff, 40 - sLength, sLength);

		return rsBuff;

	}

	/**
	 * Tries to decode a signature in a way as it would meet the W3C standard for DSA XML
	 * signature values.
	 *
	 * Making DER-Encoding of r and s.....
	 * ASN.1 Notation:
	 * sequence
	 * {
	 *        integer r
	 *		  integer s
	 * }
	 * HINT: Sun JDK 1.4.x needs a leading '0' in the binary representation
	 * of r (and s) if r[0]>0x7F or s[0]>0x7F
	 * --> Der-Encoding
	 * 0x30 //Sequence
	 * 44 + x // len in bytes (x = {0|1|2} depending on r and s (see above)
	 * 0x02 // integer
	 * 20 | 21 // len in bytes of r
	 * ....   //value of r (with leading zero if necessary)
	 * 0x02 //integer
	 * 20 | 21  //len of s
	 * ... value of s (with leading zero if necessary)
	 *
	 * @param a_encodedSignature an encoded signature in R-S format
	 * @return the decoded signature in DER format or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-DSA
	 */
	public byte[] decodeForXMLSignature(byte[] a_encodedSignature)
	{
		try
		{
			int index = 46;
			if (a_encodedSignature[0] < 0)
			{
				index++;
			}
			if (a_encodedSignature[20] < 0)
			{
				index++;
			}
			byte tmpBuff[] = new byte[index];
			tmpBuff[0] = 0x30;
			tmpBuff[1] = (byte) (index - 2);
			tmpBuff[2] = 0x02;
			if (a_encodedSignature[0] < 0)
			{
				index = 5;
				tmpBuff[3] = 21;
				tmpBuff[4] = 0;
			}
			else
			{
				tmpBuff[3] = 20;
				index = 4;
			}
			System.arraycopy(a_encodedSignature, 0, tmpBuff, index, 20);
			index += 20;
			tmpBuff[index++] = 0x02;
			if (a_encodedSignature[20] < 0)
			{
				tmpBuff[index++] = 21;
				tmpBuff[index++] = 0;
			}
			else
			{
				tmpBuff[index++] = 20;
			}
			System.arraycopy(a_encodedSignature, 20, tmpBuff, index, 20);
			return tmpBuff;
		}
		catch (Exception a_e)
		{
			return null;
		}

	}

	/**
	 * Returns http://www.w3.org/2000/09/xmldsig#dsa-sha1.
	 * @return http://www.w3.org/2000/09/xmldsig#dsa-sha1
	 */
	public String getXMLSignatureAlgorithmReference()
	{
		return "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
	}

}

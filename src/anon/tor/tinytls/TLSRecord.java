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
package anon.tor.tinytls;

/**
 * <p>†berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Organisation: </p>
 * @author not attributable
 * @version 1.0
 */

public class TLSRecord
{
	public int m_Type;
	public int m_dataLen;
	public byte[] m_Data;
	public byte[] m_Header;

	/**
	 * Constructor
	 *
	 */
	public TLSRecord()
	{
		m_Header = new byte[5];
		m_Header[1] = TinyTLS.PROTOCOLVERSION[0];
		m_Header[2] = TinyTLS.PROTOCOLVERSION[1];
		m_Data = new byte[0xFFFF];
		m_dataLen = 0;
	}

	/**
	 * sets the typeof the tls record
	 * @param type
	 * type
	 */
	public void setType(int type)
	{
		m_Type = type;
		m_Header[0] = (byte) (type & 0x00FF);
	}

	/**
	 * sets the length of the tls record
	 * @param len
	 * length
	 */
	public void setLength(int len)
	{
		m_dataLen = len;
		m_Header[3] = (byte) ( (len >> 8) & 0x00FF);
		m_Header[4] = (byte) ( (len) & 0x00FF);
	}

}

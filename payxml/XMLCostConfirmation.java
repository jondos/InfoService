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
package payxml;

import payxml.util.Base64;


public class XMLCostConfirmation extends XMLDocument
{
	//~ Instance fields ********************************************************

	private XMLCC cc;
	private byte[] digest;

	//~ Constructors ***********************************************************

	public XMLCostConfirmation(String data) throws Exception
	{
		this(data.getBytes());
	}


	public XMLCostConfirmation(byte[] data) throws Exception
	{
		setDocument(data);

		String ccString = xmlDocument.substring(xmlDocument.indexOf("<CC>"),
				xmlDocument.indexOf("</CC>") + 5
			);
		cc = new XMLCC(ccString.getBytes());

		String digestString = xmlDocument.substring(xmlDocument.indexOf(
					"<Digest>"
				) + 9, xmlDocument.indexOf("</Digest>")
			);
		digest = Base64.decode(digestString.toCharArray());
	}

	//~ Methods ****************************************************************

	public String getAIName()
	{
		return cc.getAIName();
	}


	public long getAccountNumber()
	{
		return cc.getAccountNumber();
	}


	public int getCosts()
	{
		return cc.getCosts();
	}


	public byte[] getDigest()
	{
		return getDigest();
	}


	public byte[] getHash()
	{
		return cc.getHash();
	}


	public int getTickPrice()
	{
		return cc.getTickPrice();
	}


	public String getXMLString(boolean withHead)
	{
		if (withHead) {
			return XML_HEAD + xmlDocument;
		} else {
			return xmlDocument;
		}
	}
}

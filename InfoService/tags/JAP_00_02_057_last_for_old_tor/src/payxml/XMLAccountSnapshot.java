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

/**
 * Format of the XML structure:
 * <pre>
 *   &lt;AccountSnapshot version="1.0"&gt;
 *     &lt;ID&gt;Name of AI&lt;/ID&gt;
 *     &lt;AccountNumber&gt;0123456789012&lt;/AccountNumber&gt;
 *     &lt;CreditMax&gt;1000&lt;/CreditMax&gt;
 *     &lt;Credit&gt;300&lt;/Credit&gt;
 *     &lt;Costs&gt;50&lt;/Costs&gt;
 *     &lt;Timestamp&gt;yyyy-mm-dd hh:mm:ss.fffffffff&lt;/Timestamp&gt;\n" +
 *   &lt;/AccountSnapshot&gt;\n";
 * </pre>
 */
public class XMLAccountSnapshot extends XMLDocument
{
	//~ Constructors ***********************************************************
	//TODO: lots to do
	public XMLAccountSnapshot(String aiName, long accountNumber, long maxCredit,
							  int credit, long costs, java.sql.Timestamp timestamp
							  ) throws Exception
	{
		String xmlDocument = "<AccountSnapshot version=\"1.0\">\n" + "  <ID>" +
			aiName + "</ID>\n" + "  <AccountNumber>" + accountNumber +
			"</AccountNumber>\n" + "  <CreditMax>" + maxCredit +
			"</CreditMax>\n" + "  <Credit>" + credit + "</Credit>\n" +
			"  <Costs>" + costs + "</Costs>\n" + "  <Timestamp>" + timestamp +
			"</Timestamp>\n" + "</AccountSnapshot>\n";
		setDocument(xmlDocument);
	}

	public XMLAccountSnapshot(String xml) throws Exception
	{
		setDocument(xml);
	}
}

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

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Datencontainer: Heirmit fordert die AI Daten von Pay an bzw. teilt mit das sie überhaupt bezahlt werden will
 *
 * @author Grischan Gl&auml;nzel
 */
public class XMLPayRequest extends XMLDocument
{

	/*
	 <PayRequest>
	  <AIName> </AIName>  eindeutiger Name der AI (brauche ich zum Speichern des CCs)
	  <Accounting></Accounting>   true | false
	  <BalanceNeeded></BalanceNeeded>   true | false | new
	  <CostsConfirmationNeeded></CostsConfirmationNeeded> true | false | new
	  <Challenge> <Challenge>   xxxxxxxxxx
	 </PayRequest>
	 */

	//~ Public Fields  ******************************************************
	public static final String docStartTag = "<PayRequest>";
	public static final String docEndTag = "</PayRequest>";

	public static final String FALSE = "false";
	public static final String TRUE = "true";
	public static final String NEW = "new";

	//~ Instance fields ********************************************************

	public String aiName;
	public boolean accounting;
	public String balanceNeeded; // nur true | false oder new sollten hierin stehen getTrillian(); benutzen;
	public String costConfirmsNeeded;
	boolean error;
	long challenge;

	//~ Constructors ***********************************************************

	public XMLPayRequest(String aiName, boolean accounting, String balanceNeeded, String costConfirmsNeeded,
						 long challenge) throws Exception
	{
		this.aiName = aiName;
		this.accounting = accounting;
		this.balanceNeeded = getTrillian(balanceNeeded);
		this.costConfirmsNeeded = getTrillian(costConfirmsNeeded);

		this.error = error;
		this.challenge = challenge;
		StringBuffer sb = new StringBuffer();
		sb.append(docStartTag);
		sb.append("<AIName>" + aiName + "</AIName>");
		sb.append("<Acounting>" + accounting + "</Acounting>");
		sb.append("<BalanceNeeded>" + balanceNeeded + "</BalanceNeeded>");
		sb.append("<CostsConfirmationNeeded>" + costConfirmsNeeded + "</CostsConfirmationNeeded>");
		sb.append("<Challenge>" + challenge + "</Challenge>");
		sb.append(docEndTag);
		setDocument(sb.toString());
	}

	public XMLPayRequest(byte[] data) throws Exception
	{
		setDocument(data);

		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals(docStartTag))
		{
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("AIName");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		CharacterData chdata = (CharacterData) element.getFirstChild();
		aiName = chdata.getData();

		nl = element.getElementsByTagName("Accounting");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		accounting = Boolean.valueOf(chdata.getData()).booleanValue();

		nl = element.getElementsByTagName("BalanceNeeded");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		balanceNeeded = getTrillian(chdata.getData());

		nl = element.getElementsByTagName("CostConfirmationNeeded");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		costConfirmsNeeded = getTrillian(chdata.getData());

		nl = element.getElementsByTagName("Challenge");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		challenge = Long.parseLong(chdata.getData());
	}

	public static String getTrillian(String st)
	{
		if (st.equals(TRUE) || st.equals(NEW))
		{
			return st;
		}
		else
		{
			return FALSE;
		}
	}
}

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

import org.w3c.dom.*;
import javax.xml.parsers.*;
import anon.util.XMLUtil;
import java.io.*;
/**
 * Datencontainer f&uuml;r {@link XMLBalance} und {@link XMLCostConfirmations}
 *
 * @author Andreas M&uuml;ller,Grischan Gl&auml;nzel
 */
public class XMLBalConf
{
	//~ Instance fields ********************************************************

	public XMLBalance balance;
	public XMLCostConfirmations confirmations;

	//~ Constructors ***********************************************************

	public XMLBalConf(XMLBalance balance, XMLCostConfirmations confirmations)
	{
		this.balance = balance;
		this.confirmations = confirmations;
	}

	public XMLBalConf(String data) throws Exception
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(new ByteArrayInputStream(data.getBytes()));
		Element elemRoot = doc.getDocumentElement();
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");
		balance = new XMLBalance(elem);
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "CostConfirmations");
		if (elem != null)
		{
			confirmations = new XMLCostConfirmations(elem);
		}
		else
		{
			confirmations = new XMLCostConfirmations();
		}
	}

}

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

import java.util.Enumeration;
import java.util.Vector;

/**
 * XML Datencontainer für mehrere XMLEasyCC's (cost confirmation) sowie Methoden diese zu verwalten
 * Die Cost Confirmation wird vom Jap an die Ai geschickt
 * @author Grischan Gl&auml;nzel
 */

public class XMLCostConfirmations extends XMLDocument
{
	//~ Public Fields  ******************************************************
	public static final String docStartTag = "<Confirmations>";
	public static final String docEndTag = "</Confirmations>";

	//~ Instance fields ********************************************************
	private Vector confs;

	//~ Constructors ***********************************************************

	public XMLCostConfirmations(String data) throws Exception
	{
		confs = new Vector();
		xmlToVector(data);
		//setDocument(data);
	}

	public XMLCostConfirmations()
	{
		confs = new Vector();
	}

	//~ Methods ****************************************************************
	private void xmlToVector(String xml) throws Exception
	{
		String sub;
		String tmp = xml;
		//String tmp = new String(accountsBytes);
		int first, second, off;
		off = 0;
		if (tmp.indexOf(docStartTag) == -1)
		{
			throw new Exception("wrong password or corrupt accountfile");
		}
		while ( (first = tmp.indexOf(XMLEasyCC.docStartTag, off)) != -1)
		{
			second = tmp.indexOf(XMLEasyCC.docEndTag, off) + XMLEasyCC.docEndTag.length();
			off = second;
			sub = tmp.substring(first, second);
			try
			{
				XMLEasyCC easyCC = new XMLEasyCC(sub.getBytes());
				confs.addElement(easyCC);
			}
			catch (Exception e)
			{
				throw new Exception("wrong password or corrupt accountfile");
			}
		}
	}

	/**
	 *	gibt die gesuchte KostenBestätigung heraus oder null wenn keine für diesen namen vorhanden ist
	 */
	public XMLEasyCC getCC(String ai, long accountNumber)
	{
		XMLEasyCC cc;
		Enumeration en = confs.elements();
		while (en.hasMoreElements())
		{
			cc = ( (XMLEasyCC) en.nextElement());
			if (cc.getAIName().equals(ai) && cc.getAccountNumber() == accountNumber)
			{
				return cc;
			}
		}
		return null;
	}

	/**
	 *	gibt die gesuchte KostenBestätigung heraus oder erschafft eine neue;
	 *  fügt dieser den einen Byte Betrag hinzu
	 *	@param ai Der eindeutige Name der AI
	 *	@param accountNumber Account Nummer
	 *	@param plusBytes transferierte Daten seit dem letzten aufruf update.
	 */
	public XMLEasyCC updateCC(String ai, long accountNumber, long plusBytes)
	{
		XMLEasyCC cc = getCC(ai, accountNumber);
		if (cc == null)
		{
			try
			{
				cc = new XMLEasyCC(ai, accountNumber, 0);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}
		cc.addTransferredBytes(plusBytes);
		return cc;
	}

	/**
	 *	prüft ob bereits eine entprechende CC besteht. Überschreibt sie wenn der transferredBytes Wert höher ist;
	 *  oder fügt neu hinzu wenn keine entsprechende CC besteht
	 *	@param cc hinzuzufügende CostConfirmation
	 */
	public void addCC(XMLEasyCC easyCC)
	{
		XMLEasyCC cc = getCC(easyCC.getAIName(), easyCC.getAccountNumber());
		if (cc == null)
		{
			confs.addElement(easyCC);
		}
		else if (cc.getTransferredBytes() < easyCC.getTransferredBytes())
		{
			System.out.println("neu CC ist wirklich neuer");
			confs.removeElement(cc);
			confs.addElement(easyCC);
			//cc = easyCC;
		}
	}

	public String getXMLString(boolean withHead)
	{
		//TODO: wrong!!!
		StringBuffer buffer = new StringBuffer();
		buffer.append(docStartTag + "\n");
		Enumeration enum = confs.elements();
		while (enum.hasMoreElements())
		{
			XMLEasyCC confirm = (XMLEasyCC) enum.nextElement();
			buffer.append(confirm.getXMLString() + "\n");
		}
		buffer.append(docEndTag);
		if (withHead)
		{
			return XML_HEAD + buffer.toString();
		}
		else
		{
			return buffer.toString();
		}
	}
}

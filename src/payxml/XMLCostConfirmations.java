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
import org.w3c.dom.*;
import anon.util.*;
/**
 * XML Datencontainer für mehrere XMLEasyCC's (cost confirmation) sowie Methoden diese zu verwalten
 * Die Cost Confirmation wird vom Jap an die Ai geschickt
 * @author Grischan Gl&auml;nzel
 */

public class XMLCostConfirmations extends XMLDocument
{
	//~ Instance fields ********************************************************
	private Vector m_Confs;

	//~ Constructors ***********************************************************

	public XMLCostConfirmations(String data) throws Exception
	{
		setDocument(data);
		setValues();
	}
	/**
	 * Creates an CostConfirmation from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the CostConfirmations
	 */
	public XMLCostConfirmations(Node xml) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,xml,true);
		m_theDocument.appendChild(n);
		setValues();
	}

	public XMLCostConfirmations() throws Exception
	{
		m_Confs = new Vector();
		constructXMLDocument();
	}

	//~ Methods ****************************************************************
	private void setValues() throws Exception
	{
		m_Confs = new Vector();
		Element elemRoot=m_theDocument.getDocumentElement();

		if (!elemRoot.getNodeName().equals("CostConfirmations"))
		{
			throw new Exception("wrong password or corrupt accountfile");
		}
		Node n=elemRoot.getFirstChild();
		while ( n!=null)
		{
			try
			{
				XMLEasyCC easyCC = new XMLEasyCC(n);
				m_Confs.addElement(easyCC);
			}
			catch (Exception e)
			{
				throw new Exception("wrong password or corrupt accountfile");
			}
			n=n.getNextSibling();
		}
	}

	private void constructXMLDocument() throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Element elemRoot=m_theDocument.createElement("CostConfirmations");
		for(int i=0;i<m_Confs.size();i++)
		{
			XMLEasyCC cc=(XMLEasyCC)m_Confs.elementAt(i);
			Node n=XMLUtil.importNode(m_theDocument,cc.getDomDocument().getDocumentElement(),true);
			elemRoot.appendChild(n);
		}
	}
	/**
	 *	gibt die gesuchte KostenBestätigung heraus oder null wenn keine für diesen namen vorhanden ist
	 */
	public XMLEasyCC getCC(String ai, long accountNumber)
	{
		XMLEasyCC cc;
		Enumeration en = m_Confs.elements();
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
	public void addCC(XMLEasyCC easyCC) throws Exception
	{
		XMLEasyCC cc = getCC(easyCC.getAIName(), easyCC.getAccountNumber());
		if (cc == null)
		{
			m_Confs.addElement(easyCC);
		}
		else if (cc.getTransferredBytes() < easyCC.getTransferredBytes())
		{
			System.out.println("neu CC ist wirklich neuer");
			m_Confs.removeElement(cc);
			m_Confs.addElement(easyCC);
			//cc = easyCC;
		}
		constructXMLDocument();
	}

}

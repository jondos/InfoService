package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;

import java.io.*;

import java.util.*;

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

	public XMLCostConfirmations(String data) throws Exception{
		confs = new Vector();
		xmlToVector(data);
		//setDocument(data);
	}
	public XMLCostConfirmations(){
		confs = new Vector();
	}

	//~ Methods ****************************************************************
	private void xmlToVector(String xml) throws Exception{
		String sub;
		String tmp = xml;
		//String tmp = new String(accountsBytes);
		int first, second, off;
		off = 0;
		if(tmp.indexOf(docStartTag)==-1)
			throw new Exception("wrong password or corrupt accountfile");
		while((first=tmp.indexOf(XMLEasyCC.docStartTag, off))!=-1)
		{
			second = tmp.indexOf(XMLEasyCC.docEndTag, off)+XMLEasyCC.docEndTag.length();
			off=second;
			sub = tmp.substring(first,second);
			try
			{
				XMLEasyCC easyCC = new XMLEasyCC(sub.getBytes());
				confs.add(easyCC);
			}
			catch(Exception e)
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
		while(en.hasMoreElements()){
			cc = ((XMLEasyCC) en.nextElement());
			if(cc.getAIName().equals(ai)&&cc.getAccountNumber()==accountNumber) return cc;
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
	public XMLEasyCC updateCC(String ai,long accountNumber,long plusBytes)
	{
			XMLEasyCC cc = getCC(ai,accountNumber);
			if(cc==null){
				try{
					cc = new XMLEasyCC(ai,accountNumber,0);
				}catch(Exception ex){
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
			XMLEasyCC cc = getCC(easyCC.getAIName(),easyCC.getAccountNumber());
			if(cc==null) confs.add(easyCC);
			else if(cc.getTransferredBytes()<easyCC.getTransferredBytes()){
				System.out.println("neu CC ist wirklich neuer");
				confs.remove(cc);
				confs.add(easyCC);
				//cc = easyCC;
			}
	}


	public String getXMLString(boolean withHead)
	{
		StringBuffer buffer = new StringBuffer();
        buffer.append(docStartTag+"\n");
        Enumeration enum = confs.elements();
		while(enum.hasMoreElements())
		{
			XMLEasyCC confirm= (XMLEasyCC) enum.nextElement();
		    buffer.append(confirm.getXMLString(false)+"\n");
		}
		buffer.append(docEndTag);
		if (withHead) {
			return XML_HEAD + buffer.toString();
		} else {
			return buffer.toString();
		}
	}
}

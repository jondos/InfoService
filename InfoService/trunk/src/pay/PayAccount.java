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
package pay;
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import pay.util.Log;
import payxml.XMLBalance;
import payxml.XMLCertificate;
import payxml.XMLCostConfirmations;
import payxml.XMLDocument;
import payxml.XMLTransCert;
import payxml.util.Base64;
/**
 *  Diese Klasse ist für die verwaltung eines Accounts zutändig, sie kapselt eine XML Struktur innerhalb der Klasse
 *	und Mithilfe von Klassen des payxml Packages
 *	* @author Andreas Mueller, Grischan Glänzel
 */
public class PayAccount extends XMLDocument
{
		private XMLCertificate certificate;
		private XMLBalance balance;
		private RSAPrivateCrtKeyParameters privateKey;
		private Vector transCerts;
		private XMLCostConfirmations costConfirms;


	// Exceptions
	public static class WrongCertificateException extends Exception{};
	public static class WrongCCsException extends Exception{};

		/**
		 * Erzeugt ein PayAccount Objekt aus einem XML-Dokument.
		 *
		 * @param xmlData XML-Dokument
		 * @throws Exception Wenn XML-Dokument fehlerhaft
		 */
		public PayAccount(byte[] xmlData) throws Exception
		{
		transCerts = new Vector();
				costConfirms = new XMLCostConfirmations();
				setDocument(xmlData);
				setXMLCertificate();
				setXMLBalance();
				setRSAPrivateKey();
				setXMLTransCert();
				setXMLCostConfirms();
				Log.log(this,"PayAccount Object AccountNr."+certificate.getAccountNumber()+" complete",Log.SHORT_DEBUG);
		}

		/**
		 * Erzeugt ein {@link PayAccount} Objekt aus einem Kontozertifikat und dem
		 * zugehörigen geheimen Schlüssel.
		 *
		 * @param certificate Kontozertifikat
		 * @param privateKey geheimer Schlüssel
		 */
		public PayAccount(XMLCertificate certificate, RSAPrivateCrtKeyParameters privateKey)
		{
				this.certificate=certificate;
				this.privateKey=privateKey;
				this.transCerts= new Vector();
				this.costConfirms = new XMLCostConfirmations();
		}

		/**
		 * Liefert die XML-Präsentation des Kontos.
		 *
		 * @param withHead Mit XML-Kopf?
		 * @return XML-Dokument als String
		 */
		public String getXMLString(boolean withHead)
		{
				StringBuffer buffer = new StringBuffer(512);
				buffer.append("<Account>"+certificate.getXMLString(false));
				buffer.append("<RSAPrivateKey><Modulus>");
				buffer.append(Base64.encode(privateKey.getModulus().toByteArray()));
				buffer.append("</Modulus><PublicExponent>");
				buffer.append(Base64.encode(privateKey.getPublicExponent().toByteArray()));
				buffer.append("</PublicExponent><PrivateExponent>");
				buffer.append(Base64.encode(privateKey.getExponent().toByteArray()));
				buffer.append("</PrivateExponent><P>");
				buffer.append(Base64.encode(privateKey.getP().toByteArray()));
				buffer.append("</P><Q>");
				buffer.append(Base64.encode(privateKey.getQ().toByteArray()));
				buffer.append("</Q><dP>");
				buffer.append(Base64.encode(privateKey.getDP().toByteArray()));
				buffer.append("</dP><dQ>");
				buffer.append(Base64.encode(privateKey.getDQ().toByteArray()));
				buffer.append("</dQ><QInv>");
				buffer.append(Base64.encode(privateKey.getQInv().toByteArray()));
				buffer.append("</QInv></RSAPrivateKey>");

				buffer.append("<TransferCertificates>\n");
				Enumeration enum = transCerts.elements();
				while(enum.hasMoreElements())
				{
						XMLTransCert cert = (XMLTransCert) enum.nextElement();
						buffer.append(cert.getXMLString(false));
				}
				buffer.append("</TransferCertificates>\n");

				if(balance!=null)
				{
					buffer.append(balance.getXMLString(false));
					buffer.append(costConfirms.getXMLString(false));

			// passiert jetzt in XMLCostConfirmations

			/*
			buffer.append("<Confirmations>");
					enum = costConfirms.elements();
					while(enum.hasMoreElements())
					{
							XMLCostConfirmation confirm= (XMLCostConfirmation) enum.nextElement();
							buffer.append(confirm.getXMLString(false));
					}
					buffer.append("</Confirmations>");
					*/
		}


				buffer.append("</Account>");
				String xmlDocument =buffer.toString();

				if(withHead) return XML_HEAD+xmlDocument;
				return xmlDocument;
		}

		/**
		 * Hinzufügen eines Transfer-Zertifikats.
		 *
		 * @param cert Transfer-Zertifikat
		 */
		public void addTransCert(XMLTransCert cert)
		{
				transCerts.addElement(cert);
		}

		/**
		 * Setzen des Kontoguthabens.
		 *
		 * @param balance Kontoguthaben
		 */
		public void setBalance(XMLBalance balance)
		{
			 this.balance=balance;
		}

		public void setCostConfirms(XMLCostConfirmations costConfirms)
	{
			this.costConfirms=costConfirms;
		}


		private void setXMLCertificate() throws Exception
		{
		Log.log(this,xmlDocument,Log.LONG_DEBUG);
			String st = xmlDocument.substring(xmlDocument.indexOf(XMLCertificate.docElementName),xmlDocument.indexOf("</AccountCertificate>")+21);
					certificate = new XMLCertificate(st);
		}

		private void setXMLBalance()
		{
				try
				{
						balance = new XMLBalance(xmlDocument.substring(xmlDocument.indexOf(XMLBalance.docStartTag),xmlDocument.indexOf(XMLBalance.docEndTag)+XMLBalance.docEndTag.length()));
				}
				catch(Exception e)
				{
						balance=null;
				}
		}

		private void setXMLTransCert() throws Exception
		{
				String certsString;
				try
				{
						certsString = xmlDocument.substring(xmlDocument.indexOf("<TransferCertificates>")+22,xmlDocument.indexOf("</TransferCertificates>"));
						Log.log(this,certsString,Log.LONG_DEBUG);

				}
				catch(Exception e)
				{
						return;
				}

				int first, last, index=0;
				while((first=certsString.indexOf(XMLTransCert.docStartTag,index))!=-1)
				{
						last=certsString.indexOf(XMLTransCert.docEndTag,first)+XMLTransCert.docEndTag.length();
						String tmp = certsString.substring(first,last);
						index=last;
						try
						{
						XMLTransCert cert = new XMLTransCert(tmp);
						transCerts.addElement(cert);
						}
						catch(Exception e)
						{
								continue;
						}
				}
		}

		private void setXMLCostConfirms()
		{
		try{
			String confirms= xmlDocument.substring(xmlDocument.indexOf(XMLCostConfirmations.docStartTag),xmlDocument.indexOf(XMLCostConfirmations.docEndTag)+XMLCostConfirmations.docEndTag.length());
					costConfirms = new XMLCostConfirmations(confirms);
		}catch(Exception ex){
			Log.log(this,"noch keine CostConfirmations vorhanden",Log.INFO);
			costConfirms = new XMLCostConfirmations();
		}
		}

		private void setRSAPrivateKey() throws Exception
		{
				Element element = domDocument.getDocumentElement();
				NodeList nl= element.getElementsByTagName("RSAPrivateKey");
				if (nl.getLength()<1) throw new Exception();
				element = (Element)nl.item(0);
				nl= element.getElementsByTagName("Modulus");
				if (nl.getLength()<1) throw new Exception();
				Element tmpElement=(Element)nl.item(0);
				CharacterData chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger modulus=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("PublicExponent");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger publicExponent=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("PrivateExponent");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger privateExponent=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("P");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger p=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("Q");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger q=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("dP");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger dP=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("dQ");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger dQ=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				nl= element.getElementsByTagName("QInv");
				if (nl.getLength()<1) throw new Exception();
				tmpElement=(Element)nl.item(0);
				chdata = (CharacterData)tmpElement.getFirstChild();
				BigInteger qInv=
				new BigInteger(Base64.decode(chdata.getData().toCharArray()));

				privateKey = new RSAPrivateCrtKeyParameters(modulus, publicExponent, privateExponent, p, q, dP, dQ, qInv);

		}

		/**
		 * Liefert Kontonummer des Kontos.
		 *
		 * @return Kontonummer
		 */
		public long getAccountNumber()
		{
				return certificate.getAccountNumber();
		}

		/**
		* gibt an ob überhaupt ein Kontostanddokument existiert
		*/
		public boolean hasBalance(){
	return balance!=null;
		}


		/**
		 * Liefert das Kontozertifikat.
		 *
		 * @return Kontozertifikat
		 */
		public String getAccountCertificate()
		{
				return certificate.getXMLString(false);
		}

		/**
		 * Liefert das Gültigkeitsdatum des Kontos.
		 *
		 * @return Gültigkeitsende
		 */
		public Date getValidFrom()
		{
				return certificate.getCreationTime();
		}
		public Date getValidTo()
	{
		if (balance!=null) return balance.getValidTime();
			return certificate.getCreationTime();
		}

		/**
		 * Liefert den geheimen Schlüssel des Kontos.
		 *
		 * @return Geheimer Schlüssel
		 */
		public RSAKeyParameters getPrivateKey()
		{
				return privateKey;
		}

		/**
		 * Liefert den öffentlichen Schlüssel des Kontos.
		 *
		 * @return Öffentlicher Schlüssel
		 */
		public RSAKeyParameters getPublicKey()
		{
				return new RSAKeyParameters(false,certificate.getPublicKey().getModulus(), certificate.getPublicKey().getExponent());
		}

		/**
		 * Liefert die Gesamtsumme des eingezahlten Geldes.
		 *
		 * @return Gesamtsumme
		 */
		public long getCreditMax()
		{
				if (balance!=null) return balance.getCreditMax();
	return 0L;
		}

		/**
		 * Liefert das noch verbleibene Guthaben.
		 *
		 * @return Guthaben
		 */
		public long getCredit()
		{
				if (balance!=null) return balance.getCredit();
			return 0L;
		}

		/**
		 * Liefert die Kostenbestätigungen.
		 *
		 * @return Vector von CostConfirms
		 */
		public XMLCostConfirmations getCostConfirms()
		{
				return costConfirms;
		}

		public XMLBalance getBalance()
	{
			return balance;
		}


		/**
		 * Liefert alle Transfer-Zertifikate.
		 *
		 * @return Vector von {@link XMLTransCert}
		 */
		public Vector getTransCerts()
		{
				return transCerts;
		}
}

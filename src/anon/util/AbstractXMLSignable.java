package anon.util;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.JAPSignature;

/**
 * Abstract class that implements the common signing and verifying of XML
 * signatures.
 * @author Bastian Voigt
 * @version 1.0
 */
public abstract class AbstractXMLSignable implements IXMLSignable
{
	/** the signature as DOM representation */
	protected Document m_signature;

	/**
	 * signs the document with the given JAPSignature object.
	 * @param signer JAPSignature must be initialized for signing
	 */
	public void sign(JAPSignature signer) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(this);
		signer.signXmlDoc(doc);
		Element elemSig = (Element) XMLUtil.getFirstChildByName(doc.getDocumentElement(), "Signature");
		m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elem = (Element) XMLUtil.importNode(m_signature, elemSig, true);
		m_signature.appendChild(elem);
	}

	/**
	 * Verifies the signature with the given JAPSignature object.
	 * @param verifier JAPSignature must be initialized for verifying
	 * @throws Exception
	 * @return boolean
	 */
	public boolean verifySignature(JAPSignature verifier)
	{
		try
		{
			Document doc = XMLUtil.toXMLDocument(this);
			return verifier.verifyXML(doc.getDocumentElement());
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * isSigned
	 *
	 * @return boolean true if the document is signed, false otherwise
	 */
	public boolean isSigned()
	{
		return (m_signature != null);
	}

}

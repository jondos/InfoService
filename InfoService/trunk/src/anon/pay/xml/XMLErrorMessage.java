package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class encapsulates an error or success message.
 * In order to be indipendent from the HTTP protocol on the higher layer,
 * this is now used instead of http errorcodes.
 *
 * @author Bastian Voigt
 */
public class XMLErrorMessage implements IXMLEncodable
{
	public static final int ERR_OK = 0;
	public static final int ERR_INTERNAL_SERVER_ERROR = 1;
	public static final int ERR_WRONG_FORMAT = 2;
	public static final int ERR_WRONG_DATA = 3;
	public static final int ERR_KEY_NOT_FOUND = 4;
	public static final int ERR_BAD_SIGNATURE = 5;
	public static final int ERR_BAD_REQUEST = 6;
	public static final int ERR_NO_ACCOUNTCERT = 7;
	public static final int ERR_NO_BALANCE = 8;
	public static final int ERR_NO_CONFIRMATION = 9;
	public static final int ERR_ACCOUNT_EMPTY = 10;

	private int m_iErrorCode;
	private String m_strErrMsg;

	/** default error descriptions */
	private static final String[] m_errStrings =
		{
		"Success", "Internal Server Error",
		"Wrong format", "Wrong Data", "Key not found", "Bad Signature", "Bad request",
		"No account certificate", "No balance", "No cost confirmation",
		"Account is empty"
	};

	public static final String XML_ELEMENT_NAME = "ErrorMessage";

	/**
	 * Parses an XMLErrorMessage object from DOM Document
	 *
	 * @param document Document
	 */
	public XMLErrorMessage(Document doc) throws Exception
	{
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}

	/**
	 * XMLErrorMessage
	 *
	 * @param element Element
	 */
	public XMLErrorMessage(Element element) throws Exception
	{
		setValues(element);
	}


	/**
	 * Creates an errorMessage object. The errorcode should be one of the
	 * above ERR_* constants.
	 * @param errorCode int one of the above constants
	 * @param message String a human-readable description of the error
	 */
	public XMLErrorMessage(int errorCode, String message)
	{
		m_iErrorCode = errorCode;
		m_strErrMsg = message;
	}

	/**
	 * Uses a default description String
	 * @param errorCode int
	 */
	public XMLErrorMessage(int errorCode)
	{
		m_iErrorCode = errorCode;
		if (m_iErrorCode < 0 || m_iErrorCode > m_errStrings.length)
		{
			m_strErrMsg = "Unknown Error";
		}
		else
		{
			m_strErrMsg = m_errStrings[errorCode];
		}
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("code", Integer.toString(m_iErrorCode));
		XMLUtil.setNodeValue(elemRoot, m_strErrMsg);
		return elemRoot;
	}

	public String getErrorDescription()
	{
		return m_strErrMsg;
	}

	public int getErrorCode()
	{
		return m_iErrorCode;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (! (elemRoot.getTagName().equals(XML_ELEMENT_NAME)))
		{
			throw new Exception("Format error: Root element wrong tagname");
		}
		m_iErrorCode = Integer.parseInt(elemRoot.getAttribute("code"));
		m_strErrMsg = XMLUtil.parseNodeString(elemRoot, "");

	}
}

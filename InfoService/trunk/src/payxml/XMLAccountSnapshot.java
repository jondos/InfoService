package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;


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

	public XMLAccountSnapshot(String aiName, long accountNumber, long maxCredit,
		int credit, long costs, java.sql.Timestamp timestamp
	) throws Exception
	{
		xmlDocument = "<AccountSnapshot version=\"1.0\">\n" + "  <ID>" +
			aiName + "</ID>\n" + "  <AccountNumber>" + accountNumber +
			"</AccountNumber>\n" + "  <CreditMax>" + maxCredit +
			"</CreditMax>\n" + "  <Credit>" + credit + "</Credit>\n" +
			"  <Costs>" + costs + "</Costs>\n" + "  <Timestamp>" + timestamp +
			"</Timestamp>\n" + "</AccountSnapshot>\n";
		setDocument(xmlDocument);
	}


	public XMLAccountSnapshot(String xml) throws Exception
	{
		xmlDocument = xml;
		setDocument(xml);
	}
}

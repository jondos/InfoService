package payxml;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Datencontainer: Heirmit fordert die AI Daten von Pay an bzw. teilt mit das sie überhaupt bezahlt werden will
 *
 * @author Grischan Gl&auml;nzel
 */
public class XMLPayRequest extends XMLDocument{

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
	public String balanceNeeded;  		// nur true | false oder new sollten hierin stehen getTrillian(); benutzen;
	public String costConfirmsNeeded;
	boolean error;
	long challenge;

	//~ Constructors ***********************************************************

	public XMLPayRequest(String aiName, boolean accounting, String balanceNeeded, String costConfirmsNeeded, long challenge) throws Exception{
		this.aiName = aiName;
		this.accounting = accounting;
		this.balanceNeeded = getTrillian(balanceNeeded);
		this.costConfirmsNeeded = getTrillian(costConfirmsNeeded);

		this.error = error;
		this.challenge = challenge;
		StringBuffer sb = new StringBuffer();
		sb.append(docStartTag);
		sb.append("<AIName>"+aiName+"</AIName>");
		sb.append("<Acounting>"+accounting+"</Acounting>");
		sb.append("<BalanceNeeded>"+balanceNeeded+"</BalanceNeeded>");
		sb.append("<CostsConfirmationNeeded>"+costConfirmsNeeded+"</CostsConfirmationNeeded>");
		sb.append("<Challenge>"+challenge+"</Challenge>");
		sb.append(docEndTag);
		setDocument(sb.toString());
	}
	public XMLPayRequest(byte[] data) throws Exception{
			setDocument(data);

			Element element = domDocument.getDocumentElement();
			if (!element.getTagName().equals(docStartTag)) {
				throw new Exception();
			}

			NodeList nl = element.getElementsByTagName("AIName");
			if (nl.getLength() < 1) {
				throw new Exception();
			}
			element = (Element) nl.item(0);
			CharacterData chdata = (CharacterData) element.getFirstChild();
			aiName = chdata.getData();

			nl = element.getElementsByTagName("Accounting");
			if (nl.getLength() < 1) {
				throw new Exception();
			}
			element = (Element) nl.item(0);
			chdata = (CharacterData) element.getFirstChild();
			accounting = Boolean.valueOf(chdata.getData()).booleanValue();

			nl = element.getElementsByTagName("BalanceNeeded");
			if (nl.getLength() < 1) {
				throw new Exception();
			}
			element = (Element) nl.item(0);
			chdata = (CharacterData) element.getFirstChild();
			balanceNeeded = getTrillian(chdata.getData());

			nl = element.getElementsByTagName("CostConfirmationNeeded");
			if (nl.getLength() < 1) {
				throw new Exception();
			}
			element = (Element) nl.item(0);
			chdata = (CharacterData) element.getFirstChild();
			costConfirmsNeeded = getTrillian(chdata.getData());

			nl = element.getElementsByTagName("Challenge");
			if (nl.getLength() < 1) {
				throw new Exception();
			}
			element = (Element) nl.item(0);
			chdata = (CharacterData) element.getFirstChild();
			challenge = Long.parseLong(chdata.getData());
	}

	public static String getTrillian(String st){
		if(st.equals(TRUE)||st.equals(NEW)) return st;
		else return FALSE;
	}
}

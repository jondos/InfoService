package payxml;

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
	public XMLBalConf(String data){
		int index = data.indexOf(XMLCostConfirmations.docStartTag);
		try{
			if (index>0){
					balance = new XMLBalance(data.substring(0,index));
				confirmations = new XMLCostConfirmations(data.substring(index));
			}
			else{
				balance = new XMLBalance(data);
				confirmations=new XMLCostConfirmations();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

	}
}

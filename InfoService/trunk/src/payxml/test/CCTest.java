package payxml.test;

import payxml.*;

public class CCTest{

	public static void main(String[] ewr){
		try{
			XMLCostConfirmations ccs =new XMLCostConfirmations();
			ccs.addCC(new XMLEasyCC("berlin1",78,0));
			ccs.addCC(new XMLEasyCC("berlin2",345,0));
			ccs.addCC(new XMLEasyCC("berlin3",345,100));
			ccs.addCC(new XMLEasyCC("berlin1",78,200));
			ccs.addCC(new XMLEasyCC("berlin3",345,34));
			System.out.println(ccs.getXMLString(false));
		}catch(Exception ex){
			ex.printStackTrace();
		}

	}
}
package pay.view;

import jap.*;
import javax.swing.*;

/**
 * The Jap Conf Module for the Accounts and payment Management
*
 * @author Bastian Voigt
 * @version 1.0
 */
public class JAPConfPayAccounts extends jap.AbstractJAPConfModule
{
	public JAPConfPayAccounts()
	{
		super(null);
	}

	/**
	 * getTabTitle
	 *
	 * @return String
	 * @todo internationalize
	 */
	public String getTabTitle()
	{
		return "Konten";
	}

	/**
	 * recreateRootPanel
	 */
	public void recreateRootPanel()
	{
	}
}

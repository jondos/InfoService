package pay.gui;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.ImageIcon;

import jap.JAPConstants;
import jap.JAPModel;
import jap.JAPUtil;
import java.awt.Image;
import java.awt.MediaTracker;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import java.awt.Dimension;

/**
 * This class is the main payment view on JAP's main gui window
 *
 * @author Bastian Voigt
 * @version 1.0
 */

public class PaymentMainPanel extends JPanel
{
	/**
	 * Icons for the account icon display
	 */
	private ImageIcon[] m_accountIcons;

	/**
	 * Loads some icons for the account display
	 */
	protected void loadIcons()
	{
		// Load Images for Account Icon Display
		m_accountIcons = new ImageIcon[JAPConstants.ACCOUNTICONFNARRAY.length];
		if (!JAPModel.isSmallDisplay())
		{
			for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
			{
				m_accountIcons[i] = JAPUtil.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], false);
			}
		}
		else // scale down for small displays
		{
			MediaTracker m = new MediaTracker(this);
			for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
			{
				Image tmp = JAPUtil.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], true).getImage();
				int w = tmp.getWidth(null);
				tmp = tmp.getScaledInstance( (int) (w * 0.75), -1, Image.SCALE_SMOOTH);
				m.addImage(tmp, i);
				m_accountIcons[i] = new ImageIcon(tmp);
			}
			try
			{
				m.waitForAll();
			}
			catch (Exception e)
			{}
		}
	}

	/** shows different coin icons */
	private JLabel m_AccountIconLabel;

	/** shows the current balance state */
	private JProgressBar m_BalanceProgressBar;

	/** shows the current balance as text */
	private JLabel m_BalanceText;

	/** shows the account number and the timestamp of last update */
	private JLabel m_AccountText;

	/** this button opens the configuration tab for payment */
	private JButton m_ConfigButton;

	public PaymentMainPanel()
	{
		super(new BorderLayout());
		loadIcons();

		setBorder(new TitledBorder("Account information"));
//		GridBagConstraints c = new GridBagConstraints();

		// show the icon label
		m_AccountIconLabel = new JLabel(m_accountIcons[1]);
/*		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 3.0;
		c.gridheight = 3;
		c.fill = GridBagConstraints.BOTH;
		layout.setConstraints(m_AccountIconLabel, c);*/
		this.add(m_AccountIconLabel, BorderLayout.WEST);

		JPanel centerPanel = new JPanel(new BorderLayout());
		// show the date of last update
		m_AccountText = new JLabel("Auszug vom: 27.07.77 17:17 Uhr");
		m_AccountText.setBorder(new EtchedBorder());
/*		c.gridx = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		layout.setConstraints(m_AccountText, c);*/
		centerPanel.add(m_AccountText, BorderLayout.NORTH);

		JPanel kontostandPanel = new JPanel(new BorderLayout());
		// show the current account balance
		m_BalanceProgressBar = new JProgressBar(0, 100);
		m_BalanceProgressBar.setValue(77);
/*		c.gridx = 2;
		c.gridy = 2;
		//	c.weightx=3.0;
		c.gridheight = 1;
		c.gridwidth=GridBagConstraints.RELATIVE;
		layout.setConstraints(m_BalanceProgressBar, c);*/
		kontostandPanel.add(m_BalanceProgressBar, BorderLayout.CENTER);

		m_BalanceText = new JLabel("\u20AC 177,07");
//		c.gridx = 3;
//		c.gridy = 2;
/*		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(m_BalanceText, c);*/
		kontostandPanel.add(m_BalanceText, BorderLayout.EAST);
		centerPanel.add(kontostandPanel, BorderLayout.SOUTH);
		this.add(centerPanel, BorderLayout.CENTER);

	    this.add(new JButton("Aufladen"), BorderLayout.EAST);
	}

}

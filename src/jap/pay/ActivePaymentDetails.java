package jap.pay;

import gui.dialog.JAPDialog;
import java.util.Vector;
import java.awt.Component;
import javax.swing.JDialog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.Dimension;
import javax.swing.Box;
import gui.JAPMessages;
import gui.JAPHtmlMultiLineLabel;
import anon.util.Util;
import javax.swing.JButton;
import gui.dialog.DialogContentPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import jap.JAPUtil;
import gui.GUIUtils;

/**
 * Shows details for active payments
 * invoked by clicking on "Details" for an active payment transaction in the transactions overview
 * Shows detailed info for ALL active payments
 * (maybe highlight the method that was originally selected for the TAN?)
 *
 * @author Elmar Schraml
 *
 */
public class ActivePaymentDetails extends JAPDialog implements ActionListener
{
	private static final String MSG_HEADING = ActivePaymentDetails.class.getName() + "_heading";
	private static final String MSG_TITLE = ActivePaymentDetails.class.getName() + "_title";
	private static final String MSG_CLOSEBUTTON = ActivePaymentDetails.class.getName() + "_closebutton";

	private GridBagConstraints m_c;
	private JButton m_closeButton;

	public ActivePaymentDetails(JAPDialog a_parent, Vector activeOptions, String a_transferNumber, String a_amount)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));

		try
			{
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				buildDialog(activeOptions, a_transferNumber, a_amount);
				//setSize(400,400);
				setResizable(false);
				pack();
				setVisible(true);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not create ActivePaymentDetails: ", e);
		}

    }

	private void buildDialog(Vector optionsToShow, String transferNumber, String amount)
	{
		m_c = new GridBagConstraints();
		m_c.anchor = m_c.NORTH;
		m_c.insets = new Insets(10, 30, 10, 30);
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weighty = 0;
		m_c.weightx = 0;
		getContentPane().setLayout(new GridBagLayout());

		JAPHtmlMultiLineLabel heading = new JAPHtmlMultiLineLabel("<h3>" + JAPMessages.getString(MSG_HEADING) + "</h3>");
		getContentPane().add(heading,m_c);
		m_c.gridy++;

		m_c.weightx = 0;
		Hashtable curOption;
		Vector optionPanels = new Vector(); //will store all the options, so we can set them to an equal size
		for (Enumeration options = optionsToShow.elements(); options.hasMoreElements(); )
		{
			curOption = (Hashtable) options.nextElement();
			m_c.gridy++;
			JPanel curOptionPanel = buildOptionPanel(curOption, transferNumber,amount);
			optionPanels.addElement(curOptionPanel);
			getContentPane().add(curOptionPanel,m_c);
			m_c.gridy++;
		}

	    //the various option panels' widths depends on the longest String on the panel,
		//so we make them all the same (widest) size
		Dimension largestPanel = GUIUtils.getMaxSize(optionPanels);
		GUIUtils.setSizes(optionPanels,largestPanel);

		m_closeButton = new JButton(JAPMessages.getString(MSG_CLOSEBUTTON));
		m_closeButton.addActionListener(this);
		m_c.gridy++;
		getContentPane().add(m_closeButton,m_c);
	}

	private JPanel buildOptionPanel(Hashtable optionToShow, String transferNumber, String amount)
	{
		JPanel optionPanel = new JPanel();
		BoxLayout verticalBoxLayout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);
		optionPanel.setLayout(verticalBoxLayout);

		//the option's name is not used, since heading servers as the localized, user-visible name of the option
		String curHeading = (String) optionToShow.get("heading");
		JAPHtmlMultiLineLabel headingLabel = new JAPHtmlMultiLineLabel("<b>" + curHeading + "</b>");
		optionPanel.add(headingLabel);
		optionPanel.add(Box.createRigidArea(new Dimension(0,10)));


        String curDetailedInfo = (String) optionToShow.get("detailedInfo");
		JAPHtmlMultiLineLabel detailsLabel = new JAPHtmlMultiLineLabel(curDetailedInfo);
		detailsLabel.setPreferredWidth(300);
		optionPanel.add(detailsLabel);
		optionPanel.add(Box.createRigidArea(new Dimension(0,10)));

		for (Enumeration extraInfos = ( (Vector) optionToShow.get("extraInfos")).elements(); extraInfos.hasMoreElements(); )
			{
				String extraInfoString =  (String) extraInfos.nextElement();
				extraInfoString = Util.replaceAll(extraInfoString,"%t", transferNumber);
				extraInfoString = Util.replaceAll(extraInfoString,"%a",amount);
				extraInfoString = Util.replaceAll(extraInfoString,"%c",""); //currency is not used, so get rid of the placeholder

				JAPHtmlMultiLineLabel extraInfoLabel = new JAPHtmlMultiLineLabel(extraInfoString);
				//extraInfoLabel.setPreferredWidth(300);
				optionPanel.add(extraInfoLabel);
				optionPanel.add(Box.createRigidArea(new Dimension(0,5)));
			}

		return optionPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_closeButton)
		{
			this.setVisible(false);
		}
	}

}

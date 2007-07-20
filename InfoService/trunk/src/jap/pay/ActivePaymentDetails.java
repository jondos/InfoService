package jap.pay;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import anon.util.Util;
import gui.GUIUtils;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import jap.JAPUtil;
import jap.pay.wizardnew.PaymentInfoPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;


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
	private static final String MSG_COPYBUTTON = ActivePaymentDetails.class.getName() + "_copybutton";
	private static final String MSG_PAYBUTTON = ActivePaymentDetails.class.getName() + "_paybutton";

	private GridBagConstraints m_c;
	private JButton m_closeButton;

	public ActivePaymentDetails(JAPDialog a_parent, Vector activeOptions, String a_transferNumber, long a_amount, String a_planName)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));

		try
			{
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				buildDialog(activeOptions, a_transferNumber, a_amount, a_planName);
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

	private void buildDialog(Vector optionsToShow, String transferNumber, long amount, String planName)
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
			JPanel curOptionPanel = buildOptionPanel(curOption, transferNumber,amount, planName);
			optionPanels.addElement(curOptionPanel);
			getContentPane().add(curOptionPanel,m_c);
		}

	    //the various option panels' widths depends on the longest String on the panel,
		//so we make them all the same (widest) size
		Dimension largestPanel = GUIUtils.getMaxSize(optionPanels);
		GUIUtils.setEqualWidths(optionPanels,largestPanel);

		m_closeButton = new JButton(JAPMessages.getString(MSG_CLOSEBUTTON));
		m_closeButton.addActionListener(this);
		m_c.gridy++;
		getContentPane().add(m_closeButton,m_c);
	}

	private JPanel buildOptionPanel(Hashtable optionToShow, String transferNumber, long amount, String planName)
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
		detailsLabel.setPreferredWidth(600);
		detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		optionPanel.add(detailsLabel);
		optionPanel.add(Box.createRigidArea(new Dimension(0,10)));

		for (Enumeration extraInfos = ( (Vector) optionToShow.get("extraInfos")).elements(); extraInfos.hasMoreElements(); )
			{
				String extraInfoString =  (String) extraInfos.nextElement();
				boolean isALink = true;
	            //check if it's a link or text
				try
				{
					URL urlToOpen = new URL(extraInfoString);
					//url is never used, just to see if it works, if yes the String is a link
				} catch ( MalformedURLException e)
				{
					isALink = false;
				}


				if (isALink)
				{
					if (extraInfoString.indexOf("paypal") != -1 )
					{
						extraInfoString = PaymentInfoPane.createPaypalLink(extraInfoString,amount,planName,transferNumber);
					}
					else
					{
						extraInfoString = Util.replaceAll(extraInfoString,"%t", transferNumber);
						extraInfoString = Util.replaceAll(extraInfoString,"%a",(new Long(amount)).toString());
						extraInfoString = Util.replaceAll(extraInfoString,"%c",""); //currency is not used, so get rid of the placeholder
					}
					//if a link, store it in final variable (for anonymous inner class ActionListeners), but don't show it
					final String linkToUse = extraInfoString;

					//add buttons and handlers
					JPanel linkButtonsPanel = new JPanel(); //default flow layout
					JButton bttnCopy = new JButton(JAPMessages.getString(MSG_COPYBUTTON));
					bttnCopy.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							copyToClipboard(linkToUse);
						}
					});
					linkButtonsPanel.add(bttnCopy);
					JButton bttnPay = new JButton(JAPMessages.getString(MSG_PAYBUTTON));
					bttnPay.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							openURL(linkToUse);
						}
					});
					linkButtonsPanel.add(bttnPay);
					linkButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
					optionPanel.add(linkButtonsPanel);
					optionPanel.add(Box.createRigidArea(new Dimension(0,5)));
				}
				else //regular text
				{
					//test could contain e.g. wiring instructions, so need to replace placeholders, too
				extraInfoString = Util.replaceAll(extraInfoString,"%t", transferNumber);
				extraInfoString = Util.replaceAll(extraInfoString,"%a",JAPUtil.formatEuroCentValue(amount));
				extraInfoString = Util.replaceAll(extraInfoString,"%c",""); //currency is not used, so get rid of the placeholder

				JAPHtmlMultiLineLabel extraInfoLabel = new JAPHtmlMultiLineLabel(extraInfoString);
				extraInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				optionPanel.add(extraInfoLabel);
				optionPanel.add(Box.createRigidArea(new Dimension(0,5)));
			}

			}
        optionPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		return optionPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_closeButton)
		{
			this.setVisible(false);
		}
	}


	public void openURL(String link)
	{
		AbstractOS os = AbstractOS.getInstance();
		link = cleanupLink(link);
		try
		{
			URL urlToOpen = new URL(link);
			os.openURL(urlToOpen);
		}
		catch (MalformedURLException me)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Malformed URL");
		}

	}

	private void copyToClipboard(String link)
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        link = cleanupLink(link);


		Transferable transfer = new StringSelection(link);
		sysClip.setContents(transfer, null);
	}

	private String cleanupLink(String link)
	{
		link = Util.replaceAll(link, "<br>", "");
		link = Util.replaceAll(link, "<p>", "");
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "&nbsp;", "%20");
		link = Util.replaceAll(link, " ", "%20");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();
		return link;
	}



}
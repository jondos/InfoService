/*
 Copyright (c) 2000-2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jap;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.border.Border;
import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import gui.dialog.JAPDialog;
import gui.dialog.*;
import gui.JAPMessages;
import gui.*;
import logging.*;
import java.awt.Cursor;

/**
 * This is some kind of isntallation and configuration assistant that helps the unexperienced
 * user to get the most out of JAP.
 *
 * @author Rolf Wendolsky
 */
public class ConfigAssistant extends JAPDialog
{
	private static final String BROWSER_IE = "Internet Explorer";
	private static final String BROWSER_FIREFOX = "Mozilla Firefox";
	private static final String BROWSER_OPERA = "Opera";
	private static final String BROWSER_KONQUEROR = "Konqueror";
	private static final String BROWSER_SAFARI = "Safari";

	private static final String MSG_WELCOME = ConfigAssistant.class.getName() + "_welcome";
	private static final String MSG_HELP = ConfigAssistant.class.getName() + "_help";
	private static final String MSG_TITLE = ConfigAssistant.class.getName() + "_title";
	private static final String MSG_FINISHED = ConfigAssistant.class.getName() + "_finished";
	private static final String MSG_BROWSER_CONF = ConfigAssistant.class.getName() + "_browserConf";
	private static final String MSG_RECOMMENDED = ConfigAssistant.class.getName() + "_recommended";
	private static final String MSG_OTHER_BROWSERS = ConfigAssistant.class.getName() + "_otherBrowsers";
	private static final String MSG_CLICK_TO_VIEW_HELP = ConfigAssistant.class.getName() +
		"_clickToViewHelp";
	private static final String MSG_BROWSER_TEST = ConfigAssistant.class.getName() + "_browserTest";
	private static final String MSG_MAKE_SELECTION = ConfigAssistant.class.getName() + "_makeSelection";

	private static final String MSG_ERROR_NO_WARNING = ConfigAssistant.class.getName() + "_errorNoWarning";
	private static final String MSG_EXPLAIN_NO_WARNING = ConfigAssistant.class.getName() + "_explainNoWarning";
	private static final String MSG_ERROR_NO_WARNING_AND_SURFING = ConfigAssistant.class.getName() +
		"_errorNoWarningAndSurfing";
	private static final String MSG_EXPLAIN_NO_DIRECT_CONNECTION = ConfigAssistant.class.getName() +
		"_explainNoDirectConnection";
	private static final String MSG_ERROR_WARNING_NO_SURFING = ConfigAssistant.class.getName() +
		"_errorWarningNoSurfing";
	private static final String MSG_SUCCESS_WARNING = ConfigAssistant.class.getName() + "_successWarning";
	private static final String MSG_REALLY_CLOSE = ConfigAssistant.class.getName() + "_reallyClose";
	private static final String MSG_DEACTIVATE_ACTIVE = ConfigAssistant.class.getName() +
		"_deactivateActiveContent";
	private static final String MSG_ANON_TEST = ConfigAssistant.class.getName() + "_anonTest";
	private static final String MSG_ERROR_NO_SERVICE_AVAILABLE = ConfigAssistant.class.getName() +
		"_errorNoServiceAvailable";
	private static final String MSG_ERROR_NO_CONNECTION = ConfigAssistant.class.getName() +
		"_errorNoConnection";
	private static final String MSG_ERROR_CONNECTION_SLOW = ConfigAssistant.class.getName() +
		"_errorConnectionSlow";
	private static final String MSG_ERROR_NO_SURFING = ConfigAssistant.class.getName() +
		"_errorNoSurfing";
	private static final String MSG_SUCCESS_CONNECTION = ConfigAssistant.class.getName() +
		"_successConnection";
	private static final String MSG_EXPLAIN_NO_CONNECTION = ConfigAssistant.class.getName() +
		"_explainNoConnection";
	private static final String MSG_EXPLAIN_BAD_CONNECTION = ConfigAssistant.class.getName() +
		"_explainBadConnection";
	private static final String MSG_EXPLAIN_NO_SERVICE_AVAILABLE = ConfigAssistant.class.getName() +
		"_explainNoServiceAvailable";


	private static final String IMG_ARROW = "arrow46.gif";
	private static final String IMG_HELP_BUTTON = ConfigAssistant.class.getName() + "_en_help.gif";
	private static final String IMG_SERVICES = ConfigAssistant.class.getName() + "_services.gif";

	private JTextPane m_lblHostname, m_lblPort;
	private JRadioButton m_radioNoWarning, m_radioNoWarningAndSurfing, m_radioSuccessWarning,
		m_radioErrorWarningNoSurfing;
	private ButtonGroup m_groupWarning;
	private JRadioButton m_radioNoConnection, m_radioConnectionSlow, m_noSurfing, m_ConnectionOK,
		m_radioNoServiceAvailable;
	private ButtonGroup m_groupAnon;

	public ConfigAssistant(JAPDialog a_parentDialog)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_TITLE), false);

		final JAPDialog thisDialog = this;
		JLabel tempLabel;
		Insets insets = new Insets(0, 0, 0, 5);
		ImageIcon wizardIcon = GUIUtils.loadImageIcon("install.gif");

		DialogContentPane.Layout layout = new DialogContentPane.Layout(wizardIcon);
		JLabel lblImage;
		Border border;
		border = BorderFactory.createRaisedBevelBorder();
		//border = BorderFactory.createLoweredBevelBorder();

		DialogContentPane paneWelcome = new SimpleWizardContentPane(
				  this, JAPMessages.getString(MSG_WELCOME), layout, null);

		DialogContentPane paneHelp = new SimpleWizardContentPane(
				  this,
				  JAPMessages.getString(MSG_HELP,
										"<a href=\"http://www.anon-online.de\">http://www.anon-online.de</a>"),
				  layout, new DialogContentPane.Options(paneWelcome));
		lblImage = new JLabel(GUIUtils.loadImageIcon(IMG_HELP_BUTTON));
		lblImage.setBorder(border);
		paneHelp.getContentPane().add(lblImage);


		DialogContentPane paneBrowserConf = new SimpleWizardContentPane(
				  this, JAPMessages.getString(MSG_BROWSER_CONF), layout, new DialogContentPane.Options(paneHelp))
		{
			public CheckError[] checkUpdate()
			{
				m_lblPort.setText("" + JAPModel.getInstance().getHttpListenerPortNumber());
				return super.checkUpdate();
			}
		};
		JComponent contentPane = paneBrowserConf.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = insets;

		JTextComponentToClipboardCopier textCopier = new JTextComponentToClipboardCopier(false);

		tempLabel = new JLabel("Hostname" + ":" + "Port");
		contentPane.add(tempLabel, constraints);

		constraints.gridx++;
		m_lblHostname = GUIUtils.createSelectableAndResizeableLabel(contentPane);
		m_lblHostname.setText("localhost");
		textCopier.registerTextComponent(m_lblHostname);
		m_lblHostname.setBackground(Color.white);
		contentPane.add(m_lblHostname, constraints);

		constraints.gridx++;
		tempLabel = new JLabel(":");
		contentPane.add(tempLabel, constraints);

		constraints.gridx++;
		m_lblPort = GUIUtils.createSelectableAndResizeableLabel(contentPane);
		m_lblPort.setText("" + 65535);
		textCopier.registerTextComponent(m_lblPort);
		m_lblPort.setBackground(Color.white);
		contentPane.add(m_lblPort, constraints);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_IE, "browser_ie", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_FIREFOX, "browser_firefox", true);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_OPERA, "browser_opera", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_KONQUEROR, "browser_konqueror", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_SAFARI, "browser_safari", false);
		addBrowserInstallationInfo(contentPane, constraints,
								   JAPMessages.getString(MSG_OTHER_BROWSERS), "browser_unknown", false);


		DialogContentPane paneBrowserTest = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_BROWSER_TEST), layout,
			  new DialogContentPane.Options(paneBrowserConf))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				if (m_groupWarning.getSelection() == null)
				{
					return new CheckError[]{new CheckError(
					   JAPMessages.getString(MSG_MAKE_SELECTION), LogType.GUI)};
				}

				return errors;
			}
			public boolean isSkippedAsPreviousContentPane()
			{
				return m_groupWarning.getSelection() != null &&
					(m_radioNoWarning.isSelected() || m_radioNoWarningAndSurfing.isSelected());
			}
		};
		contentPane = paneBrowserTest.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		m_radioNoWarning = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_WARNING));
		contentPane.add(m_radioNoWarning, constraints);
		m_radioNoWarningAndSurfing =
			new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_WARNING_AND_SURFING));
		constraints.gridy++;
		contentPane.add(m_radioNoWarningAndSurfing, constraints);
		m_radioErrorWarningNoSurfing = new JRadioButton(JAPMessages.getString(MSG_ERROR_WARNING_NO_SURFING));
		constraints.gridy++;
		contentPane.add(m_radioErrorWarningNoSurfing, constraints);
		m_radioSuccessWarning = new JRadioButton(JAPMessages.getString(MSG_SUCCESS_WARNING));
		m_radioSuccessWarning.setForeground(new Color(0, 160, 0));
		constraints.gridy++;
		contentPane.add(m_radioSuccessWarning, constraints);
		m_groupWarning = new ButtonGroup();
		m_groupWarning.add(m_radioNoWarning);
		m_groupWarning.add(m_radioNoWarningAndSurfing);
		m_groupWarning.add(m_radioErrorWarningNoSurfing);
		m_groupWarning.add(m_radioSuccessWarning);


		DialogContentPane paneBrowserTestNoWarning = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_WARNING), layout,
			  new DialogContentPane.Options(paneBrowserTest))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return m_groupWarning.getSelection() != null &&
					!(m_radioNoWarning.isSelected() || m_radioNoWarningAndSurfing.isSelected());
			}
			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneBrowserTestNoWarning.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);


		DialogContentPane paneExplainNoDirectConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_DIRECT_CONNECTION, new Object[]{
										  JAPMessages.getString("confButton"),
										  JAPMessages.getString("confProxyBorder")}), layout,
			  new DialogContentPane.Options(paneBrowserTestNoWarning))
		{

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupWarning.getSelection() != null && !m_radioErrorWarningNoSurfing.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoDirectConnection.setDefaultButtonOperation(
			DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);


		DialogContentPane paneDeactivateActiveContents = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_DEACTIVATE_ACTIVE), layout,
			  new DialogContentPane.Options(paneExplainNoDirectConnection));
		contentPane = paneDeactivateActiveContents.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = -1;
		constraints.anchor = GridBagConstraints.WEST;
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_IE, "browser_ie", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_FIREFOX, "browser_firefox", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_OPERA, "browser_opera", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_KONQUEROR, "browser_konqueror", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_SAFARI, "browser_safari", false);
		addBrowserInstallationInfo(contentPane, constraints,
								   JAPMessages.getString(MSG_OTHER_BROWSERS), "browser_unknown", false);

		DialogContentPane paneAnonTest = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_ANON_TEST), layout,
			  new DialogContentPane.Options(paneDeactivateActiveContents))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				if (m_groupAnon.getSelection() == null)
				{
					return new CheckError[]{new CheckError(
					   JAPMessages.getString(MSG_MAKE_SELECTION), LogType.GUI)};
				}

				return errors;
			}
		};
		contentPane = paneAnonTest.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		m_radioNoServiceAvailable = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_SERVICE_AVAILABLE));
		contentPane.add(m_radioNoServiceAvailable, constraints);
		m_radioNoConnection = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_CONNECTION));
		constraints.gridy++;
		contentPane.add(m_radioNoConnection, constraints);
		m_noSurfing = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_SURFING));
		constraints.gridy++;
		contentPane.add(m_noSurfing, constraints);
		m_radioConnectionSlow =
			new JRadioButton(JAPMessages.getString(MSG_ERROR_CONNECTION_SLOW));
		constraints.gridy++;
		contentPane.add(m_radioConnectionSlow, constraints);
		m_ConnectionOK = new JRadioButton(JAPMessages.getString(MSG_SUCCESS_CONNECTION));
		m_ConnectionOK.setForeground(new Color(0, 160, 0));
		constraints.gridy++;
		contentPane.add(m_ConnectionOK, constraints);

		m_groupAnon = new ButtonGroup();
		m_groupAnon.add(m_radioNoServiceAvailable);
		m_groupAnon.add(m_radioNoConnection);
		m_groupAnon.add(m_noSurfing);
		m_groupAnon.add(m_radioConnectionSlow);
		m_groupAnon.add(m_ConnectionOK);

		DialogContentPane paneExplainNoServiceAvailable = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_SERVICE_AVAILABLE, new Object[]{
										  JAPMessages.getString("confButton"),
										  JAPMessages.getString("confTreeForwardingClientLeaf")}),
			  layout, new DialogContentPane.Options(paneAnonTest))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null && !m_radioNoServiceAvailable.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoServiceAvailable.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);



		DialogContentPane paneExplainNoConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_CONNECTION), layout,
			  new DialogContentPane.Options(paneExplainNoServiceAvailable))
		{
			public CheckError[] checkUpdate()
			{
				m_radioNoServiceAvailable.setVisible(true);
				return super.checkUpdate();
			}

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null && !m_radioNoConnection.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoConnection.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);
		lblImage = new JLabel(GUIUtils.loadImageIcon(IMG_SERVICES));
		lblImage.setBorder(border);
		paneExplainNoConnection.getContentPane().add(lblImage);


		DialogContentPane paneExplainBadConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_BAD_CONNECTION), layout,
			  new DialogContentPane.Options(paneExplainNoConnection))
		{

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null &&
					! (m_noSurfing.isSelected() || m_radioConnectionSlow.isSelected());
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainBadConnection.setDefaultButtonOperation(
			DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);
		lblImage = new JLabel(GUIUtils.loadImageIcon(IMG_SERVICES));
		lblImage.setBorder(border);
		paneExplainBadConnection.getContentPane().add(lblImage);


		final DialogContentPane paneFinish = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_FINISHED), layout,
			  new DialogContentPane.Options(paneExplainBadConnection));
		paneFinish.getButtonCancel().setVisible(false);

		// prevent premature closing of the wizard
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent a_event)
			{
				boolean bClose = true;
				if (!paneFinish.isVisible())
				{
					bClose = (JAPDialog.showConfirmDialog(thisDialog, JAPMessages.getString(MSG_REALLY_CLOSE),
						OPTION_TYPE_OK_CANCEL, MESSAGE_TYPE_QUESTION) == RETURN_VALUE_OK);
				}
				if (bClose)
				{
					dispose();
				}
			}
		});



		DialogContentPane.updateDialogOptimalSized(paneWelcome);
		setResizable(false);
		m_radioNoServiceAvailable.setVisible(false);
	}

	private void addBrowserInstallationInfo(JComponent a_component, GridBagConstraints a_constraints,
											String a_browserName, String a_helpContext, boolean a_bRecommended)
	{
		JLabel tempLabel;

		a_constraints.gridx = 0;
		a_constraints.gridy++;
		a_constraints.gridwidth = 1;
		tempLabel = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW));
		a_component.add(tempLabel, a_constraints);

		a_constraints.gridwidth = 4;
		a_constraints.gridx = 1;
		if (a_bRecommended)
		{
			tempLabel = new JLabel(a_browserName + " (" + JAPMessages.getString(MSG_RECOMMENDED) + ")");
		}
		else
		{
			tempLabel = new JLabel(a_browserName);
		}

		registerHelpContext(tempLabel, a_helpContext);
		a_component.add(tempLabel, a_constraints);
		tempLabel = new JLabel();
		a_constraints.weightx = 1.0;
		a_constraints.fill = GridBagConstraints.HORIZONTAL;
		a_component.add(tempLabel, a_constraints);
		a_constraints.weightx = 0.0;

	}

	private void registerHelpContext(JLabel a_label, final String a_context)
	{
		a_label.setForeground(Color.blue);
		a_label.setToolTipText(JAPMessages.getString(MSG_CLICK_TO_VIEW_HELP));
		a_label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		a_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				JAPHelp.getInstance().getContextObj().setContext(a_context);
				JAPHelp.getInstance().setVisible(true);
			}
		});
	}

}

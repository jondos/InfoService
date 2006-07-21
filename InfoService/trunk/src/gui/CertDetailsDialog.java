/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package gui;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Locale;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import anon.crypto.CertPath;
import anon.crypto.CertificateInfoStructure;
import anon.crypto.IMyPublicKey;
import anon.crypto.JAPCertificate;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509Extensions;
import anon.crypto.X509UnknownExtension;
import anon.crypto.MyRSAPublicKey;
import gui.dialog.JAPDialog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.ImageIcon;

/**
 * <p>CertDetails Dialog </p>
 * <p>Description: Displays any certificates</p>
 * @author Kuno G. Gruen
 */
public class CertDetailsDialog extends JAPDialog
{
	public static final String MSG_CERTVALID = CertDetailsDialog.class.getName() + "_certValid";
	public static final String MSG_CERTNOTVALID = CertDetailsDialog.class.getName() + "_certNotValid";
	public static final String MSG_CERT_VERIFIED = CertDetailsDialog.class.getName() + "_certVerified";
	public static final String MSG_CERT_NOT_VERIFIED = CertDetailsDialog.class.getName() + "_certNotVerified";
	private static final String MSG_TITLE = CertDetailsDialog.class.getName();
	private static final String MSG_X509Attribute_ST = CertDetailsDialog.class.getName() + "_attributeST";
	private static final String MSG_X509Attribute_L = CertDetailsDialog.class.getName() + "_attributeL";
	private static final String MSG_X509Attribute_C = CertDetailsDialog.class.getName() + "_attributeC";
	private static final String MSG_X509Attribute_CN = CertDetailsDialog.class.getName() + "_attributeCN";
	private static final String MSG_X509Attribute_O = CertDetailsDialog.class.getName() + "_attributeO";
	private static final String MSG_X509Attribute_OU = CertDetailsDialog.class.getName() + "_attributeOU";
	private static final String MSG_X509Attribute_EMAIL = CertDetailsDialog.class.getName() +
		"_attributeEMAIL";
	private static final String MSG_SHOW_CERT = CertDetailsDialog.class.getName() + "_showCert";
	private static final String MSG_CERT_HIERARCHY = CertDetailsDialog.class.getName() + "_certHierarchy";
	private static final String MSG_SYMBOLS = CertDetailsDialog.class.getName() + "_symbols";
	private static final String MSG_DETAILS = CertDetailsDialog.class.getName() + "_detailsTab";

	private static final String MSG_X509Attribute_EMAILADDRESS = CertDetailsDialog.class.getName() +
		"_attributeEMAIL";
	private static final String MSG_X509Attribute_SURNAME = CertDetailsDialog.class.getName() +
		"_attributeSURNAME";
	private static final String MSG_X509Attribute_GIVENNAME = CertDetailsDialog.class.getName() +
		"_attributeGIVENNAME";
	private static final String MSG_ALERT_CERTDATE_EXPIRED = CertDetailsDialog.class.getName() +
		"_alertCertValidityExpired";
	private static final String MSG_ALERT_CERTDATE_NOTYET = CertDetailsDialog.class.getName() +
		"_alertCertNotYetValid";
	private static final String MSG_ALERT_NOT_TRUSTED = CertDetailsDialog.class.getName() +
		"_alertSignatureNotTrusted";
	private static final String UNKNOWN_EXTENSION = CertDetailsDialog.class.getName() +
		"_alertUnknownExtension";
	private static final String TITLE_DISTINGUISHEDNAME = CertDetailsDialog.class.getName() +
		"_titleDistinguishedName";
	private static final String TITLE_ISSUER = CertDetailsDialog.class.getName() +
		"_titleIssuer";
	private static final String TITLE_VALIDITY = CertDetailsDialog.class.getName() +
		"_titleValidity";
	private static final String TITLE_VALIDITY_GENERAL = CertDetailsDialog.class.getName() +
		"_titleValidityGeneral";
	private static final String TITLE_VALIDITY_TO = CertDetailsDialog.class.getName() +
		"_titleValidityTo";
	private static final String TITLE_VALIDITY_FROM = CertDetailsDialog.class.getName() +
		"_titleValidityFrom";
	private static final String TITLE_EXTENSIONS = CertDetailsDialog.class.getName() +
		"_titleExtensions";
	private static final String TITLE_IDENTIFICATION = CertDetailsDialog.class.getName() +
		"_titleIdentification";
	private static final String TITLE_IDENTIFICATION_SHA1 = CertDetailsDialog.class.getName() +
		"_titleIdentificationSHA1";
	private static final String TITLE_IDENTIFICATION_MD5 = CertDetailsDialog.class.getName() +
		"_titleIdentificationMD5";
	private static final String TITLE_IDENTIFICATION_SERIAL = CertDetailsDialog.class.getName() +
		"_titleIdentificationSerial";
	private static final String TITLE_KEYS = CertDetailsDialog.class.getName() +
		"_titleKeys";
	private static final String TITLE_KEYS_ALGORITHM = CertDetailsDialog.class.getName() +
		"_titleKeysAlgorithm";
	private static final String TITLE_KEYS_KEYLENGTH = CertDetailsDialog.class.getName() +
		"_titleKeysKeylength";
	private static final String TITLE_KEYS_SIGNALGORITHM = CertDetailsDialog.class.getName() +
		"_titleKeysSignatureAlgorithm";

	private static final String CERT_VALID_INACTIVE = "certinactive.gif";
	private static final String CERT_INVALID_INACTIVE = "certinvalidinactive.gif";
	private static final JLabel LABEL = new JLabel();

	private static final Color TITLE_COLOR = Color.blue;
	private static final Color ALERT_COLOR = Color.red;

	private static final Font TITLE_FONT = new Font(LABEL.getFont().getName(), Font.BOLD,
		(LABEL.getFont().getSize()) + 3);
	private static final Font KEY_FONT = new Font(LABEL.getFont().getName(), Font.BOLD,
												  (LABEL.getFont().getSize()));
	private static final Font VALUE_FONT = new Font(LABEL.getFont().getName(), Font.PLAIN,
		(LABEL.getFont().getSize()));
	private static final Font ALERT_FONT = new Font(LABEL.getFont().getName(), Font.BOLD,
		(LABEL.getFont().getSize()));

	public final static ImageIcon CERTENABLEDICON = GUIUtils.loadImageIcon("cenabled.gif", false);
	public final static ImageIcon CERTDISABLEDICON = GUIUtils.loadImageIcon("cdisabled.gif", false);
	public final static ImageIcon IMAGE_WARNING = GUIUtils.loadImageIcon("warning.gif", false);

	private JLabel lbl_summaryIcon;
	private Locale m_Locale;
	private String str;

	// Used for a CertDetailsDialog with a certPath
	private JList m_certList;
	private JTabbedPane m_tabbedPane;
	private DefaultListModel m_certListModel;
	private JAPCertificate m_detailedCert, m_selectedCert;

	/**
	 *
	 * @param a_parent The parent object
	 * @param a_cert JAPCertificate which will be shown
	 * @param a_bIsVerifyable boolean indicating if the cert has been verified
	 * @param a_locale the current locale
	 * @param a_certPath the certPath of the displayed certificate
	 */
	public CertDetailsDialog(Component a_parent, JAPCertificate a_cert, boolean a_bIsVerifyable,
							 Locale a_locale, CertPath a_certPath)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));
		m_Locale = a_locale;

		//init TabbedPane
		JTabbedPane tabbedPane = new JTabbedPane();

		//draw Panels
		TitledGridBagPanel detailsPanel = drawDetailsPanel(a_cert, a_bIsVerifyable);
		JPanel certPathPanel = drawCertPathPanel(a_certPath);


		//add Panels to TabbedPane
		tabbedPane.add(JAPMessages.getString(MSG_DETAILS), detailsPanel);
		tabbedPane.add(JAPMessages.getString(MSG_CERT_HIERARCHY), certPathPanel);

		JScrollPane sp = new JScrollPane(
			tabbedPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(sp);

		m_tabbedPane = tabbedPane;
		m_detailedCert = a_cert;
		setSize();
		getContentPane().setVisible(true);

	}
	/**
	 *
	 * @param a_parent The parent object
	 * @param a_cert JAPCertificate which will be shown
	 * @param a_bIsVerifyable boolean indicating if the cert has been verified
	 * @param a_locale the current locale
	 */
	public CertDetailsDialog(Component a_parent, JAPCertificate a_cert, boolean a_bIsVerifyable,
							 Locale a_locale)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));
		m_Locale = a_locale;

		TitledGridBagPanel detailsPanel = drawDetailsPanel(a_cert, a_bIsVerifyable);

		JScrollPane sp = new JScrollPane(
			detailsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		this.getContentPane().add(sp);

		setSize();
		setVisible(true);
	}

	private void setSize()
	{
		this.pack();
		if (this.getSize().height > 600)
		{
			this.setSize(getSize().width, 600);
		}
		if (this.getSize().width > 800)
		{
			this.setSize(800, getSize().height);
		}
	}
	/**
	 * Translates a Vector of numerical identifiers into human readable names
	 *
	 * @see anon.crypto.X509DistinguishedName.getAttributeNameFromAttributeIdentifier()
	 * @param a_vector Vector with numerical identifiers
	 * @return a Vector with human readable Strings
	 */
	private Vector idsToNames(Vector a_vector)
	{
		Vector res = new Vector(a_vector.size());
		String str = " ";

		if (a_vector != null && a_vector.size() > 0)
		{
			for (int i = 0; i < a_vector.size(); i++)
			{
				String abbrev = (anon.crypto.X509DistinguishedName.
								 getAttributeNameFromAttributeIdentifier( (String) a_vector.elementAt(i)));

				if (abbrev.equals(X509DistinguishedName.LABEL_STATE_OR_PROVINCE))
				{
					str = JAPMessages.getString(MSG_X509Attribute_ST);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_LOCALITY))
				{
					str = JAPMessages.getString(MSG_X509Attribute_L);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_COUNTRY))
				{
					str = JAPMessages.getString(MSG_X509Attribute_C);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_COMMON_NAME))
				{
					str = JAPMessages.getString(MSG_X509Attribute_CN);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_ORGANISATION))
				{
					str = JAPMessages.getString(MSG_X509Attribute_O);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_ORGANISATIONAL_UNIT))
				{
					str = JAPMessages.getString(MSG_X509Attribute_OU);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_EMAIL))
				{
					str = JAPMessages.getString(MSG_X509Attribute_EMAIL);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_EMAIL_ADDRESS))
				{
					str = JAPMessages.getString(MSG_X509Attribute_EMAILADDRESS);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_SURNAME))
				{
					str = JAPMessages.getString(MSG_X509Attribute_SURNAME);
				}
				else if (abbrev.equals(X509DistinguishedName.LABEL_GIVENNAME))
				{
					str = JAPMessages.getString(MSG_X509Attribute_GIVENNAME);
				}

				else
				{
					str = abbrev;
				}
				if (!str.equals(abbrev))
				{
					str += " (" + abbrev + ")";
				}
				res.addElement(str);
			}
		}
		return res;
	}

	private TitledGridBagPanel drawDetailsPanel(JAPCertificate a_cert, boolean a_bIsVerifyable)
	{
		Insets inset = new Insets(2, 5, 2, 5);
		TitledGridBagPanel detailsPanel = new TitledGridBagPanel(null, inset);

		JLabel lbl_key;
		JLabel lbl_val;

		// Image
		if (a_bIsVerifyable)
		{
			lbl_summaryIcon = new JLabel(GUIUtils.loadImageIcon(CERT_VALID_INACTIVE, true), JLabel.RIGHT);
		}
		else
		{
			lbl_summaryIcon = new JLabel(GUIUtils.loadImageIcon(CERT_INVALID_INACTIVE, true), JLabel.RIGHT);
		}
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.gridheight = 3;
		constraints.insets = (new Insets(1, 10, 1, 10));
		detailsPanel.add(lbl_summaryIcon, constraints);

		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridheight = 1;
		constraints.gridx = 1;
		constraints.insets = inset;

		// Common Name
		JLabel lbl_summaryName = new JLabel(a_cert.getSubject().getCommonName(), JLabel.LEFT);
		lbl_summaryName.setForeground(TITLE_COLOR);
		lbl_summaryName.setFont(TITLE_FONT);
		constraints.gridwidth = 2;
		detailsPanel.add(lbl_summaryName, constraints);

		constraints.gridy = 1;
		constraints.gridwidth = 1;
		detailsPanel.add(new JLabel(JAPMessages.getString(TITLE_ISSUER), JLabel.RIGHT), constraints);
		constraints.gridx = 2;

		str = a_cert.getIssuer().getOrganisation();
		if ( (str == null) || (str.equals("")))
		{
			str = a_cert.getIssuer().getCommonName();
		}
		detailsPanel.add(new JLabel(str), constraints);
		constraints.gridx = 1;
		constraints.gridy = 2;
		detailsPanel.add(new JLabel(JAPMessages.getString(TITLE_VALIDITY_TO), JLabel.RIGHT), constraints);
		constraints.gridx = 2;
		detailsPanel.add(new JLabel( (a_cert.getValidity().getValidTo()).toString()), constraints);

		detailsPanel.addDummyRows(5);

		// Display alert messages if necessary
		Date today = new Date();
		if (!a_cert.getValidity().isValid(today))
		{
			if (a_cert.getValidity().getValidFrom().getTime() < today.getTime())
			{
				String MSG = JAPMessages.getString(MSG_ALERT_CERTDATE_EXPIRED);
				JLabel lbl_validityAlert = new JLabel(MSG, JLabel.LEFT);
				lbl_validityAlert.setFont(ALERT_FONT);
				lbl_validityAlert.setForeground(ALERT_COLOR);
				detailsPanel.addRow(null, null, lbl_validityAlert, null);
			}
			else if (a_cert.getValidity().getValidTo().getTime() > today.getTime())
			{
				String MSG = JAPMessages.getString(MSG_ALERT_CERTDATE_NOTYET);
				JLabel lbl_validityAlert = new JLabel(MSG, JLabel.LEFT);
				lbl_validityAlert.setFont(ALERT_FONT);
				lbl_validityAlert.setForeground(ALERT_COLOR);
				detailsPanel.addRow(null, null, lbl_validityAlert, null);
			}
		}

		if (!a_bIsVerifyable)
		{
			String MSG = JAPMessages.getString(MSG_ALERT_NOT_TRUSTED);
			JLabel lbl_verifyAlert = new JLabel(MSG, JLabel.LEFT);
			lbl_verifyAlert.setFont(ALERT_FONT);
			lbl_verifyAlert.setForeground(ALERT_COLOR);
			detailsPanel.addRow(null, null, lbl_verifyAlert, null);
		}

		// Distinguished Name
		X509DistinguishedName dName = a_cert.getSubject();
		Vector distinguishedNameKeys = dName.getAttributeIdentifiers();
		Vector distinguishedNameValues = dName.getAttributeValues();
		replaceCountryCodeByCountryName(distinguishedNameValues, distinguishedNameKeys);
		distinguishedNameKeys = idsToNames(distinguishedNameKeys);

		JLabel title_dName = new JLabel(JAPMessages.getString(TITLE_DISTINGUISHEDNAME), JLabel.RIGHT);
		title_dName.setFont(TITLE_FONT);
		title_dName.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_dName, null, new JSeparator(JSeparator.HORIZONTAL));
		for (int i = 0; i < distinguishedNameKeys.size(); i++)
		{
			lbl_key = new JLabel(distinguishedNameKeys.elementAt(i).toString(), JLabel.RIGHT);
			lbl_key.setFont(KEY_FONT);
			lbl_val = new JLabel(distinguishedNameValues.elementAt(i).toString(), JLabel.LEFT);
			lbl_val.setFont(VALUE_FONT);
			detailsPanel.addRow(lbl_key, null, lbl_val);
		}

		// Issuer
		Vector issuerKeys = a_cert.getIssuer().getAttributeIdentifiers();
		Vector issuerValues = a_cert.getIssuer().getAttributeValues();
		replaceCountryCodeByCountryName(issuerValues, issuerKeys);
		issuerKeys = idsToNames(issuerKeys);

		JLabel title_Issuer = new JLabel(JAPMessages.getString(TITLE_ISSUER), JLabel.RIGHT);
		title_Issuer.setFont(TITLE_FONT);
		title_Issuer.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_Issuer, null, new JSeparator(JSeparator.HORIZONTAL));

		for (int i = 0; i < issuerKeys.size(); i++)
		{
			lbl_key = new JLabel(issuerKeys.elementAt(i).toString(), JLabel.RIGHT);
			lbl_key.setFont(KEY_FONT);
			lbl_val = new JLabel(issuerValues.elementAt(i).toString(), JLabel.LEFT);
			lbl_val.setFont(VALUE_FONT);
			detailsPanel.addRow(lbl_key, null, lbl_val);
		}

		// Extensions
		X509Extensions extensionsVect = a_cert.getExtensions();
		JLabel title_extensions = new JLabel(JAPMessages.getString(TITLE_EXTENSIONS), JLabel.RIGHT);
		title_extensions.setFont(TITLE_FONT);
		title_extensions.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_extensions, null, new JSeparator(JSeparator.HORIZONTAL));
		String critical = null;

		for (int i = 0; i < extensionsVect.getExtensions().size(); i++)
		{
			if (extensionsVect.getExtension(i) instanceof X509UnknownExtension)
			{
				if (extensionsVect.getExtension(i).isCritical())
				{
					critical = "*";
				}

				lbl_key = new JLabel(JAPMessages.getString(UNKNOWN_EXTENSION) + critical, JLabel.RIGHT);
				lbl_key.setFont(KEY_FONT);
				StringBuffer sb = new StringBuffer();
				for (int j = 0; j < extensionsVect.getExtension(i).getValues().size(); j++)
				{
					// no known values available
				}
				lbl_val = new JLabel(sb.toString(), JLabel.LEFT);
				lbl_key.setFont(KEY_FONT);
				lbl_val.setFont(VALUE_FONT);
				detailsPanel.addRow(lbl_key, null, lbl_val);
			}

			else
			{
				lbl_key = new JLabel(extensionsVect.getExtension(i).getName(), JLabel.RIGHT);
				lbl_key.setFont(KEY_FONT);
				Vector values = extensionsVect.getExtension(i).getValues();
				if (values.size() == 0)
				{
					detailsPanel.addRow(lbl_key, null, null);
				}
				else
				{
					lbl_val = new JLabel(values.elementAt(0).toString());
					lbl_val.setFont(VALUE_FONT);
					detailsPanel.addRow(lbl_key, null, lbl_val);
					for (int j = 1; j < values.size(); j++)
					{
						lbl_val = new JLabel(values.elementAt(j).toString());
						lbl_val.setFont(VALUE_FONT);
						detailsPanel.addRow(null, null, lbl_val);
					}
				}
			}
		}

		// Validity
		Validity validity = a_cert.getValidity();
		Vector validityKeys = new Vector();
		validityKeys.addElement(new String(JAPMessages.getString(TITLE_VALIDITY_GENERAL)));
		validityKeys.addElement(new String(JAPMessages.getString(TITLE_VALIDITY_FROM)));
		validityKeys.addElement(new String(JAPMessages.getString(TITLE_VALIDITY_TO)));
		Vector validityValues = new Vector();
		if (validity.isValid(new Date()))
		{
			validityValues.addElement(JAPMessages.getString(MSG_CERTVALID));
		}
		else
		{
			validityValues.addElement(JAPMessages.getString(MSG_CERTNOTVALID));
		}
		validityValues.addElement(validity.getValidFrom().toString());
		validityValues.addElement(validity.getValidTo().toString());

		JLabel title_validity = new JLabel(JAPMessages.getString(TITLE_VALIDITY), JLabel.RIGHT);
		title_validity.setFont(TITLE_FONT);
		title_validity.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_validity, null, new JSeparator(JSeparator.HORIZONTAL));

		for (int i = 0; i < validityKeys.size(); i++)
		{
			lbl_key = new JLabel(validityKeys.elementAt(i).toString(), JLabel.RIGHT);
			lbl_val = new JLabel(validityValues.elementAt(i).toString(), JLabel.LEFT);
			lbl_key.setFont(KEY_FONT);
			lbl_val.setFont(VALUE_FONT);
			detailsPanel.addRow(lbl_key, null, lbl_val);
		}

		// Fingerprints
		Vector fpKeys = new Vector();
		fpKeys.addElement(JAPMessages.getString(TITLE_IDENTIFICATION_SHA1));
		fpKeys.addElement(JAPMessages.getString(TITLE_IDENTIFICATION_MD5));
		fpKeys.addElement(JAPMessages.getString(TITLE_IDENTIFICATION_SERIAL));
		Vector fpValues = new Vector();
		fpValues.addElement(a_cert.getSHA1Fingerprint());
		fpValues.addElement(a_cert.getMD5Fingerprint());
		fpValues.addElement(a_cert.getSerialNumber());

		JLabel title_fingerprints = new JLabel(JAPMessages.getString(TITLE_IDENTIFICATION), JLabel.RIGHT);
		title_fingerprints.setFont(TITLE_FONT);
		title_fingerprints.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_fingerprints, null, new JSeparator(JSeparator.HORIZONTAL));
		for (int i = 0; i < fpKeys.size(); i++)
		{
			lbl_key = new JLabel(fpKeys.elementAt(i).toString(), JLabel.RIGHT);
			lbl_val = new JLabel(fpValues.elementAt(i).toString());
			lbl_key.setFont(KEY_FONT);
			lbl_val.setFont(VALUE_FONT);
			detailsPanel.addRow(lbl_key, null, lbl_val);
		}

		/// Key Algorithm and Key length
		Vector keyKeys = new Vector();
		keyKeys.addElement(JAPMessages.getString(TITLE_KEYS_ALGORITHM));


		Vector keyValues = new Vector();
		keyValues.addElement(new String(a_cert.getPublicKey().getAlgorithm()));
		if (a_cert.getPublicKey() instanceof MyRSAPublicKey)
				{
					/** @todo Calculate correct keysize for DSA keys */
					int kLength = ( (IMyPublicKey) a_cert.getPublicKey()).getKeyLength();
					keyKeys.addElement(JAPMessages.getString(TITLE_KEYS_KEYLENGTH));
					keyValues.addElement(new Integer(kLength).toString());
		}
		keyKeys.addElement(JAPMessages.getString(TITLE_KEYS_SIGNALGORITHM));
		keyValues.addElement(a_cert.getPublicKey().getSignatureAlgorithm().getXMLSignatureAlgorithmReference());

		JLabel title_keys = new JLabel(JAPMessages.getString(TITLE_KEYS), JLabel.RIGHT);
		title_keys.setFont(TITLE_FONT);
		title_keys.setForeground(TITLE_COLOR);
		detailsPanel.addRow(title_keys, null, new JSeparator(JSeparator.HORIZONTAL));
		for (int i = 0; i < keyKeys.size(); i++)
		{
			lbl_key = new JLabel(keyKeys.elementAt(i).toString(), JLabel.RIGHT);
			lbl_val = new JLabel(keyValues.elementAt(i).toString());
			lbl_key.setFont(KEY_FONT);
			lbl_val.setFont(VALUE_FONT);
			detailsPanel.addRow(lbl_key, null, lbl_val);
		}
		return detailsPanel;
	}

	/**
	 * Draws the Panel that shows the certification Path
	 * @param a_certPath The certPath of the Certificate that is shown in this
	 *                   CertDetailsDialog
	 * @return the Panel that showes the certification Path
	 * @todo make a special CellRenderer, so we do not have to use
	 *       a certificate InfoStructure here
	 */
	private JPanel drawCertPathPanel(CertPath a_certPath)
	{
		//init Panel and Layout
		JPanel certPathPanel = new JPanel();
		certPathPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.gridwidth = 1;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(10, 10, 5, 10);

		JLabel title_certPath = new JLabel(JAPMessages.getString(MSG_CERT_HIERARCHY), JLabel.RIGHT);
		title_certPath.setFont(TITLE_FONT);
		title_certPath.setForeground(TITLE_COLOR);
		certPathPanel.add(title_certPath, constraints);

		//init List, Model, CellRenderer and ListSelectionListener
		m_certListModel = new DefaultListModel();
		m_certList = new JList(m_certListModel);
		m_certList.setFont(VALUE_FONT);
		m_certList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_certList.setCellRenderer(new CertPathListCellRenderer());
		m_certList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if (m_certListModel.getSize() != 0 && m_certList.getSelectedValue() != null)
				{
					m_selectedCert = ( (CertificateInfoStructure) m_certList.getSelectedValue()).
						getCertificate();
				}
			}
		});
		m_certList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (a_event.getClickCount() == 2)
				{
					showCert();
				}
			}
		});
		//fill the list with the certificates from the certPath
		if (a_certPath != null)
		{
			Enumeration certificates = a_certPath.getCertificates();
			{  //if the certPath is already verified we just ceck the validity
				while (certificates.hasMoreElements())
				{
					CertificateInfoStructure cis = (CertificateInfoStructure)certificates.nextElement();
					m_certListModel.add(m_certListModel.getSize(), cis);
				}
			}
		}
		//add scrollbars to the List
		JScrollPane scrpaneList = new JScrollPane();
		scrpaneList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrpaneList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		scrpaneList.getViewport().add(m_certList);
		constraints.gridy++;
		constraints.gridwidth = 1;
		constraints.gridheight = 5;
		constraints.weightx = 3;
		constraints.weighty = 2;
		constraints.insets = new Insets(5, 20, 10, 20);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.NORTH;
		certPathPanel.add(scrpaneList, constraints);

		//add a Button to view the Certificate (no certpath will be shown)
		JButton view = new JButton(JAPMessages.getString(MSG_SHOW_CERT));
		view.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				showCert();
			}

		});
		constraints.gridx++;
		constraints.gridheight = 1;
		constraints.weightx = 0;
		constraints.weighty = 1;
		constraints.insets = new Insets(10, 5, 10, 20);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		certPathPanel.add(view, constraints);

	    //add Symbol description
		constraints.gridy++;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.SOUTHWEST;
		constraints.fill = GridBagConstraints.NONE;
		constraints.insets = new Insets(5, 5, 5, 5);
		JLabel lbl_symbols =
			new JLabel("<html><u><b>" + JAPMessages.getString(MSG_SYMBOLS) + "</b></u></html>");
		certPathPanel.add(lbl_symbols, constraints);

		constraints.insets = new Insets(5, 15, 5, 5);
		constraints.gridy++;
		JLabel lbl_verified = new JLabel(JAPMessages.getString(MSG_CERTVALID), CERTENABLEDICON, JLabel.LEFT);
		certPathPanel.add(lbl_verified, constraints);

		constraints.gridy++;
		JLabel lbl_invalid = new JLabel(JAPMessages.getString(MSG_CERTNOTVALID),IMAGE_WARNING, JLabel.LEFT);
		//lbl_invalid.setForeground(Color.orange);
	    certPathPanel.add(lbl_invalid, constraints);

		constraints.gridy++;
		constraints.insets = new Insets(5, 15, 20, 5);
		JLabel lbl_unverified =
			new JLabel(JAPMessages.getString(MSG_CERT_NOT_VERIFIED), CERTDISABLEDICON, JLabel.LEFT);
		lbl_unverified.setForeground(Color.red);
	    certPathPanel.add(lbl_unverified, constraints);

	    //add Seperator
	    constraints.gridx--;
		constraints.gridy++;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(10, 20, 10, 10);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTH;

		certPathPanel.add(new JSeparator(), constraints);

		return certPathPanel;
	}

	private void showCert()
	{
		if (m_selectedCert != null)
		{
			//if the cert from this dialog and the cert to show are equal jump to the frist tab
			if (m_selectedCert.equals(m_detailedCert))
			{
				m_tabbedPane.setSelectedIndex(0);
			}
			else
			{ //open a new dialog without a certPath tab
				CertDetailsDialog dialog =
					new CertDetailsDialog(getContentPane(), m_selectedCert, true, m_Locale);
				dialog.setVisible(true);
			}
		}
	}

	/**
	 * Looks for the C identifier and replaces the corresponding attribute by a country name if possible.
	 * @param a_attributes a Vector with distinguished name attributes
	 * @param a_identifiers a Vector with identifiers corresponding with the dn attributes
	 */
	private void replaceCountryCodeByCountryName(Vector a_attributes, Vector a_identifiers)
	{
		for (int i = 0; i < a_attributes.size(); i++)
		{
			if (a_identifiers.elementAt(i).equals(X509DistinguishedName.IDENTIFIER_C))
			{
				try
				{
					a_attributes.setElementAt(
						new CountryMapper(a_attributes.elementAt(i).toString(), m_Locale).toString(), i);
				}
				catch (IllegalArgumentException a_e)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Invalid / Unknown country code");
					a_attributes.setElementAt(a_attributes.elementAt(i), i);
				}
			}
		}
	}

}

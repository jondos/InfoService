/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.crypto.CertificateInfoStructure;
import anon.crypto.JAPCertificate;
import anon.crypto.X509DistinguishedName;
import anon.crypto.SignatureVerifier;
import gui.CAListCellRenderer;
import gui.CertDetailsDialog;
import gui.JAPMessages;
import gui.JAPHelp;
import gui.JAPHtmlMultiLineLabel;
import gui.CountryMapper;
import gui.dialog.JAPDialog;
import java.awt.Font;

/**
 * This is the configuration GUI for the cert.
 */

final class JAPConfCert extends AbstractJAPConfModule implements Observer
{
	private static final String MSG_DETAILS = JAPConfCert.class.getName() + "_details";

	private TitledBorder m_borderCert;
	private JLabel m_labelDate, m_labelCN, m_labelE, m_labelCSTL, m_labelO, m_labelOU;
	private JLabel m_labelDateData, m_labelCNData, m_labelEData, m_labelCSTLData, m_labelOData, m_labelOUData;
	private JButton m_bttnCertInsert, m_bttnCertRemove, m_bttnCertStatus, m_bttnCertDetails;
	private DefaultListModel m_listmodelCertList;
	private JList m_listCert;
	private JScrollPane m_scrpaneList;
	private Enumeration m_enumCerts;
	private JCheckBox m_cbCertCheckEnabled;
	private JPanel m_panelCAInfo, m_panelCAList;
	private JLabel m_lblCertTitle;

	public JAPConfCert()
	{
		super(new JAPConfCertSavePoint());
		/* observe the store of trusted certificates */
		SignatureVerifier.getInstance().getVerificationCertificateStore().addObserver(this);
		/* tricky: initialize the components by calling the observer */
		update(SignatureVerifier.getInstance().getVerificationCertificateStore(), null);
		/* set the selected index of the list to the first item to avoid exceptions */
	    m_listCert.setSelectedIndex(0);
	}

	private void updateInfoPanel(JAPCertificate a_cert)
	{
		X509DistinguishedName name;
		String strCSTL = null;
		String country;

		m_labelCNData.setText("");
		m_labelEData.setText("");
		m_labelCSTLData.setText("");
		m_labelOData.setText("");
		m_labelOUData.setText("");
		m_labelDateData.setText("");
		if (a_cert == null)
		{
			return;
		}

		StringBuffer strBuff = new StringBuffer();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		strBuff.append(sdf.format(a_cert.getValidity().getValidFrom()));
		strBuff.append(" - ");
		strBuff.append(sdf.format(a_cert.getValidity().getValidTo()));
		m_labelDateData.setText(strBuff.toString());

		name = a_cert.getSubject();
		if (name.getCommonName() != null && name.getCommonName().trim().length() > 0)
		m_labelCNData.setText(name.getCommonName().trim());
		if (name.getEmailAddress() != null && name.getEmailAddress().trim().length() > 0)
		{
			m_labelEData.setText(name.getEmailAddress().trim());
		}
		else if (name.getE_EmailAddress() != null && name.getE_EmailAddress().trim().length() > 0)
		{

			m_labelEData.setText(name.getE_EmailAddress());
		}
		if (name.getLocalityName() != null && name.getLocalityName().trim().length() > 0)
		{
			strCSTL = name.getLocalityName().trim();
		}
		if (name.getStateOrProvince() != null && name.getStateOrProvince().trim().length() > 0)
		{
			if (strCSTL != null)
			{
				strCSTL += ", ";
			}
			else
			{
				strCSTL = "";
			}
			strCSTL += name.getStateOrProvince().trim();
		}
		if (name.getCountryCode() != null)
		{
			try
			{
				country = new CountryMapper(name.getCountryCode(), JAPMessages.getLocale()).toString();
			}
			catch (IllegalArgumentException a_e)
			{
				country = name.getCountryCode();
			}

			if (country.trim().length() > 0)
			{
				if (strCSTL != null)
				{
					strCSTL += ", ";
				}
				else
				{
					strCSTL = "";
				}
				strCSTL += country.trim();
			}
		}
		m_labelCSTLData.setText(strCSTL);

		if (name.getOrganisation() != null && name.getOrganisation().trim().length() > 0)
		{
			m_labelOData.setText(name.getOrganisation().trim());
		}
		if (name.getOrganisationalUnit() != null && name.getOrganisationalUnit().trim().length() > 0)
		{
			m_labelOUData.setText(name.getOrganisationalUnit().trim());
		}
	}

	/**
	 * Creates the cert root panel with all child-panels.
	 */
	public void recreateRootPanel()
	{

		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();

		m_borderCert = new TitledBorder(JAPMessages.getString("confCertTab"));
		//m_borderCert.setTitleFont(getFontSetting());
		panelRoot.setBorder(m_borderCert);
		JPanel caLabel = createCALabel();
		m_panelCAList = createCertCAPanel();
		m_panelCAInfo = createCertInfoPanel();
		panelRoot.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(caLabel, c);
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;

		panelRoot.add(m_panelCAList, c);
		c.gridy++;
		c.insets = new Insets(20, 10, 20, 10);
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(new JSeparator(), c);

		c.gridy++;
		c.insets = new Insets(0, 0, 0, 0);
		panelRoot.add(m_panelCAInfo, c);
	}

	/**
	 * Returns the title for the cert configuration tab.
	 *
	 * @return The title for the cert configuration tab.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confCertTab");
	}

	private JPanel createCALabel()
	{
		JPanel r_panelCALabel = new JPanel();

		GridBagLayout panelLayoutCA = new GridBagLayout();
		r_panelCALabel.setLayout(panelLayoutCA);

		JLabel labelTrust1 = new JLabel(JAPMessages.getString("certTrust1"));
		JLabel labelTrust2 = new JLabel(JAPMessages.getString("certTrust2"));
		//labelTrust1.setFont(getFontSetting());
		//labelTrust2.setFont(getFontSetting());

		m_cbCertCheckEnabled = new JCheckBox();
		m_cbCertCheckEnabled.setSelected(true);
		m_cbCertCheckEnabled.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbCertCheckEnabled.isSelected();
				m_labelDate.setEnabled(b);
				m_labelCN.setEnabled(b);
				m_labelE.setEnabled(b);
				m_labelCSTL.setEnabled(b);
				m_labelO.setEnabled(b);
				m_labelOU.setEnabled(b);
				m_labelDateData.setEnabled(b);
				m_labelCNData.setEnabled(b);
				m_labelEData.setEnabled(b);
				m_labelCSTLData.setEnabled(b);
				m_labelOData.setEnabled(b);
				m_labelOUData.setEnabled(b);
				m_bttnCertInsert.setEnabled(b);
				Object value = m_listCert.getSelectedValue();
				boolean enableRemove = false;
				if (value != null)
				{
					enableRemove =
						b && !((CertificateInfoStructure) m_listCert.getSelectedValue()).isNotRemovable();
				}
				m_bttnCertRemove.setEnabled(enableRemove);
				m_bttnCertStatus.setEnabled(b);
				m_bttnCertDetails.setEnabled(b);
				m_listCert.setEnabled(b);
				m_panelCAInfo.setEnabled(b);
				m_panelCAList.setEnabled(b);
			}
		});

		GridBagConstraints panelConstraintsCA = new GridBagConstraints();
		panelConstraintsCA.anchor = GridBagConstraints.WEST;
		panelConstraintsCA.fill = GridBagConstraints.NONE;
		panelConstraintsCA.weightx = 0;
		panelConstraintsCA.insets = new Insets(10, 10, 0, 0);

		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 0;

		r_panelCALabel.add(m_cbCertCheckEnabled, panelConstraintsCA);
		panelConstraintsCA.gridx = 1;
		panelConstraintsCA.gridy = 0;
		panelConstraintsCA.weightx = 1.0;
		panelConstraintsCA.insets = new Insets(10, 0, 0, 0);
		r_panelCALabel.add(labelTrust1, panelConstraintsCA);

		panelConstraintsCA.gridx = 1;
		panelConstraintsCA.gridy = 1;
		panelConstraintsCA.insets = new Insets(0, 0, 10, 0);
		panelLayoutCA.setConstraints(labelTrust2, panelConstraintsCA);
		r_panelCALabel.add(labelTrust2);

		return r_panelCALabel;
	}

	private JPanel createCertCAPanel()
	{
		JPanel r_panelCA = new JPanel();

		GridBagLayout panelLayoutCA = new GridBagLayout();
		r_panelCA.setLayout(panelLayoutCA);

		GridBagConstraints panelConstraintsCA = new GridBagConstraints();

		m_listmodelCertList = new DefaultListModel();

		m_listCert = new JList(m_listmodelCertList);
		m_listCert.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    m_listCert.setCellRenderer(new CAListCellRenderer());
		m_listCert.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{

				if (m_listmodelCertList.getSize() == 0 || m_listCert.getSelectedValue() == null)
				{
					updateInfoPanel(null);
					m_bttnCertRemove.setEnabled(false);
					m_bttnCertStatus.setEnabled(false);
				}
				else
				{
					CertificateInfoStructure j = (CertificateInfoStructure) m_listCert.getSelectedValue();
					updateInfoPanel(j.getCertificate());

					if (j.isEnabled())
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
					}
					else
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
					}
					m_bttnCertStatus.setEnabled(true);
					/* if the cert is not removable, the Remove Button is not enabled */
					m_bttnCertRemove.setEnabled(!j.isNotRemovable());
				} // else

			} // valuechanged
		});

		m_scrpaneList = new JScrollPane();
		m_scrpaneList.getViewport().add(m_listCert, null);

		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 0;
		panelConstraintsCA.anchor = GridBagConstraints.NORTHWEST;
		panelConstraintsCA.weightx = 1.0;
		panelConstraintsCA.weighty = 1.0;
		panelConstraintsCA.gridwidth = 4;
		panelConstraintsCA.insets = new Insets(0, 10, 10, 10);
		panelConstraintsCA.fill = GridBagConstraints.BOTH;
		panelLayoutCA.setConstraints(m_scrpaneList, panelConstraintsCA);
		r_panelCA.add(m_scrpaneList);

		m_bttnCertInsert = new JButton(JAPMessages.getString("certBttnInsert"));
		//m_bttnCertInsert.setFont(getFontSetting());
		m_bttnCertInsert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//######
				boolean decode_error = false;
				JAPCertificate cert = null;
				try
				{
					cert = JAPUtil.openCertificate(new JFrame());
				}
				catch (Exception je)
				{
					//cert = null;
					decode_error = true;
				}
				//if (cert == null)
				if (cert == null && decode_error)
				{
					JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("certInputErrorTitle"));
				}
				if (cert != null)
				{

					SignatureVerifier.getInstance().getVerificationCertificateStore().
						addCertificateWithoutVerification(cert, JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX, true, false);
				}
			}
		});

		m_bttnCertRemove = new JButton(JAPMessages.getString("certBttnRemove"));
		//m_bttnCertRemove.setFont(getFontSetting());
		m_bttnCertRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_listmodelCertList.getSize() > 0)
				{
					CertificateInfoStructure certActual = (CertificateInfoStructure) m_listCert.
						getSelectedValue();
					if (certActual != null)
					{
						SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificate(
							certActual);
					}
				}
				if (m_listmodelCertList.getSize() == 0)
				{
					m_bttnCertRemove.setEnabled(false);
					m_bttnCertStatus.setEnabled(false);
					updateInfoPanel(null);
				}
				else
				{
					updateInfoPanel(null);
					m_listCert.setSelectedIndex(0);

					CertificateInfoStructure j = (CertificateInfoStructure) m_listCert.getSelectedValue();
					updateInfoPanel(j.getCertificate());
				}
			}
		});

		m_bttnCertStatus = new JButton(JAPMessages.getString("certBttnEnable"));
		//m_bttnCertStatus.setFont(getFontSetting());
		m_bttnCertStatus.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				CertificateInfoStructure certActual = (CertificateInfoStructure) m_listCert.getSelectedValue();
				boolean enabled = certActual.isEnabled();

				if (enabled)
				{
					SignatureVerifier.getInstance().getVerificationCertificateStore().setEnabled(certActual, false);
					m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
				}
				else
				{
					SignatureVerifier.getInstance().getVerificationCertificateStore().setEnabled(certActual, true);
					m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
				}
			}
		});

	    m_bttnCertDetails = new JButton(JAPMessages.getString(MSG_DETAILS));
		//m_bttnCertDetails.setFont(getFontSetting());
		m_bttnCertDetails.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				CertDetailsDialog dialog = new CertDetailsDialog(getRootPanel().getParent(),
					((CertificateInfoStructure)m_listCert.getSelectedValue()).getCertificate().getX509Certificate(),
					true, JAPController.getInstance().getLocale());
					dialog.pack();
					dialog.setVisible(true);
			}
		});

		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 1;
		panelConstraintsCA.weightx = 0.0;
		panelConstraintsCA.gridwidth = 1;
		panelConstraintsCA.weighty = 0.0;
		panelConstraintsCA.fill = GridBagConstraints.NONE;
		panelConstraintsCA.insets = new Insets(0, 10, 0, 0);
		panelLayoutCA.setConstraints(m_bttnCertInsert, panelConstraintsCA);
		r_panelCA.add(m_bttnCertInsert);

		panelConstraintsCA.gridx = 1;
		panelConstraintsCA.gridy = 1;
		panelLayoutCA.setConstraints(m_bttnCertRemove, panelConstraintsCA);
		r_panelCA.add(m_bttnCertRemove);

		panelConstraintsCA.gridx = 2;
		panelConstraintsCA.gridy = 1;
		panelLayoutCA.setConstraints(m_bttnCertStatus, panelConstraintsCA);
		r_panelCA.add(m_bttnCertStatus);

	    panelConstraintsCA.gridx = 3;
		panelConstraintsCA.gridy = 1;
		panelLayoutCA.setConstraints(m_bttnCertDetails, panelConstraintsCA);
		r_panelCA.add(m_bttnCertDetails);

		return r_panelCA;
	}

	private JPanel createCertInfoPanel()
	{
		JPanel r_panelInfo = new JPanel();
		GridBagLayout panelLayoutInfo = new GridBagLayout();
		r_panelInfo.setLayout(panelLayoutInfo);

		GridBagConstraints panelConstraintsInfo = new GridBagConstraints();
		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.weightx = 1.0;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 0);
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 0;
		panelConstraintsInfo.gridwidth = 2;

		m_lblCertTitle = new JLabel(JAPMessages.getString("certInfoBorder"));
		r_panelInfo.add(m_lblCertTitle, panelConstraintsInfo);

		m_labelDate = new JLabel(JAPMessages.getString("certDate"));
		//m_labelDate.setFont(getFontSetting());

		m_labelCN = new JLabel(JAPMessages.getString("certName"));
		//m_labelCN.setFont(getFontSetting());
		m_labelE = new JLabel(JAPMessages.getString("certMail"));
		//m_labelE.setFont(getFontSetting());
		m_labelCSTL = new JLabel(JAPMessages.getString("certLocation"));
		//m_labelCSTL.setFont(getFontSetting());
		m_labelO = new JLabel(JAPMessages.getString("certOrg"));
		//m_labelO.setFont(getFontSetting());
		m_labelOU = new JLabel(JAPMessages.getString("certOrgUnit"));
		//m_labelOU.setFont(getFontSetting());

		m_labelDateData = new JLabel();
		//m_labelDateData.setFont(getFontSetting());

		m_labelCNData = new JLabel();
		//m_labelCNData.setFont(getFontSetting());

		m_labelEData = new JLabel();
		//m_labelEData.setFont(getFontSetting());

		m_labelCSTLData = new JLabel();
		//m_labelCSTLData.setFont(getFontSetting());

		m_labelOData = new JLabel();
		//m_labelOData.setFont(getFontSetting());

		m_labelOUData = new JLabel();
		//m_labelOUData.setFont(getFontSetting());

		/*		    	gridx
		 0:				1:
		 gridy	0:
		   1:  labelCN			labelCNData
		   2:	labelO			labelOData
		   3:	labelOU			labelOUData
		   4:	labelCSTL		labelCSTLData
		   5:	labelE			labelEData
		   ---------------------------------------
		   6:  labelDate		labelDateData
		 */

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.fill = GridBagConstraints.HORIZONTAL;
		panelConstraintsInfo.gridwidth = 1;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 1;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 0, 0);
		panelLayoutInfo.setConstraints(m_labelCN, panelConstraintsInfo);
		r_panelInfo.add(m_labelCN);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 1;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelCNData, panelConstraintsInfo);
		r_panelInfo.add(m_labelCNData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 2;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 0, 0);
		panelLayoutInfo.setConstraints(m_labelO, panelConstraintsInfo);
		r_panelInfo.add(m_labelO);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 2;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelOData, panelConstraintsInfo);
		r_panelInfo.add(m_labelOData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 3;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 0, 0);
		panelLayoutInfo.setConstraints(m_labelOU, panelConstraintsInfo);
		r_panelInfo.add(m_labelOU);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 3;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelOUData, panelConstraintsInfo);
		r_panelInfo.add(m_labelOUData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 4;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 0, 0);
		panelLayoutInfo.setConstraints(m_labelCSTL, panelConstraintsInfo);
		r_panelInfo.add(m_labelCSTL);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 4;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelCSTLData, panelConstraintsInfo);
		r_panelInfo.add(m_labelCSTLData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 5;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 0, 0);
		panelLayoutInfo.setConstraints(m_labelE, panelConstraintsInfo);
		r_panelInfo.add(m_labelE);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 5;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelEData, panelConstraintsInfo);
		r_panelInfo.add(m_labelEData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 6;
		panelConstraintsInfo.fill = GridBagConstraints.HORIZONTAL;
		panelConstraintsInfo.weightx = 0;
		panelConstraintsInfo.insets = new Insets(10, 15, 10, 0);
		panelLayoutInfo.setConstraints(m_labelDate, panelConstraintsInfo);
		r_panelInfo.add(m_labelDate);

		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 6;
		panelConstraintsInfo.weightx = 1;
		panelConstraintsInfo.insets = new Insets(10, 10, 10, 10);
		panelLayoutInfo.setConstraints(m_labelDateData, panelConstraintsInfo);
		r_panelInfo.add(m_labelDateData);
		panelConstraintsInfo.anchor = GridBagConstraints.WEST;

		return r_panelInfo;

	}

	public void update(Observable a_notifier, Object a_message)
	{
		/**
		 * list init, add certificates by issuer name
		 * It is important to place this here as otherwise a deadlock with
		 * CertificateStore.removeCertificate is possible (this class is an observer...).
		 * Therefore the lock on CertificateStore and on this class should not be mixed!
		 */
		Enumeration enumCerts = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAllCertificates().elements();

		synchronized (this)
		{
			if (a_notifier == SignatureVerifier.getInstance().getVerificationCertificateStore())
			{
				/* the message is from the SignatureVerifier trusted certificates store */
				int lastIndex = m_listCert.getSelectedIndex();
				m_listmodelCertList.clear();
				m_enumCerts = enumCerts;
				while (m_enumCerts.hasMoreElements())
				{
					CertificateInfoStructure j = (CertificateInfoStructure) m_enumCerts.nextElement();
					/* we handle only root certificates */
					if (j.getCertificateType() == JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX)
					{
						m_listmodelCertList.addElement(j);
					}
				}
				/* select the item again that was selected */
				//lastIndex = Math.min(m_listCert.getComponentCount() - 1, lastIndex);
				//if (lastIndex >= 0)
				if (m_listmodelCertList.getSize() > 0 && lastIndex >= 0)
				{
					m_listCert.setSelectedIndex(lastIndex);
				}
			}
		}
	}

	protected void onUpdateValues()
	{
		JLabel label = new JLabel();
		m_lblCertTitle.setFont(new Font(label.getFont().getName(), Font.BOLD, (int)(label.getFont().getSize() * 1.2)));
		m_cbCertCheckEnabled.setSelected(SignatureVerifier.getInstance().isCheckSignatures());
	}

	protected boolean onOkPressed()
	{
		//Cert seetings
		SignatureVerifier.getInstance().setCheckSignatures(m_cbCertCheckEnabled.isSelected());
		return true;
	}

	protected void onResetToDefaultsPressed()
	{
		super.onResetToDefaultsPressed();
		m_cbCertCheckEnabled.setSelected(JAPConstants.DEFAULT_CERT_CHECK_ENABLED);
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("cert");
	}

}

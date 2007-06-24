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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import anon.crypto.SignatureVerifier;
import gui.CAListCellRenderer;
import gui.CertDetailsDialog;
import gui.JAPMessages;
import gui.dialog.JAPDialog;

/**
 * This is the configuration GUI for the cert.
 */

final class JAPConfCert extends AbstractJAPConfModule implements Observer
{
	public static final String MSG_NO_CHECK_WARNING = JAPConfCert.class.getName() + "_noCheckWarning";

	private static final String MSG_DETAILS = JAPConfCert.class.getName() + "_details";



	private TitledBorder m_borderCert;
	private CertDetailsDialog.CertShortInfoPanel m_shortInfoPanel;
	private JButton m_bttnCertInsert, m_bttnCertRemove, m_bttnCertStatus, m_bttnCertDetails;
	private DefaultListModel m_listmodelCertList;
	private JList m_listCert;
	private JScrollPane m_scrpaneList;
	private Enumeration m_enumCerts;
	private JCheckBox m_cbCertCheckEnabled;
	private JPanel  m_panelCAList;

	public JAPConfCert()
	{
		super(new JAPConfCertSavePoint());
		/* observe the store of trusted certificates */
		SignatureVerifier.getInstance().getVerificationCertificateStore().addObserver(this);
		/* tricky: initialize the components by calling the observer */
		update(SignatureVerifier.getInstance().getVerificationCertificateStore(), null);
		/* set the selected index of the list to the first item to avoid exceptions */
	    //m_listCert.setSelectedIndex(0);
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
		panelRoot.setBorder(m_borderCert);
		JPanel caLabel = createCALabel();
		m_panelCAList = createCertCAPanel();
		m_shortInfoPanel = new CertDetailsDialog.CertShortInfoPanel();
		panelRoot.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(caLabel, c);
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;

		panelRoot.add(m_panelCAList, c);
		c.gridy++;
		//c.insets = new Insets(20, 10, 20, 10);
		c.insets = new Insets(10, 10, 10, 10);
		c.weighty = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(new JSeparator(), c);

		c.gridy++;
		c.insets = new Insets(0, 0, 0, 0);
		panelRoot.add(m_shortInfoPanel, c);

		//c.weighty = 1;
		//c.weightx = 1;
		/*
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		panelRoot.add(new JLabel(), c);*/
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

		JLabel labelTrust1 = new JLabel(JAPMessages.getString("certTrust") + ":");

		m_cbCertCheckEnabled = new JCheckBox();
		m_cbCertCheckEnabled.setSelected(true);
		m_cbCertCheckEnabled.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				if (!m_cbCertCheckEnabled.isSelected())
				{
					JAPDialog.showWarningDialog(
									   m_cbCertCheckEnabled, JAPMessages.getString(MSG_NO_CHECK_WARNING));
				}
			}
		});
		m_cbCertCheckEnabled.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbCertCheckEnabled.isSelected();

				m_shortInfoPanel.setEnabled(b);
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
		panelConstraintsCA.insets = new Insets(10, 0, 10, 0);
		r_panelCALabel.add(labelTrust1, panelConstraintsCA);

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
					m_shortInfoPanel.update((JAPCertificate)null);
					m_bttnCertRemove.setEnabled(false);
					m_bttnCertStatus.setEnabled(false);
				}
				else
				{
					CertificateInfoStructure j = (CertificateInfoStructure) m_listCert.getSelectedValue();
					m_shortInfoPanel.update(j.getCertificate());

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
				}
			}
		});

		m_listCert.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (a_event.getClickCount() == 2)
				{
					m_bttnCertDetails.doClick();
				}
			}
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
		m_bttnCertInsert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
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
					m_shortInfoPanel.update((JAPCertificate)null);
				}
				else
				{
					m_shortInfoPanel.update((JAPCertificate)null);
					m_listCert.setSelectedIndex(0);

					CertificateInfoStructure j = (CertificateInfoStructure) m_listCert.getSelectedValue();
					m_shortInfoPanel.update(j.getCertificate());
				}
			}
		});

		m_bttnCertStatus = new JButton(JAPMessages.getString("certBttnEnable"));
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
			if (a_notifier == SignatureVerifier.getInstance().getVerificationCertificateStore() &&
				(a_message == null || (a_message instanceof Integer &&
									   ((Integer)a_message).intValue() == JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX)))
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
				/* select the item again that was selected before */
				if (m_listmodelCertList.getSize() > 0 && lastIndex >= 0 &&
					lastIndex < m_listmodelCertList.getSize())
				{
					m_listCert.setSelectedIndex(lastIndex);
				}
			}
		}
	}

	public void fontSizeChanged(final JAPModel.FontResize a_resize, final JLabel a_dummyLabel)
	{
		/*
		m_lblCertTitle.setFont(new Font(a_dummyLabel.getFont().getName(), Font.BOLD,
										(int)(a_dummyLabel.getFont().getSize() * 1.2)));
								 */
	}

	protected void onUpdateValues()
	{
		if (m_cbCertCheckEnabled.isSelected() != SignatureVerifier.getInstance().isCheckSignatures())
		{
			m_cbCertCheckEnabled.setSelected(SignatureVerifier.getInstance().isCheckSignatures());
		}
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

	public String getHelpContext()
	{
		return "cert";
	}
}

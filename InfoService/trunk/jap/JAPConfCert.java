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
import java.util.Hashtable;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.bouncycastle.asn1.x509.X509NameTokenizer;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateStore;
import anon.crypto.JAPCertificateStoreId;
import gui.CAListCellRenderer;

/**
 * This is the configuration GUI for the cert.
 */

public class JAPConfCert extends AbstractJAPConfModule
{

	private DefaultListModel m_dlmCertList;
	private TitledBorder m_borderCertInfo;
	private JLabel m_labelTrust1, m_labelTrust2, m_labelDate, m_labelCN, m_labelE, m_labelCSTL, m_labelO,
		m_labelOU;
	private JLabel m_labelDateData, m_labelCNData, m_labelEData, m_labelCSTLData, m_labelOData, m_labelOUData;
	private JButton m_bttnCertInsert, m_bttnCertRemove, m_bttnCertStatus;
	private DefaultListModel m_listmodelCertList;
	private JList m_listCert;
	private JScrollPane m_scrpaneList;
	private Enumeration m_enumCerts;
	private ListSelectionListener m_listsel;

	private Hashtable m_HTCertsToInsert, m_HTCertsToRemove, m_HTCertsToStatusChange;

	private JAPCertificateStore m_jcs;

	public JAPConfCert()
	{
	}

	private void updateInfoPanel(JAPCertificate a_cert)
	{
		StringBuffer strBuff = new StringBuffer();
		strBuff.append(a_cert.getStartDate().toGMTString());
		strBuff.append(" - ");
		strBuff.append(a_cert.getEndDate().toGMTString());
		m_labelDateData.setText(strBuff.toString());
		m_labelCNData.setText("");
		m_labelEData.setText("");
		m_labelCSTLData.setText("");
		m_labelOData.setText("");
		m_labelOUData.setText("");

		X509NameTokenizer x509TokenIssuer = new X509NameTokenizer(a_cert.getIssuer().toString());
		while (x509TokenIssuer.hasMoreTokens())
		{
			String strElement = (String) x509TokenIssuer.nextToken();

			if (strElement.startsWith("CN="))
			{
				m_labelCNData.setText(strElement.substring(3));

			}
			else if (strElement.startsWith("E="))
			{
				m_labelEData.setText(strElement.substring(2));

			}
			else if (strElement.startsWith("C="))
			{
				strBuff.setLength(0);
				strBuff.append(strElement.substring(2));
				strBuff.append(m_labelCSTLData.getText());
				m_labelCSTLData.setText(strBuff.toString());
			}

			else if (strElement.startsWith("ST="))
			{
				strBuff.setLength(0);
				strBuff.append(strElement.substring(3));
				strBuff.append(" / ");
				strBuff.append(m_labelCSTLData.getText());
				m_labelCSTLData.setText(strBuff.toString());
			}

			else if (strElement.startsWith("L="))
			{
				strBuff.setLength(0);
				strBuff.append(strElement.substring(2));
				strBuff.append(" / ");
				strBuff.append(m_labelCSTLData.getText());
				m_labelCSTLData.setText(strBuff.toString());
			}

			else if (strElement.startsWith("O="))
			{
				m_labelOData.setText(strElement.substring(2));

			}
			else if (strElement.startsWith("OU="))
			{
				m_labelOUData.setText(strElement.substring(3));
			}
		}

		if (m_labelCSTLData.getText().trim().endsWith("/"))
		{
			String strLabel = m_labelCSTLData.getText().trim();
			int length = strLabel.length();
			strLabel = strLabel.substring(0, length - 1);
			m_labelCSTLData.setText(strLabel);
		}
	}

	/**
	 * Creates the cert root panel with all child-panels.
	 */
	public void repaintRootPanel()
	{
		m_jcs = JAPModel.getCertificateStore();
		m_HTCertsToInsert = new Hashtable();
		m_HTCertsToRemove = new Hashtable();
		m_HTCertsToStatusChange = new Hashtable();

		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();

		JPanel caPanel = createCertCAPanel();
		JPanel infoPanel = createCertInfoPanel();

		panelRoot.setLayout(new BorderLayout());
		panelRoot.add(caPanel, BorderLayout.CENTER);
		panelRoot.add(infoPanel, BorderLayout.SOUTH);

		/* insert all components in the root panel */
		// updateGuiOutput();
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

	/**
	 * This method is called automatically by AbstractJAPConfModule if the cert tab comes to
	 * foreground. This method calls updateGuiOutput().
	 */
	public void onRootPanelShown()
	{
		updateGuiOutput();
	}

	public void onResetToDefaultsPressed()
	{
		JAPCertificateStore jcsOrg = JAPCertificateStore.getInstance();
		JAPCertificate cert = null;
		try
		{
			byte[] tmp=JAPUtil.loadRessource(JAPConstants.CERTSPATH +
											  JAPConstants.TRUSTEDROOTCERT);
			cert = JAPCertificate.getInstance(tmp);
		}
		catch (Exception e)
		{
			cert = null;
		}
		jcsOrg.addCertificate(cert, true);
		JAPController.setCertificateStore(jcsOrg);
		m_jcs = JAPModel.getCertificateStore();
		updateGuiOutput();
	}

	public void onCancelPressed()
	{

		/* certificates that were inserted -> REMOVE
		 *
		 */
		Enumeration enumCertsToInsert = m_HTCertsToInsert.elements();
		while (enumCertsToInsert.hasMoreElements())
		{
			JAPCertificate cert = (JAPCertificate) enumCertsToInsert.nextElement();
			m_jcs.removeCertificate(cert);
		}
		m_HTCertsToInsert.clear();

		/* certificates that were removed -> RE-INSERT
		 *
		 */
		Enumeration enumCertsToRemove = m_HTCertsToRemove.elements();
		while (enumCertsToRemove.hasMoreElements())
		{
			JAPCertificate cert = (JAPCertificate) enumCertsToRemove.nextElement();
			m_jcs.addCertificate(cert, true);
		}
		m_HTCertsToRemove.clear();

		/* certificates that were enabled/disabled -> reset status
		 *
		 */
		Enumeration enumCertsToStatusChange = m_HTCertsToStatusChange.elements();
		while (enumCertsToStatusChange.hasMoreElements())
		{
			JAPCertificate cert = (JAPCertificate) enumCertsToStatusChange.nextElement();
			m_jcs.enableCertificate(cert, !cert.getEnabled());
		}
		m_HTCertsToStatusChange.clear();

		JAPController.setCertificateStore(m_jcs);
		updateGuiOutput();
	}

	public void onOkPressed()
	{
		JAPController.setCertificateStore(m_jcs);
		updateGuiOutput();
	}

	private JPanel createCertCAPanel()
	{
		final JPanel r_panelCA = new JPanel();

		GridBagLayout panelLayoutCA = new GridBagLayout();
		r_panelCA.setLayout(panelLayoutCA);

		m_labelTrust1 = new JLabel(JAPMessages.getString("certTrust1"));
		m_labelTrust1.setFont(getFontSetting());
		m_labelTrust2 = new JLabel(JAPMessages.getString("certTrust2"));
		m_labelTrust2.setFont(getFontSetting());

		GridBagConstraints panelConstraintsCA = new GridBagConstraints();
		panelConstraintsCA.anchor = GridBagConstraints.NORTHWEST;
		panelConstraintsCA.fill = GridBagConstraints.HORIZONTAL;
		panelConstraintsCA.weightx = 1.0;
		panelConstraintsCA.insets = new Insets(10, 10, 0, 0);

		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 0;
		panelLayoutCA.setConstraints(m_labelTrust1, panelConstraintsCA);
		r_panelCA.add(m_labelTrust1);

		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 1;
		panelConstraintsCA.insets = new Insets(0, 10, 10, 0);
		panelLayoutCA.setConstraints(m_labelTrust2, panelConstraintsCA);
		r_panelCA.add(m_labelTrust2);

		m_listmodelCertList = new DefaultListModel();

		m_enumCerts = m_jcs.elements();
		while (m_enumCerts.hasMoreElements())
		{
			JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
			m_listmodelCertList.addElement(j);
		}

		m_listCert = new JList(m_listmodelCertList);
		m_listCert.setCellRenderer(new CAListCellRenderer());
		boolean bStatusEnabled = false;
		if (m_listmodelCertList.size() > 0)
		{
			m_listCert.setSelectedIndex(0);
			JAPCertificate actualCert = (JAPCertificate) m_listCert.getSelectedValue();
			bStatusEnabled = actualCert.getEnabled();
		}
		m_listCert.addListSelectionListener(new ListSelectionListener()
		{

			public void valueChanged(ListSelectionEvent e)
			{

				if (m_listmodelCertList.getSize() == 0)
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");
				}
				else
				{
					JAPCertificate j = (JAPCertificate) m_listCert.getSelectedValue();
					updateInfoPanel(j);

					if (j.getEnabled())
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
					}
					else
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));

					}
				} // else

			} // valuechanged
		});

		m_scrpaneList = new JScrollPane();
		m_scrpaneList.getViewport().add(m_listCert, null);
		panelConstraintsCA.gridx = 0;
		panelConstraintsCA.gridy = 2;
		panelConstraintsCA.gridheight = 5;
		panelConstraintsCA.weighty = 1.0;
		panelConstraintsCA.insets = new Insets(0, 10, 20, 0);
		panelConstraintsCA.fill = GridBagConstraints.BOTH;
		panelLayoutCA.setConstraints(m_scrpaneList, panelConstraintsCA);
		r_panelCA.add(m_scrpaneList);

		m_bttnCertInsert = new JButton(JAPMessages.getString("certBttnInsert"));
		m_bttnCertInsert.setFont(getFontSetting());
		m_bttnCertInsert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JAPCertificate cert = null;
				try
				{
					cert = JAPUtil.openCertificate(new JFrame());
				}
				catch (Exception je)
				{
					cert=null;
				}
				if(cert==null)
				{
					JOptionPane.showMessageDialog(r_panelCA,
												  JAPMessages.getString("certInputError"),
												  JAPMessages.getString("certInputErrorTitle"),
												  JOptionPane.ERROR_MESSAGE);
				}
				if (cert != null)
				{

					if (!m_jcs.checkCertificateExists(cert))
					{
						m_listmodelCertList.addElement(cert);
						// m_listCert.removeAll();
						// m_listCert.setModel(m_listmodelCertList);
						// m_scrpaneList.getViewport().removeAll();
						// m_scrpaneList.getViewport().add(m_listCert, null);
						m_jcs.addCertificate(cert, true);
						m_HTCertsToInsert.put(JAPCertificateStoreId.getId(cert), cert);

						if (cert.getEnabled())
						{
							m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
						}
						else
						{
							m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
						}
					}
					else
					{
						//
					}
				}
				m_bttnCertRemove.setEnabled(true);
				m_bttnCertStatus.setEnabled(true);
				if (m_listmodelCertList.getSize() == 1)
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");

					m_listCert.setSelectedIndex(0);

					JAPCertificate j = (JAPCertificate) m_listCert.getSelectedValue();
					updateInfoPanel(j);

					if (j.getEnabled())
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
					}
					else
					{
						m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
					}
				}

			}
		});

		m_bttnCertRemove = new JButton(JAPMessages.getString("certBttnRemove"));
		m_bttnCertRemove.setFont(getFontSetting());
		m_bttnCertRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_listmodelCertList.getSize() > 0)
				{
					int index = m_listCert.getSelectedIndex();

					JAPCertificate certActual = (JAPCertificate) m_listCert.getSelectedValue();
					String strActualIssuerCN = (String) certActual.getIssuer().getValues().elementAt(0);
					m_enumCerts = m_jcs.elements();
					while (m_enumCerts.hasMoreElements())
					{
						JAPCertificate cert = (JAPCertificate) m_enumCerts.nextElement();
						String strIssuerCN = (String) cert.getIssuer().getValues().elementAt(0);
						if (strIssuerCN.equals(strActualIssuerCN))
						{
							m_jcs.removeCertificate(cert);
							m_HTCertsToRemove.put(JAPCertificateStoreId.getId(cert), cert);
							m_listmodelCertList.remove(index);

							if (cert.getEnabled())
							{
								m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
							}
							else
							{
								m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
							}
						}
					}
				}
				if (m_listmodelCertList.getSize() == 0)
				{
					m_bttnCertRemove.setEnabled(false);
					m_bttnCertStatus.setEnabled(false);
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");
				}
				else
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");

					m_listCert.setSelectedIndex(0);

					JAPCertificate j = (JAPCertificate) m_listCert.getSelectedValue();
					updateInfoPanel(j);
				}
			}
		});

		if (bStatusEnabled)
		{
			m_bttnCertStatus = new JButton(JAPMessages.getString("certBttnEnable"));
		}
		else
		{
			m_bttnCertStatus = new JButton(JAPMessages.getString("certBttnDisable"));

		}
		m_bttnCertStatus.setFont(getFontSetting());
		m_bttnCertStatus.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JAPCertificate certActual = (JAPCertificate) m_listCert.getSelectedValue();
				boolean enabled = certActual.getEnabled();

				if (enabled)
				{
					m_jcs.enableCertificate(certActual, false);
					m_HTCertsToStatusChange.put(JAPCertificateStoreId.getId(certActual), certActual);
					m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
				}
				else
				{
					m_jcs.enableCertificate(certActual, true);
					m_HTCertsToStatusChange.put(JAPCertificateStoreId.getId(certActual), certActual);
					m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
				}
				m_listCert.repaint();
			}
		});

		panelConstraintsCA.gridx = 2;
		panelConstraintsCA.gridy = 2;
		panelConstraintsCA.weightx = 0.0;
		panelConstraintsCA.gridheight = 1;
		panelConstraintsCA.weighty = 0.0;
		panelConstraintsCA.ipadx = 10;
		panelConstraintsCA.fill = GridBagConstraints.BOTH;
		panelConstraintsCA.insets = new Insets(0, 10, 0, 0);
		panelLayoutCA.setConstraints(m_bttnCertInsert, panelConstraintsCA);
		r_panelCA.add(m_bttnCertInsert);

		panelConstraintsCA.ipadx = 0;
		panelConstraintsCA.gridx = 2;
		panelConstraintsCA.gridy = 3;
		panelConstraintsCA.fill = GridBagConstraints.BOTH;
		panelLayoutCA.setConstraints(m_bttnCertRemove, panelConstraintsCA);
		r_panelCA.add(m_bttnCertRemove);

		panelConstraintsCA.gridx = 2;
		panelConstraintsCA.gridy = 4;
		panelConstraintsCA.fill = GridBagConstraints.BOTH;
		panelLayoutCA.setConstraints(m_bttnCertStatus, panelConstraintsCA);
		r_panelCA.add(m_bttnCertStatus);

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
		panelConstraintsInfo.insets = new Insets(10, 10, 0, 0);

		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 0;

		m_borderCertInfo = new TitledBorder(JAPMessages.getString("certInfoBorder"));
		m_borderCertInfo.setTitleFont(getFontSetting());
		r_panelInfo.setBorder(m_borderCertInfo);

		m_labelDate = new JLabel(JAPMessages.getString("certDate"));
		m_labelDate.setFont(getFontSetting());

		m_labelCN = new JLabel(JAPMessages.getString("certName"));
		m_labelCN.setFont(getFontSetting());
		m_labelE = new JLabel(JAPMessages.getString("certMail"));
		m_labelE.setFont(getFontSetting());
		m_labelCSTL = new JLabel(JAPMessages.getString("certLocation"));
		m_labelCSTL.setFont(getFontSetting());
		m_labelO = new JLabel(JAPMessages.getString("certOrg"));
		m_labelO.setFont(getFontSetting());
		m_labelOU = new JLabel(JAPMessages.getString("certOrgUnit"));
		m_labelOU.setFont(getFontSetting());

		m_labelDateData = new JLabel();
		m_labelDateData.setFont(getFontSetting());

		m_labelCNData = new JLabel();
		m_labelCNData.setFont(getFontSetting());

		m_labelEData = new JLabel();
		m_labelEData.setFont(getFontSetting());

		m_labelCSTLData = new JLabel();
		m_labelCSTLData.setFont(getFontSetting());

		m_labelOData = new JLabel();
		m_labelOData.setFont(getFontSetting());

		m_labelOUData = new JLabel();
		m_labelOUData.setFont(getFontSetting());

		// init with first certificate of store
		if (m_jcs != null && m_jcs.elements().hasMoreElements())
		{
			JAPCertificate j = (JAPCertificate) m_jcs.elements().nextElement();

			updateInfoPanel(j);

			if (j.getEnabled())
			{
				m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
			}
			else
			{
				m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));

			}
		}

		/*		    	gridx
		 0:				1:
		 gridy	0:
		   1:  labelDate		labelDateData
		   2:  labelCN			labelCNData
		   3:	labelE			labelEData
		   4:	labelCSTL		labelCSTLData
		   5:	labelO			labelOData
		   6:	labelOU			labelOUData
		 */

		panelConstraintsInfo.ipadx = 5;
		panelConstraintsInfo.ipady = 5;

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 1;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelDate, panelConstraintsInfo);
		r_panelInfo.add(m_labelDate);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 1;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelDateData, panelConstraintsInfo);
		r_panelInfo.add(m_labelDateData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 2;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelCN, panelConstraintsInfo);
		r_panelInfo.add(m_labelCN);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 2;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelCNData, panelConstraintsInfo);
		r_panelInfo.add(m_labelCNData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 3;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelE, panelConstraintsInfo);
		r_panelInfo.add(m_labelE);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 3;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelEData, panelConstraintsInfo);
		r_panelInfo.add(m_labelEData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 4;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelCSTL, panelConstraintsInfo);
		r_panelInfo.add(m_labelCSTL);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 4;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelCSTLData, panelConstraintsInfo);
		r_panelInfo.add(m_labelCSTLData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 5;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelO, panelConstraintsInfo);
		r_panelInfo.add(m_labelO);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 5;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelOData, panelConstraintsInfo);
		r_panelInfo.add(m_labelOData);

		panelConstraintsInfo.anchor = GridBagConstraints.WEST;
		panelConstraintsInfo.gridx = 0;
		panelConstraintsInfo.gridy = 6;
		panelConstraintsInfo.ipadx = 30;
		panelConstraintsInfo.insets = new Insets(0, 10, 0, 10);
		panelLayoutInfo.setConstraints(m_labelOU, panelConstraintsInfo);
		r_panelInfo.add(m_labelOU);

		panelConstraintsInfo.anchor = GridBagConstraints.EAST;
		panelConstraintsInfo.gridx = 1;
		panelConstraintsInfo.gridy = 6;
		panelConstraintsInfo.ipadx = 0;
		panelLayoutInfo.setConstraints(m_labelOUData, panelConstraintsInfo);
		r_panelInfo.add(m_labelOUData);

		return r_panelInfo;
	}

	/**
	 * Updates the GUI (the list of all known infoservices, the prefered infoservice, ...). This
	 * method is called automatically, if the infoservice tab comes to foreground.
	 */
	private void updateGuiOutput()
	{
		synchronized (this)
		{
			m_listmodelCertList = new DefaultListModel();

			// list init, add certificates by issuer name
			m_enumCerts = m_jcs.elements();
			while (m_enumCerts.hasMoreElements())
			{
				JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
				// was: m_listmodelCertList.addElement(issuerCN);
				m_listmodelCertList.addElement(j);
			}

			m_listCert = new JList(m_listmodelCertList);
			m_listCert.setCellRenderer(new CAListCellRenderer());
			boolean bStatusEnabled = false;
			if (m_listmodelCertList.size() > 0)
			{
				m_listCert.setSelectedIndex(0);
				JAPCertificate certActual = (JAPCertificate) m_listCert.getSelectedValue();
				bStatusEnabled = certActual.getEnabled();
			}

			m_listCert.addListSelectionListener(new ListSelectionListener()
			{

				public void valueChanged(ListSelectionEvent e)
				{

					if (m_listmodelCertList.getSize() == 0)
					{
						m_labelDateData.setText("");
						m_labelCNData.setText("");
						m_labelEData.setText("");
						m_labelCSTLData.setText("");
						m_labelOData.setText("");
						m_labelOUData.setText("");
					}
					else
					{
						JAPCertificate j = (JAPCertificate) m_listCert.getSelectedValue();
						// String currIssuerCN = (String) j.getIssuer().getValues().elementAt(0);
						updateInfoPanel(j);

						if (j.getEnabled())
						{
							m_bttnCertStatus.setText(JAPMessages.getString("certBttnDisable"));
						}
						else
						{
							m_bttnCertStatus.setText(JAPMessages.getString("certBttnEnable"));
						}
					} // else

				} // valuechanged
			});

			// m_scrpaneList = new JScrollPane();
			m_scrpaneList.getViewport().removeAll();
			m_scrpaneList.getViewport().add(m_listCert, null);
		}
	}

}

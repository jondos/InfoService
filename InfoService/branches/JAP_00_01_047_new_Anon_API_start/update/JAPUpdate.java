package update;

import gui.wizard.*;
import JAPModel;
import JAPConstants;
import javax.swing.*;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.util.Properties;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.Insets;
import java.awt.image.*;
import java.awt.Image;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.*;

import HTTPClient.*;
import java.io.IOException;
import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */
// this class shall provide the logic and windows for the JAP-Update Wizard
// using inner classes
public class JAPUpdate
{
 private JFrame parentFrame;
 private JPanel firstPanel, buttonPanel,installed, latest, description;
 private GridBagLayout gridBagFrame, gridBagPanel, gridBagInstalled, gridBagLatest;
 private GridBagConstraints cFirstPane, cButtonPanel, cInstalledPanel, cLatestPanel;
 private JButton upgrade, close, abort;
 private JAPWizardBase wizardBase;
 private JTextField textVersionInst, textDateInst, textTypeInst,
 textVersionLate, textDateLate, textTypeLate;
 private JTextArea textDescription;
 private JLabel labelVersion1, labelDate1, labelType1, labelVersion2, labelDate2, labelType2;
 private String versionInst, typeInst, dateInst, selectedVersion;
 private JAPModel japModel;
 private JComboBox boxTypeLate;
 private String[] typeString = {"Released", "Development"};
 private int totalSteps = 3;
 private JAPUpdateConnection juc;

 private File jnlpReleaseFile, jnlpDevelopmentFile;

  public JAPUpdate()
  {
  japModel = JAPModel.getModel();
  parentFrame = new JFrame("JAP-Update");
 // parentFrame.setIconImage();
 parentFrame.addWindowListener(new WindowListener(){

  public void windowClosing(WindowEvent e) {
        e.getWindow().setVisible(false);
   //     System.out.println("Window closing"+ e.toString());
        //System.exit(3);
    }

    public void windowClosed(WindowEvent e) {
        //System.out.println("Window closed"+ e.toString());
    }

    public void windowOpened(WindowEvent e) {
        //System.out.println("Window opened"+ e.toString());
    }

    public void windowIconified(WindowEvent e) {
        //System.out.println("Window iconified"+ e.toString());
    }

    public void windowDeiconified(WindowEvent e) {
       // System.out.println("Window deiconified"+ e.toString());
    }

    public void windowActivated(WindowEvent e) {
       // System.out.println("Window activated"+ e.toString());
    }

    public void windowDeactivated(WindowEvent e) {
       // System.out.println("Window deactivated"+ e.toString());
    }


  });
 /////////////////////////////////////////////
  makeIcon();
  gridBagFrame = new GridBagLayout();
  gridBagPanel = new GridBagLayout();
  buttonPanel = new JPanel();
  buttonPanel.setLayout(gridBagPanel);
  cButtonPanel = new GridBagConstraints();
  cFirstPane = new GridBagConstraints();
  parentFrame.getContentPane().setLayout(gridBagFrame);
  //firstPanel = new JPanel(gridBag);
  makeAbortButton();
  makeCloseButton();
  makeUpgradeButton();
  makeButtonPanel();
  cFirstPane.gridx = 0;
  cFirstPane.gridy = 2;
  cFirstPane.weightx = 1.0;
  cFirstPane.weighty = 1.0;
  cFirstPane.gridwidth = 3;
  cFirstPane.insets = new Insets(4,10,4,10);
  cFirstPane.fill = GridBagConstraints.HORIZONTAL;
  cFirstPane.anchor = GridBagConstraints.SOUTH;
  gridBagFrame.setConstraints(buttonPanel, cFirstPane);
  parentFrame.getContentPane().add(buttonPanel, cFirstPane);
  cFirstPane.gridx = 0;
  cFirstPane.gridy = 0;
  cFirstPane.gridwidth = 1;
  cFirstPane.weightx = 0.5;
  cFirstPane.anchor = GridBagConstraints.WEST;
  makeLabels();
  makeTextFieldsInst();
  setTextFieldsInst("0.37","Released","22.12.2001");
  makeInstalledPanel();
  gridBagFrame.setConstraints(installed, cFirstPane);
  parentFrame.getContentPane().add(installed, cFirstPane);
  makeTextFieldsLate();
  setTextFieldsLate("Released");
  makeBoxTypeLate();
  makeLatestPanel();
  cFirstPane.gridx = 1;
  cFirstPane.gridy = 0;
  cFirstPane.anchor = GridBagConstraints.EAST;
  gridBagFrame.setConstraints(latest, cFirstPane);
  parentFrame.getContentPane().add(latest, cFirstPane);
  makeTextAreaDesc();
  //setTextFieldDescription();
  makeDescription();
  cFirstPane.gridx = 0;
  cFirstPane.gridy = 1;
  cFirstPane.gridwidth = 2;
  cFirstPane.anchor = GridBagConstraints.CENTER;
  cFirstPane.insets = new Insets(10,0,1,0);
  gridBagFrame.setConstraints(description, cFirstPane);
  parentFrame.getContentPane().add(new JScrollPane(description), cFirstPane);
  buttonPanel.setVisible(true);
  //parentFrame.setSize(new Dimension(500,500));
  parentFrame.pack();
  parentFrame.setResizable(false);
  parentFrame.setVisible(true);

  juc = new JAPUpdateConnection(japModel);
  juc.connect("Released");
  setTextFieldDescription();
  parentFrame.pack();
  }

  private void makeIcon()
  {
  ImageIcon icon = new ImageIcon("images/icon16.gif");

  parentFrame.setIconImage( icon.getImage());
  }

//user's clicked Upgrade
  private void makeWizardPages()
  {
  wizardBase = new JAPWizardBase(totalSteps, japModel);
  parentFrame.setVisible(false);

  }

  private void makeLabels()
  {
  labelVersion1 = new JLabel("Version :");
  labelDate1 = new JLabel("Date :");
  labelType1 = new JLabel("Type :");
  labelVersion2 = new JLabel("Version :");
  labelDate2 = new JLabel("Date :");
  labelType2 = new JLabel("Type :");
  }

  private void makeTextFieldsInst()
  {
  textVersionInst = new JTextField();
  textTypeInst = new JTextField();
  textDateInst = new JTextField();
  }

  public void setTextFieldsInst(String versionInst, String typeInst, String dateInst)
  {
 // Border empty = new BorderFactory().createEmptyBorder();
// int i=10;
 //Dimension dim = new Dimension(85,20);

 this.versionInst = JAPConstants.aktVersion2;
 this.typeInst = typeInst;
 this.dateInst = dateInst;
 EmptyBorder empty = new EmptyBorder(0,0,0,0);
  textVersionInst.setText(this.versionInst);
  textTypeInst.setText(this.typeInst);
  textDateInst.setText(this.dateInst);
  //dim = textDateInst.getSize();
 // textVersionInst.setPreferredSize(dim);
 // textTypeInst.setPreferredSize(dim);
 // textDateInst.setPreferredSize(dim);
  textVersionInst.setBorder(empty);
  textVersionInst.setEditable(false);
  textTypeInst.setBorder(empty);
  textTypeInst.setEditable(false);
  textDateInst.setBorder(empty);
  textDateInst.setEditable(false);

  }

  public void setTextFieldsLate(String typeLate)
  {
  String versionLate;
  if(juc == null)
    {
      juc = new JAPUpdateConnection(japModel);
    }

  if(typeLate.equals((Object)"Development"))
  {
   if(juc.getDevelopmentVersion()==null)
     {
        juc.connect(typeLate);
     }
   versionLate = juc.getDevelopmentVersion();
  }
  else
  {
     if(juc.getReleasedVersion()== null)
     {
        juc.connect(typeLate);
     }
   versionLate = juc.getReleasedVersion();
  }

  String dateLate = "";
  //japModel.getInfoService().getNewVersionNumber();
  if((!versionLate.equals(versionInst) || !typeLate.equals(typeInst) || !dateLate.equals(dateInst)))
  {
  setUpgradeEnabled();
  System.out.println("Enable");
  }
  EmptyBorder empty = new EmptyBorder(0,0,0,0);
  textVersionLate.setText(versionLate);
  //textTypeLate.setText(typeLate);
  textDateLate.setText(dateLate);
  textVersionLate.setBorder(empty);
  textVersionLate.setEditable(false);
  //textTypeLate.setBorder(empty);
 // textTypeLate.setEditable(false);
  textDateLate.setBorder(empty);
  textDateLate.setEditable(false);
  }

  private void makeTextAreaDesc()
  {
  textDescription = new JTextArea();

  }

  public void setTextFieldDescription(/*String description*/)
  {
  Dimension dim = new Dimension(260,100);
  EtchedBorder etched = new EtchedBorder();
  textDescription.setBorder(etched);
  //textDescription.setSize(dim);
  textDescription.setPreferredSize(dim);
  textDescription.setMinimumSize(dim);
  textDescription.setMaximumSize(dim);
   if(juc != null)
      {
  textDescription.setText(juc.getDescription());
  //textDescription.setDocument(juc.getDocument());
      }
      textDescription.setLineWrap(true);
        textDescription.setWrapStyleWord(true);
  textDescription.setVisible(true);
  textDescription.setEditable(false);

  }

  private void makeInstalledPanel()
  {
  gridBagInstalled = new GridBagLayout();
  cInstalledPanel = new GridBagConstraints();
  TitledBorder titledBorder = new TitledBorder("Installed");
  installed = new JPanel(gridBagInstalled);
  installed.setBorder(titledBorder);
  cInstalledPanel.gridx = 0;
  cInstalledPanel.gridy = 0;
  cInstalledPanel.anchor = GridBagConstraints.WEST;
  cInstalledPanel.insets = new Insets(2,3,2,3);
  gridBagInstalled.setConstraints(labelVersion1, cInstalledPanel);
  installed.add(labelVersion1, cInstalledPanel);
  //installed.add(labelVersion1);
  cInstalledPanel.gridx = 1;
  cInstalledPanel.gridy = 0;
  gridBagInstalled.setConstraints(textVersionInst, cInstalledPanel);
  installed.add(textVersionInst, cInstalledPanel);
  //installed.add(textVersionInst);
  cInstalledPanel.gridx = 0;
  cInstalledPanel.gridy = 1;
  gridBagInstalled.setConstraints(labelDate1, cInstalledPanel);
  installed.add(labelDate1, cInstalledPanel);
  //installed.add(labelDate1);
  cInstalledPanel.gridx = 1;
  cInstalledPanel.gridy = 1;
  gridBagInstalled.setConstraints(textDateInst, cInstalledPanel);
  installed.add(textDateInst, cInstalledPanel);
  //installed.add(textDateInst);
  cInstalledPanel.gridx = 0;
  cInstalledPanel.gridy = 2;
  gridBagInstalled.setConstraints(labelType1, cInstalledPanel);
  installed.add(labelType1, cInstalledPanel);
  //installed.add(labelType1);
  cInstalledPanel.gridx = 1;
  cInstalledPanel.gridy = 2;
  gridBagInstalled.setConstraints(textTypeInst, cInstalledPanel);
  installed.add(textTypeInst, cInstalledPanel);
  //installed.add(textTypeInst);
  installed.setPreferredSize(new Dimension(120,120));
  installed.setMaximumSize(new Dimension(120,120));
  installed.setMinimumSize(new Dimension(120,120));
  installed.setVisible(true);

  }

  private void makeLatestPanel()
  {
  TitledBorder titledBorder = new TitledBorder("Latest");
  gridBagLatest = new GridBagLayout();
  cLatestPanel = new GridBagConstraints();
  latest = new JPanel(gridBagLatest);
  latest.setBorder(titledBorder);
  cLatestPanel.gridx = 0;
  cLatestPanel.gridy = 0;
  cLatestPanel.anchor = GridBagConstraints.WEST;
  cLatestPanel.insets = new Insets(2,3,2,3);
  gridBagLatest.setConstraints(labelVersion2, cLatestPanel);
  latest.add(labelVersion2, cLatestPanel);
  //latest.add(labelVersion2);
  cLatestPanel.gridx = 1;
  cLatestPanel.gridy = 0;
  gridBagLatest.setConstraints(textVersionLate, cLatestPanel);
  latest.add(textVersionLate, cLatestPanel);
  //latest.add(textVersionLate);
  cLatestPanel.gridx = 0;
  cLatestPanel.gridy = 1;
  gridBagLatest.setConstraints(labelDate2, cLatestPanel);
  latest.add(labelDate2, cLatestPanel);
  //latest.add(labelDate2);
  cLatestPanel.gridx = 1;
  cLatestPanel.gridy = 1;
  gridBagLatest.setConstraints(textDateLate, cLatestPanel);
  latest.add(textDateLate, cLatestPanel);
  //latest.add(textDateLate);
  cLatestPanel.gridx = 0;
  cLatestPanel.gridy = 2;
  gridBagLatest.setConstraints(labelType2, cLatestPanel);
  latest.add(labelType2, cLatestPanel);
  //latest.add(labelType2);
  cLatestPanel.gridx = 1;
  cLatestPanel.gridy = 2;
  gridBagLatest.setConstraints(boxTypeLate, cLatestPanel);
  latest.add(boxTypeLate, cLatestPanel);
  //latest.add(textTypeLate);
  latest.setPreferredSize(new Dimension(120,120));
  latest.setMaximumSize(new Dimension(120,120));
  latest.setMinimumSize(new Dimension(120,120));
  latest.setVisible(true);
  }

  private void makeDescription()
  {
  TitledBorder titledBorder = new TitledBorder("Description");
  description = new JPanel();
  description.setBorder(titledBorder);
  description.setPreferredSize(new Dimension(450,150));
  description.add(textDescription);

  }

  private void makeBoxTypeLate()
  {
  boxTypeLate = new JComboBox(typeString);
  boxTypeLate.setSelectedIndex(0);
  boxTypeLate.setEditable(false);
 // boxTypeLate.setPreferredSize(textDateLate.getPreferredSize());
  boxTypeLate.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent e)
    {
        //JComboBox cb = (JComboBox)e.getSource();
         selectedVersion = (String)boxTypeLate.getSelectedItem();
         setTextFieldsLate(selectedVersion);
    }

  });

  }

  private void makeTextFieldsLate()
  {
  textVersionLate = new JTextField();
 // textTypeLate = new JTextField();
  textDateLate = new JTextField();
  }

  private void makeButtonPanel()
  {
  cButtonPanel.gridx = 0;
  cButtonPanel.gridy = 0;
  cButtonPanel.weightx = 1.0;
  cButtonPanel.weighty = 1.0;
  cButtonPanel.anchor = GridBagConstraints.WEST;
  cButtonPanel.insets = new Insets(2,5,2,0);
  gridBagPanel.setConstraints(upgrade, cButtonPanel);
  buttonPanel.add(upgrade, cButtonPanel);
  cButtonPanel.gridx = 1;
  cButtonPanel.gridy = 0;
  //cButtonPanel.weightx = 1.0;

  cButtonPanel.anchor = GridBagConstraints.CENTER;
  cButtonPanel.insets = new Insets(2,20,2,5);
  gridBagPanel.setConstraints(close, cButtonPanel);
  buttonPanel.add(close, cButtonPanel);
  cButtonPanel.gridx = 2;
  cButtonPanel.gridy = 0;
  //cButtonPanel.weightx = 0.0;
  cButtonPanel.anchor = GridBagConstraints.EAST;
  cButtonPanel.insets = new Insets(2,0,2,5);
  gridBagPanel.setConstraints(abort, cButtonPanel);
  buttonPanel.add(abort, cButtonPanel);

  }

 private void makeUpgradeButton()
 {
  upgrade = new JButton("Help");
  upgrade.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doHelp();
  }
});
 upgrade.setVisible(true);
 upgrade.setEnabled(false);
 }

 private void makeAbortButton()
 {
 abort = new JButton("Close");
 abort.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doAbort();
  }
});
  abort.setVisible(true);
  abort.setEnabled(true);
}

 private void makeCloseButton()
 {
 close = new JButton("Upgrade");
 close.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doUpgrade();
  }
});
 close.setVisible(true);
 close.setEnabled(true);
 }
 //what happens now?
 public void doUpgrade()
 {
 parentFrame.setVisible(false);
 parentFrame.dispose();
 for (int i=0; i<1000;i++){System.out.println("Wait");}
 makeWizardPages();
 }

 public void doAbort()
 {
 parentFrame.setVisible(false);
 parentFrame.dispose();
 }
 //what's the difference between closing and aborting the action?
 public void doHelp()
 {

 }

 public void setTotalSteps(int totalSteps)
 {
 this.totalSteps = totalSteps;
 }

 public void setUpgradeEnabled()
 {
 upgrade.setEnabled(true);
 }

 public JAPUpdateConnection getJAPUpdateConnection()
 {
     return juc;
 }

 public static void main(String[] args)
 {
 JAPUpdate j = new JAPUpdate();
 System.out.println(j.toString());

 }
}
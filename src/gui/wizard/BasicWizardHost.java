package gui.wizard;




/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

import javax.swing.Box;
import javax.swing.border.*;
import javax.swing.border.EtchedBorder;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.*;
import javax.swing.JSeparator;
import java.awt.event.*;
import java.awt.*;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Dialog;

import java.util.Vector;

import java.net.URL;
import java.lang.*;
// this shall become the browser/wizardhost providing class ...
public class BasicWizardHost implements WizardHost,ActionListener
  {
    private JDialog m_Dialog;
    private JButton m_bttnOk;
    private JButton m_bttnCancel;
    private JButton m_bttnFinish;
    private JButton m_bttnBack;
    private JButton m_bttnNext;
    private JButton m_bttnHelp;
    private WizardPage m_currentPage;
    private int m_TotalSteps;
    private Wizard m_Wizard;

    private final static String COMMAND_NEXT="NEXT";
    private final static String COMMAND_BACK="BACK";
    private final static String COMMAND_CANCEL="CANCEL";

    public BasicWizardHost(Frame parent,Wizard wizard)
      {
        m_Wizard=wizard;
        m_currentPage=null;
        m_Dialog = new JDialog(parent,wizard.getWizardTitle(),true);
        GridBagLayout gridBag= new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        m_Dialog.getContentPane().setLayout(gridBag);

        GridBagLayout gridBagPanel=new GridBagLayout();
        GridBagConstraints cPanel = new GridBagConstraints();
        JPanel panel = new JPanel();
        panel.setLayout(gridBagPanel);

        m_bttnBack=new JButton("< Back");
        m_bttnBack.setActionCommand(COMMAND_BACK);
        m_bttnBack.addActionListener(this);
        m_bttnNext=new JButton("Next >");
        m_bttnNext.setActionCommand(COMMAND_NEXT);
        m_bttnNext.addActionListener(this);
        m_bttnHelp=new JButton("Help");
        m_bttnCancel=new JButton("Cancel");
        m_bttnCancel.setActionCommand(COMMAND_CANCEL);
        m_bttnCancel.addActionListener(this);
        m_bttnFinish=new JButton("Finish");

        //setResizable(false);
        JSeparator separator = new JSeparator();
        separator.setVisible(true);

        JPanel panelPage=new JPanel();

        cPanel.gridx = 0;
        cPanel.gridy = 0;

        cPanel.fill=GridBagConstraints.NONE;
        cPanel.anchor = GridBagConstraints.WEST;
        cPanel.weightx = 1.0;
        cPanel.weighty = 1.0;
        cPanel.insets = new Insets(10,10,10,50);
        //gridBagPanel.setConstraints(m_bttnBack,cPanel);
        panel.add(m_bttnHelp,cPanel);
        cPanel.weightx=0;
        cPanel.gridx=1;
        cPanel.insets = new Insets(10,10,10,20);
        panel.add(m_bttnCancel,cPanel);
        cPanel.gridx=2;
        cPanel.insets = new Insets(10,2,10,2);
        panel.add(m_bttnBack,cPanel);
        cPanel.gridx=3;
        panel.add(m_bttnNext,cPanel);
        cPanel.gridx=4;
        cPanel.insets = new Insets(10,20,10,10);
        panel.add(m_bttnFinish,cPanel);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight=1;
        c.fill=GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(10,10,10,10);
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridBag.setConstraints(panelPage, c);
        m_Dialog.getContentPane().add(panelPage,0);
        c.gridy = 1;
        c.fill=GridBagConstraints.HORIZONTAL;
        c.weighty=0;
        c.insets = new Insets(0,10,0,10);
        gridBag.setConstraints(separator,c);
        m_Dialog.getContentPane().add(separator);
        c.gridy = 2;
        c.insets = new Insets(0,0,0,0);
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(panel,c);
        m_Dialog.getContentPane().add(panel);
    }

  public void setWizardPage(WizardPage page)
    {
      GridBagConstraints c=new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 1;
      c.gridheight=1;
      c.fill=GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.NORTHWEST;
      c.insets = new Insets(10,10,10,10);
      c.weightx = 1.0;
      c.weighty = 1.0;
      JComponent panel=page.getPageComponent(this);
      Component oldPanel=m_Dialog.getContentPane().getComponent(0);
      m_Dialog.getContentPane().remove(0);
      ((GridBagLayout)m_Dialog.getContentPane().getLayout()).setConstraints(panel, c);
      m_Dialog.getContentPane().add(panel,0);
      if(m_currentPage==null)
        {
          m_currentPage=page;
          m_Dialog.pack();
          m_Dialog.show();
        }
      else
        {
          panel.setSize(oldPanel.getSize());
          //panel.setVisible(true);
          panel.setBackground(Color.red);
          //m_Dialog.pack();
          m_Dialog.repaint();
          m_currentPage=page;
        }
    }
/*
public void initialize()
{



 japWPage.deactivated(this);
 browser.remove((Component)japWPage);
 // --> ruft setNextWizardPage() auf
 japWPage.activated(this);
 browser.setTitle(title);
  cBrowser.gridx = 0;
  cBrowser.gridy = 0;
  cBrowser.gridwidth = 1;
 // cBrowser.ipadx = 80;
  cBrowser.anchor = GridBagConstraints.NORTH;
  cBrowser.insets = new Insets(5,5,1,5);
  cBrowser.weightx = 1.0;
  cBrowser.weighty = 0.0;
  gridBagBrowser.setConstraints((Component)japWPage, cBrowser);
  browser.getContentPane().add((Component)japWPage , cBrowser);
 browser.pack();
 browser.setVisible(true);



}

public int getTotalSteps()
{
return totalSteps;
}
 public void setFinishEnabled(boolean enabled){
 makeFinishButton().setEnabled(enabled);
 }
*/

  public Dialog getDialogParent()
    {
      return m_Dialog;
    }
 public void setHelpEnabled(boolean enabled)
  {
    m_bttnHelp.setEnabled(enabled);
  }

 public void setNextEnabled(boolean enabled)
  {
    m_bttnNext.setEnabled(enabled);
  }

  public void setBackEnabled(boolean b)
   {
      m_bttnBack.setEnabled(b);
   }

  public void setCancelEnabled(boolean b)
    {
      m_bttnCancel.setEnabled(b);
    }

  public void setFinishEnabled(boolean b)
    {
      m_bttnFinish.setEnabled(b);
    }


/*

public void doFinish()
{
try{

   System.out.print("   doFinish()");
   browser.setVisible(false);
   browser = null;
   System.gc();
   //System.exit(2);
   }catch (Exception ve)
   {
   ve.printStackTrace();
   }
}
public void doNext()
{

 japWPage.deactivated(this);
 browser.remove((Component)japWPage);
 // --> ruft setNextWizardPage() auf
 japWBase.getNextWizardPage();
 japWPage.activated(this);
 browser.setTitle(title);
  cBrowser.gridx = 0;
  cBrowser.gridy = 0;
  cBrowser.gridwidth = 1;
 // cBrowser.ipadx = 80;
  cBrowser.anchor = GridBagConstraints.NORTH;
  cBrowser.insets = new Insets(5,5,1,5);
  cBrowser.weightx = 1.0;
  cBrowser.weighty = 0.0;
  gridBagBrowser.setConstraints((Component)japWPage, cBrowser);
  browser.getContentPane().add((Component)japWPage , cBrowser);
 browser.pack();
 browser.setVisible(true);

 back.setEnabled(true);
}

//todo
public void doBack()
{
 japWPage.deactivated(this);
 //set new page
 browser.remove((Component)japWPage);
 japWBase.getLastWizardPage();
 //activate the new page
 japWPage.activated(this);
 browser.setTitle(title);
 cBrowser.gridx = 0;
  cBrowser.gridy = 0;
  cBrowser.gridwidth = 1;
 // cBrowser.ipadx = 80;
  cBrowser.anchor = GridBagConstraints.NORTH;
  cBrowser.insets = new Insets(5,5,1,5);
  cBrowser.weightx = 1.0;
  cBrowser.weighty = 0.0;
  gridBagBrowser.setConstraints((Component)japWPage, cBrowser);
  browser.getContentPane().add((Component)japWPage , cBrowser);
 browser.pack();
 browser.setVisible(true);
}
// just close browser, don't save anything
public void doCancel()
{
try{
    //browser.close();
      browser.setVisible(false);
      browser = null;
      System.gc();
      System.exit(2);
   }catch(Exception ex)
   {
   ex.printStackTrace();
   }
}

public void doHelp()
{
 //japWBase.help(japWPage,this);
}
//currently not used
public Icon getIcon()
{
//Image image = "haus13.jpg";
Icon imico = new ImageIcon("haus13.jpg");
imico.getIconHeight();
System.out.println(imico.getIconHeight()+" height");
return imico;
}

public void setButtonTexts(String[]identifier, String language)
{
this.language = language;
  help.setText(identifier[0]);
  cancel.setText(identifier[1]);
  back.setText(identifier[2]);
  next.setText(identifier[3]);
  finish.setText(identifier[4]);
}
// todo: how to make the title
public void setTitle(String title)
{
 this.title = title;
}

public void setNextWizardPage(WizardPage currentPage, int indexOfWizardPage)
{
  this.japWPage = currentPage;
  this.indexOfWizardPage = indexOfWizardPage+1;

}

*/

  public void actionPerformed(ActionEvent e)
    {
      String command=e.getActionCommand();
      if(command.equals(COMMAND_NEXT))
        {
          m_Wizard.next(m_currentPage,this);
        }
      else if(command.equals(COMMAND_BACK))
        {
          m_Wizard.back(m_currentPage,this);
        }
      else if(command.equals(COMMAND_CANCEL))
        {
          m_Wizard.wizardCompleted();
          m_Dialog.dispose();
        }
    }
}
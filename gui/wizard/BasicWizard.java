package gui.wizard;

import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Dialog;
import java.util.Vector;
import update.*;

import JAPUtil;
import JAPConstants;
//import JAPController;
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

// this class shall provide the basics for the wizard
//as flow control and so on ...
public class BasicWizard implements Wizard
  {
    //private Dialog helpDialog;
    private WizardPage currentWizardPage;
    private BasicWizardHost wizardHost;
    //private JFrame browser;
    private Vector m_Pages;
    private String m_strTitle;
    private int indexOfWizardPage;
 //   final String[] german ={"Hilfe","Abbrechen","Zurück","Weiter","Fertig"};
  //  final String[] english ={"Help","Cancel","Back","Next","Finish"};

//private JAPController japController;

public BasicWizard()
  {
    m_Pages=new Vector();
    indexOfWizardPage = 0;
  }

// todo -- what does appear if the user's clicked help
public void help(WizardPage wtp, WizardHost wh)
{
 // return helpDialog;

}

// determine number and order of the wizardpages
public WizardPage invokeWizard(WizardHost host)
 {
    host.setBackEnabled(false);
    host.setFinishEnabled(false);
    host.setWizardPage((WizardPage)m_Pages.elementAt(0));
    return null;
 }
/*
 //user's clicked back
 public void getLastWizardPage()
 {
 if(indexOfWizardPage == 0)
 {
 wizardHost.setBackEnabled(false);
 }else
 {

 currentWizardPage = wtpArray[indexOfWizardPage-1];
 indexOfWizardPage--;
 if(indexOfWizardPage == 0){wizardHost.back.setEnabled(false);}
 wizardHost.next.setEnabled(true);

 wizardHost.setNextWizardPage(currentWizardPage,indexOfWizardPage);
 }
 }
// user's clicked Next
 public void getNextWizardPage()
 {
 try{
 indexOfWizardPage++ ;
 currentWizardPage =  next(currentWizardPage, wizardHost);
 //System.out.println(currentWizardPage.toString());

 if(indexOfWizardPage == (totalSteps-1))
 {
 wizardHost.next.setEnabled(false);
 }
 //System.out.println("index "+indexOfWizardPage );
 if (currentWizardPage == null)
    {
    System.out.println("currentWizardPage ist null -- getNextWizardPage()");
    }
    }catch(Exception ve)
    {
    ve.printStackTrace();
    }
 wizardHost.setNextWizardPage(currentWizardPage,indexOfWizardPage);
// System.out.println(currentWizardPage.toString()+" getNextWizardPage() wizbase");

/// wizardHost.setButtonTexts(german,"de");
 }

 public void setWizardTitle(String title)
 {

 }
*/
 public WizardPage next(WizardPage currentPage, WizardHost host)
   {
      int pageIndex=m_Pages.indexOf(currentPage);
      pageIndex++;
      host.setBackEnabled(true);
      if(pageIndex==m_Pages.size()-1)
        {
          host.setFinishEnabled(true);
          host.setNextEnabled(false);
        }
      host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      return null;
   }

 public WizardPage back(WizardPage currentPage, WizardHost host)
   {
      int pageIndex=m_Pages.indexOf(currentPage);
      pageIndex--;
      host.setNextEnabled(true);
      host.setFinishEnabled(false);
      if(pageIndex==0)
        host.setBackEnabled(false);
      host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      return null;
   }

 public void addWizardPage(int index,WizardPage wizardPage)
  {
    m_Pages.insertElementAt(wizardPage,index);
  }


  public int initTotalSteps()
    {
      return m_Pages.size();
    }

 public WizardPage finish(WizardPage currentPage, WizardHost host){return null;}


  public void wizardCompleted()
    {

    }

/* public JAPWizardBase getWizardBase()
 {
 return this;
 }
*/
  public void setWizardTitle(String title)
    {
      m_strTitle=title;
    }

  public String getWizardTitle()
    {
      return m_strTitle;
    }
}


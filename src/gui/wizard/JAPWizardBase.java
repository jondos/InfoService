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
import java.util.Observer;
import update.*;
import JAPController;
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
public class JAPWizardBase implements Wizard{


private Dialog helpDialog;
private WizardPage currentWizardPage;
private BasicWizardHost wizardHost;
private JFrame browser;
private WizardPage[] wtpArray;
private int indexOfWizardPage;
private int totalSteps;
final String[] german ={"Hilfe","Abbrechen","Zurück","Weiter","Fertig"};
final String[] english ={"Help","Cancel","Back","Next","Finish"};

private JAPController japController;

  public JAPWizardBase(int totalSteps , JAPController japController) {
  //initOpenTool((byte)2,(byte)3);
  this.japController = japController;
  initTotalSteps(totalSteps);
  createWizardPages();
  currentWizardPage = (WizardPage)invokeWizard(wizardHost);
  indexOfWizardPage = 0;
  wizardHost = new BasicWizardHost(this, currentWizardPage);
  this.browser = wizardHost.getBrowser();
  wizardHost.setTotalSteps(this.totalSteps);
  wizardHost.setButtonTexts(english,"en");
  wizardHost.initialize();
  //System.out.println(currentWizardPage.toString());
  }


// define which pages appear in what order
private void createWizardPages()
{
wtpArray = new BasicWizardPage[totalSteps];
wtpArray[0] = new JAPWelcomeWizardPage(japController);
wtpArray[1] = new JAPDownloadWizardPage();
wtpArray[2] = new JAPFinishWizardPage("Finish");
}

// todo -- what does appear if the user's clicked help
public void help(WizardPage wtp, WizardHost wh)
{
 // return helpDialog;

}

// determine number and order of the wizardpages
public WizardPage invokeWizard(WizardHost host)
 {
  setWizardTitle("Java A Wizard");
  addWizardPage(wtpArray[0]);
  setWizardTitle("BWP-Invoke");
  addWizardPage(wtpArray[1]);
  setWizardTitle("BWP2-invoke");
  addWizardPage(wtpArray[2]);
  return wtpArray[0];
 }
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

 public WizardPage next(WizardPage currentPage, WizardHost host)
 {
 //todo!!!!!

 return wtpArray[indexOfWizardPage];
 }

 public void addWizardPage(WizardPage wizardPage){}

 public void initTotalSteps(int totalSteps)
 {
 this.totalSteps = totalSteps;
 }

 public void finish(WizardPage currentPage, WizardHost host){}

 public void wizardCompleted(){}

 public JAPWizardBase getWizardBase()
 {
 return this;
 }
public static void main(String args[])
{
//JAPWizardBase wb = new JAPWizardBase(3);
//System.out.println(wb.toString());
}

}





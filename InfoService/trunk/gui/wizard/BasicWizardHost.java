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


import java.net.URL;
import java.lang.*;
// this shall become the browser/wizardhost providing class ...
public class BasicWizardHost implements WizardHost
{

private JButton okbutton;
private WizardHost wh;
private Color color;
private JPanel panel;// panel2, panel3, panel4;
private static JFrame browser;
private JButton cancel, finish;
public JButton back, next, help;
private WizardPage japWPage;
private int totalSteps, indexOfWizardPage;
private Border etched;
private Box box;
final JAPWizardBase japWBase;
private String language, title;
private Graphics graphics;
private JSeparator separator;
private GridBagLayout gridBagBrowser, gridBagPanel;
private GridBagConstraints cBrowser, cPanel;
//private Wizard myWizard;

  public BasicWizardHost(JAPWizardBase japWBase, WizardPage japWPage)
  {

 this.japWBase = japWBase;
 this.japWPage = japWPage;

 title = "JAP Wizard";
 browser = new JFrame();
  gridBagBrowser = new GridBagLayout();
  gridBagPanel = new GridBagLayout();
  cPanel = new GridBagConstraints();
  cBrowser = new GridBagConstraints();
  panel = new JPanel();
  panel.setLayout(gridBagPanel);

  setCancelEnabled(true);
  setBackEnabled(false);
  setNextEnabled(true);
  setFinishEnabled(true);
  setHelpEnabled(true);
  browser.setTitle(title);
  makeIcon();
  browser.setResizable(false);
  browser.getContentPane().setLayout(gridBagBrowser);

  separator = new JSeparator();
  separator.setVisible(true);

  cPanel.gridx = 0;
  cPanel.gridy = 0;

  cPanel.anchor = GridBagConstraints.WEST;
  cPanel.weightx = 1.0;
  cPanel.weighty = 1.0;
  cPanel.insets = new Insets(5,5,5,0);
  gridBagPanel.setConstraints(help,cPanel);
  panel.add(help,cPanel);

  cPanel.gridx = 1;
  cPanel.anchor = GridBagConstraints.CENTER;
  cPanel.weightx = 0.0;
  cPanel.weighty = 0.0;
  cPanel.insets = new Insets(5,0,5,20);
  gridBagPanel.setConstraints(cancel,cPanel);
  panel.add(cancel,cPanel);

  cPanel.gridx = 2;
  cPanel.insets = new Insets(5,0,5,5);
  gridBagPanel.setConstraints(back,cPanel);
  panel.add(back, cPanel);
  cPanel.gridx = 3;
  cPanel.insets = new Insets(5,0,5,5);
  gridBagPanel.setConstraints(next,cPanel);
  panel.add(next, cPanel);

  cPanel.gridx = 4;
  cPanel.anchor = GridBagConstraints.EAST;
  cPanel.weightx = 1.0;
  cPanel.weighty = 1.0;
  gridBagPanel.setConstraints(finish,cPanel);
  panel.add(finish,cPanel);
 // needs a ComponentListener?
  browser.addComponentListener(new ComponentListener(){
  public void componentHidden(ComponentEvent e) {
	//System.out.println("componentHidden event from "
	//	       + e.getComponent().getClass().getName());
    }

    public void componentMoved(ComponentEvent e) {
	//System.out.println("componentMoved event from "
	//	       + e.getComponent().getClass().getName());
    }

    public void componentResized(ComponentEvent e) {
	//System.out.println("componentResized event from "
	//	       + e.getComponent().getClass().getName());
    }

  public void componentShown(ComponentEvent e) {
	//System.out.println("componentShown event from "
	//	       + e.getComponent().getClass().getName());
                       //e.getComponent().setEnabled(true);
                       e.getComponent().setVisible(true);
    }

  });
  browser.addWindowListener(new WindowListener(){

  public void windowClosing(WindowEvent e) {
        e.getWindow().setVisible(false);
   //     System.out.println("Window closing"+ e.toString());
   //     System.exit(3);
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
  browser.setSize(panel.getSize());

  cBrowser.gridx = 0;
  cBrowser.gridy = 0;
  cBrowser.gridwidth = 1;
  cBrowser.ipadx = 80;
  cBrowser.anchor = GridBagConstraints.NORTH;
  cBrowser.insets = new Insets(5,5,1,5);
  cBrowser.weightx = 1.0;
  cBrowser.weighty = 0.0;
  gridBagBrowser.setConstraints((Component)japWPage, cBrowser);
  browser.getContentPane().add((Component)japWPage , cBrowser);
  cBrowser.gridy = 1;
  cBrowser.ipadx = 500;//panel.getSize().width;
  cBrowser.insets = new Insets(5,0,0,0);
  gridBagBrowser.setConstraints(separator,cBrowser);
  browser.getContentPane().add(separator,cBrowser);
  cBrowser.gridy = 2;
  cBrowser.weightx = 1.0;
  cBrowser.weighty = 1.0;
  cBrowser.gridwidth = 3;
  cBrowser.insets = new Insets(0,5,1,5);
 // cBrowser.ipady = 20;
  cBrowser.ipadx = 80;
  cBrowser.fill = GridBagConstraints.HORIZONTAL;
  cBrowser.anchor = GridBagConstraints.SOUTH;
  gridBagBrowser.setConstraints(panel, cBrowser);
  browser.getContentPane().add(panel, cBrowser);
  browser.pack();
  browser.setVisible(false);

  }

 private void makeIcon()
 {
 ImageIcon icon = new ImageIcon("images/icon16.gif");
 browser.setIconImage(icon.getImage());
 }

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

public JFrame getBrowser()
{
return this.browser;
}


public int getTotalSteps()
{
return totalSteps;
}
 public void setFinishEnabled(boolean enabled){
 makeFinishButton().setEnabled(enabled);
 }

 public void setHelpEnabled(boolean enabled){
 makeHelpButton().setEnabled(enabled);
 }

 public void setNextEnabled(boolean enabled)
          {
          makeNextButton().setEnabled(enabled);
          }


 public void setTotalSteps(int count)
          {
          this.totalSteps = count;
        //  browser.setTitle("JAP Wizard   Step 1 of "+totalSteps);
          }
 public void setUseSwingThreadEnabled(boolean b){}

 public void setBackEnabled(boolean b)
   {
    makeBackButton().setEnabled(b);
   }

 public void setCancelEnabled(boolean b){
  makeCancelButton().setEnabled(b);
  }

  public JButton makeFinishButton()
{
finish = new JButton();
finish.setSize(50,30);
finish.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doFinish();
  }
});
finish.setVisible(true);
return finish;
}

public JButton makeCancelButton()
{
cancel = new JButton();
cancel.setSize(50,30);
cancel.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doCancel();
  }
});
cancel.setVisible(true);
return cancel;
}

public JButton makeNextButton()
{
next = new JButton();
next.setSize(50,30);
next.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doNext();
  }
});
next.setVisible(true);
return next;
}

public JButton makeBackButton()
{
back = new JButton();
back.setSize(50,30);
back.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doBack();
  }
});
back.setVisible(true);
return back;
}

public JButton makeHelpButton()
{
help = new JButton();
help.setSize(50,30);
help.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  doHelp();
  }
});
cancel.setVisible(true);
return cancel;
}

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


}
package gui.wizard;

//import java.util.Locale;
import javax.swing.*;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.event.*;
import java.awt.*;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.TitledBorder;
import java.awt.GridLayout;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import javax.swing.JCheckBox;
import java.awt.TextField;
import javax.swing.JTextField;
import java.io.*;
import java.io.FileInputStream;
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */
// this class shall provide the basic GUI features of the Wizardpages and shall being used
//for subclassing by the real WizardPages
                                       //Observable
public  class BasicWizardPage extends JPanel implements WizardPage {


private String  title, instructions;
private BasicWizardHost tw3;
private ImageIcon icon;
protected JLabel iconLabel;
private GridBagConstraints c;
private GridBagLayout gridBag;
public GridBagConstraints componentConstraints;
public GridBagLayout  componentGridBag;
private String name = "default";
private TitledBorder tborder;
public JPanel componentPanel;
  public BasicWizardPage()
  {

  gridBag = new GridBagLayout();
  c = new GridBagConstraints();
  c.fill = GridBagConstraints.HORIZONTAL;
  this.setLayout(gridBag);

 // this.setLargeIcon(getIcon());
  this.setInstructions("1. Schliessen sie das Fenster.\n"+ "\n"+
  "2. Machen Sie jenes.\n"+"3. Schliessen sie ab.\n");

 // this.setPageTitle(name);

  this.setIcon(getIcon());
  //tborder = BorderFactory.createTitledBorder("BasicWizardPage");
 // this.setBorder(tborder);
  this.setVisible(true);
  }

  public BasicWizardPage(String name) {

/////  this.name = name;
 /* gridBag = new GridBagLayout();
  c = new GridBagConstraints();
  c.fill = GridBagConstraints.HORIZONTAL;
  this.setLayout(gridBag);*/
////  createGridBagLayout();
 // this.setLargeIcon(getIcon());
////  this.setInstructions("1. Schliessen sie das Fenster.\n"+ name+"\n"+
////  "2. Machen Sie jenes.\n"+"3. Schliessen sie ab.\n");

////  this.setPageTitle(name);

////  this.setIcon(getIcon());
////  createBorder("BasicWizardPage");
  //tborder = BorderFactory.createTitledBorder("BasicWizardPage");
 // this.setBorder(tborder);
  this.setVisible(true);

  }

  public void createGridBagLayout()
  {
  gridBag = new GridBagLayout();
  c = new GridBagConstraints();
  c.fill = GridBagConstraints.HORIZONTAL;
  this.setLayout(gridBag);
  }

  public void createBorder(String title)
  {
  tborder = BorderFactory.createTitledBorder(title);
  componentPanel.setBorder(tborder);
  }

  public JPanel setContent()
  {
  return this;
  }

  public void setInstructions(String instructions){}

  public void createComponentPanel()
  {
  componentPanel = new JPanel();
  componentConstraints = new GridBagConstraints();
  componentGridBag = new GridBagLayout();
  this.setLayout(componentGridBag);
  }

  public void setPageTitle(String title)
  {
  this.title = title;
  this.setName(title);
  JLabel label = new JLabel(title);
  componentConstraints.gridx = 0;
  componentConstraints.gridy = 0;
  //c.fill = GridBagConstraints.CENTER;
 // c.gridwidth = 2;
  componentConstraints.weightx = 0.95;
  componentConstraints.weighty = 0.95;
  // c.ipady = 40;
  componentConstraints.anchor = GridBagConstraints.NORTH;
  componentConstraints.insets = new Insets(0,10,0,2);
  gridBag.setConstraints(label,componentConstraints);
  componentPanel.add(label,componentConstraints);
  }

  public void deactivated(WizardHost host)
  {
  this.setVisible(false);
  }

  public void setIcon(ImageIcon icon)
  {
  this.icon = icon;

  iconLabel = new JLabel(icon);
  c.gridx = 0;
  c.gridy = 0;
  c.weightx = 0.05;
  c.weighty = 0.05;
  //c.gridheight = 3;
  c.gridwidth = 2;
 // c.ipady = 40;
  c.fill = GridBagConstraints.BOTH;
  c.anchor = GridBagConstraints.NORTHWEST;
  c.insets = new Insets(0,10,0,20);
  gridBag.setConstraints(iconLabel,c);
  this.add(iconLabel,c);


  }

  public void setComponentPanel()
  {
  createComponentPanel();
  c.gridx = 1;
  c.gridy = 0;
  c.weightx = 0.95;
  c.weighty = 0.95;
  //c.gridheight = 3;
  c.gridwidth = 2;
 // c.ipady = 40;
  c.fill = GridBagConstraints.BOTH;
  c.anchor = GridBagConstraints.NORTH;
  c.insets = new Insets(0,10,0,20);
  gridBag.setConstraints(componentPanel,c);
  this.add(componentPanel,c);
  }

  public void activated(WizardHost host)
  {
  this.setVisible(true);
  }



  public JComponent getPageComponent(WizardHost host)
  {
  return this;
  }

  public void checkPage()
  {}

public ImageIcon getIcon()
{

 ImageIcon imico = new ImageIcon("images/install.gif");

 if (imico == null)
  {
   System.out.println("Icon not found");
  }
 //System.out.println(imico.toString()+imico.getIconHeight());

 return imico;
}

public static void main(String[]args )
{
BasicWizardPage jb = new BasicWizardPage("Hello World");
jb.setVisible(true);
System.out.println("Icon");
}

}
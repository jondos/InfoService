package update;
import gui.wizard.BasicWizardPage;
import gui.wizard.*;
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
import javax.swing.ProgressMonitor;
import java.io.*;
import java.io.FileInputStream;
/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */

public class JAPFinishWizardPage extends BasicWizardPage
{


  public JAPFinishWizardPage(String name)
  {
   this.createGridBagLayout();
  // this.setIcon(this.getIcon());
   this.setComponentPanel();
   //this.setPageTitle(name);
   this.createBorder(name);


  }

  public static void getCodeBase()
  {
    JAPUpdateConnection.getCodeBase();
  }
// now add the different Components to the ComponentPanel
  public static void main(String[] args)
  {
  JAPFinishWizardPage j= new JAPFinishWizardPage("Finish");
  j.setVisible(true);
  System.out.println(j.toString());
  }
}
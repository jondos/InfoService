package update;
import gui.wizard.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitorInputStream;
import java.awt.GridBagConstraints;
import java.awt.*;
import java.awt.event.*;

import JAPController;
import JAPInfoService;
/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */
// shall download the binaries
public class JAPWelcomeWizardPage extends BasicWizardPage
{
private JAPUpdateConnection juc;
public JProgressBar progressBar;
private JTextArea taskOutput;
private Timer timer;
private final static int ONE_TENTH_SECOND = 100;
private JAPController japController;
private JAPInfoService infoService;
private JButton test;
//private ProgressMonitorInputStream pmis;
  public JAPWelcomeWizardPage(JAPController japController)
  {
  this.japController = japController;
  infoService = japController.getInfoService();
  this.createGridBagLayout();
  // this.setIcon(this.getIcon());
   this.setComponentPanel();
   //this.setPageTitle(name);
   this.createBorder("Welcome");
   //getCodeBase();
   //pmis = new ProgressMonitorInputStream();
   makeProgressBar();
   timer = new Timer(ONE_TENTH_SECOND,new ActionListener()
       {
           public void actionPerformed(ActionEvent e)
           {
                progressBar.setValue(infoService.getCount());
                System.out.println("setValue() "+ infoService.getCount());
                if(infoService.ready == true)
                  {
                    timer.stop();
                  }
              //  taskOutput.append(task.getMessage() + newline);
             //   taskOutput.setCaretPosition(
              //          taskOutput.getDocument().getLength());
           }
       });

  }

 private void makeProgressBar()
 {
    this.componentGridBag = new GridBagLayout();
    this.componentConstraints = new GridBagConstraints();
    this.componentPanel.setLayout(componentGridBag);
    progressBar = new JProgressBar(0,3102);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    //  progressBar.setString("Download File ...");
    //this.add(progressBar);
    this.componentConstraints.gridx = 1;
    this.componentConstraints.gridy = 1;
    this.componentConstraints.insets = new Insets(3,3,3,3);
    this.componentConstraints.anchor = GridBagConstraints.SOUTH;
    //this.componentConstraints.
    this.componentGridBag.setConstraints(progressBar, componentConstraints);
    this.componentPanel.add(progressBar, componentConstraints);

    taskOutput = new JTextArea(5, 20);
    taskOutput.setMargin(new Insets(5,5,5,5));
    taskOutput.setEditable(false);

    this.componentConstraints.gridx = 1;
    this.componentConstraints.gridy = 2;
    this.componentGridBag.setConstraints(new JScrollPane(taskOutput), componentConstraints);
    this.componentPanel.add(taskOutput, componentConstraints);

    test = new JButton("Download");
    test.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
          {
            Runnable doHelloWorld = new Runnable()
              {
                public void run()
                  {
                    System.out.println("Hello World on " + Thread.currentThread());
                    downloadFile();
                  }
              };
            Thread t=new Thread(doHelloWorld);
            t.start();
          }
      });
    this.componentConstraints.gridx = 1;
    this.componentConstraints.gridy = 3;
    this.componentPanel.add(test, componentConstraints);
  }

  private void downloadFile()
  {
     //timer.start();
     infoService.connect("anon.inf.tu-dresden.de","/win/jap_swing/setup.exe", "setup.exe",progressBar);
  }

  private void makestatusField(){}

  public static void getCodeBase()
  {
    JAPUpdateConnection.getCodeBase();
  }
}
package update;

import gui.wizard.BasicWizardPage;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Component;
import javax.swing.JLabel;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.*;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import JAPUtil;
import JAPConstants;
import JAPController;
import anon.infoservice.*;
import update.JAPUpdateWizard;

import java.io.File;
import java.lang.Thread;

public class JAPDownloadWizardPage extends BasicWizardPage implements Runnable
  {
    private JLabel m_labelStatus, m_labelInformation;
    // Labels indicating the Steps of the current Update
    private JLabel m_labelStep1;
    private JLabel m_labelStep2, m_labelStep3, m_labelStep4, m_labelStep5;
    //labels as placeholders for the icon indicating which step is the current job
    private JLabel m_labelIconStep1;
    private JLabel m_labelIconStep2, m_labelIconStep3, m_labelIconStep4, m_labelIconStep5;
    private ImageIcon arrow, blank;
    private JProgressBar progressBar;
    private JPanel m_panelProgressBar;

    private String pathToJapJar;

    boolean rename = false;
    boolean renameSuccess = false;

    private String version;
    private File jnlpFile;

    private Thread observeUpdateThread;

    private JAPController japController;

    private GridBagLayout gridBagDownload;
    private GridBagConstraints constraintsDownload;

    private JAPUpdateWizard updateWizard;

    public JAPDownloadWizardPage(String version, JAPUpdateWizard updateWizard)
      {
        this.version = version;
        this.updateWizard = updateWizard;
        setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
        setPageTitle("Download");

        arrow = new ImageIcon("images/arrow.jpg");
        blank = new ImageIcon();

        gridBagDownload = new GridBagLayout();
        constraintsDownload = new GridBagConstraints();
        m_panelComponents.setLayout(gridBagDownload);



        constraintsDownload.gridx= 0;
        constraintsDownload.gridy = 0;
        constraintsDownload.gridheight = 1;
        constraintsDownload.gridwidth = 2;
        constraintsDownload.anchor = GridBagConstraints.NORTH;
        constraintsDownload.insets = new Insets(0,5,10,5);
        m_labelInformation = new JLabel("<html>Die neue JAP.jar Datei wird nun heruntergeladen.<BR>Bitte haben Sie etwa Geduld bis der Download abgeschlossen ist.</html>");
        gridBagDownload.setConstraints(m_labelInformation,constraintsDownload);
        m_panelComponents.add(m_labelInformation,constraintsDownload);

        m_labelIconStep1 = new JLabel();
        m_labelIconStep1.setIcon(arrow);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 2;
        constraintsDownload.gridheight = 1;
        constraintsDownload.gridwidth = 1 ;
        constraintsDownload.anchor = GridBagConstraints.WEST;
        gridBagDownload.setConstraints(m_labelIconStep1, constraintsDownload);
        m_panelComponents.add(m_labelIconStep1, constraintsDownload);

        m_labelStep1 = new JLabel();
        //m_labelStep1.setIcon(arrow);
        constraintsDownload.gridx = 1;
        constraintsDownload.gridy = 2;
        constraintsDownload.weightx = 1.0;
        constraintsDownload.gridheight =1;
        constraintsDownload.anchor = GridBagConstraints.WEST;
        constraintsDownload.insets = new Insets(5,5,5,5);
        gridBagDownload.setConstraints(m_labelStep1, constraintsDownload);
        m_panelComponents.add(m_labelStep1, constraintsDownload);

        m_labelIconStep2 = new JLabel();
        m_labelIconStep2.setIcon(arrow);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 3;
        constraintsDownload.weightx = 0.0;
        gridBagDownload.setConstraints(m_labelIconStep2, constraintsDownload);
        m_panelComponents.add(m_labelIconStep2, constraintsDownload);

        m_labelStep2 = new JLabel("<html><b>2. Herunterladen des Updates</b></html>");
        constraintsDownload.gridx = 1;
        constraintsDownload.gridy = 3;
        gridBagDownload.setConstraints(m_labelStep2, constraintsDownload);
        m_panelComponents.add(m_labelStep2, constraintsDownload);


        m_labelIconStep3 = new JLabel();
        m_labelIconStep3.setIcon(arrow);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 4;
        gridBagDownload.setConstraints(m_labelIconStep3, constraintsDownload);
        m_panelComponents.add(m_labelIconStep3, constraintsDownload);

        m_labelStep3 = new JLabel("<html><b>3. Erzeugen der neuen Jap.jar als Jap_"+updateWizard.version+".jar</b></html>");
        constraintsDownload.gridx = 1;
        constraintsDownload.gridy = 4;
        gridBagDownload.setConstraints(m_labelStep3, constraintsDownload);
        m_panelComponents.add(m_labelStep3, constraintsDownload);


        m_labelIconStep5 = new JLabel();
        m_labelIconStep5.setIcon(arrow);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 5;
        gridBagDownload.setConstraints(m_labelIconStep5, constraintsDownload);
        m_panelComponents.add(m_labelIconStep5, constraintsDownload);

        m_labelStep5 = new JLabel("<html><b>4. �berschreiben der alten Jap.jar</b></html>");
        constraintsDownload.gridx = 1;
        constraintsDownload.gridy = 5;
        gridBagDownload.setConstraints(m_labelStep5, constraintsDownload);
        m_panelComponents.add(m_labelStep5, constraintsDownload);
        // define an own panel for progressBar and its label

           m_panelProgressBar = new JPanel();
           GridBagLayout gridBagLayout = new GridBagLayout();
           GridBagConstraints constraintsPanelProgress = new GridBagConstraints();
           m_panelProgressBar.setLayout(gridBagLayout);
           m_labelStatus= new JLabel("<html><b>Fortschritt:</b></html>");
           constraintsPanelProgress.gridx = 0;
           constraintsPanelProgress.gridy = 0;
           constraintsPanelProgress.insets = new Insets(10,25,5,5);
           gridBagLayout.setConstraints(m_labelStatus, constraintsPanelProgress);
           m_panelProgressBar.add(m_labelStatus, constraintsPanelProgress);

           progressBar = new JProgressBar(0,10000);
           progressBar.setValue(0);
           progressBar.setStringPainted(true);
           progressBar.setPreferredSize(new Dimension(200,20));
           progressBar.setMaximumSize(new Dimension(200,20));
           m_panelProgressBar.add(progressBar);
           constraintsPanelProgress.gridx = 1;
           constraintsPanelProgress.gridy = 0;
           constraintsPanelProgress.insets = new Insets(10,5,5,5);
           gridBagLayout.setConstraints(progressBar, constraintsPanelProgress);
           m_panelProgressBar.add(progressBar, constraintsPanelProgress);

       constraintsDownload.gridx = 0;
       constraintsDownload.gridy = 6;
       constraintsDownload.gridwidth = 2;
       gridBagDownload.setConstraints(m_panelProgressBar, constraintsDownload);
       m_panelComponents.add(m_panelProgressBar, constraintsDownload);

        this.setVisible(true);
      }

      //start the Thread's run method
      public void start()
        {
          observeUpdateThread = new Thread(this);
          observeUpdateThread.start();
        }

      //method is called by JAPUpdateWizard
      public void setPath(String pathToJapJar)
      {

        this.pathToJapJar = pathToJapJar;
      /*  if (pathToJapJar.length()>50)
        {
           int i = pathToJapJar.length();
           int j = pathToJapJar.length();
           int l = 0;
            do
            {
             i = i/50;
             l = j-50;
            pathToJapJar.substring(l,j)
            }while(i==0)
        }*/

        String pre;
        String suf;
        int i;
        i = pathToJapJar.lastIndexOf(".");
        suf = pathToJapJar.substring(i);
        pre = pathToJapJar.substring(0,i);
        m_labelStep1.setText("<html><b>1. Sichern von "+pathToJapJar+" nach <BR>"+pre+JAPConstants.aktVersion2+suf+"</b></html>");

        renameJapJar(pre, suf);
      }

     private void renameJapJar(String prefix, String suffix)
     {
        File newfile = new File(prefix+JAPConstants.aktVersion2+suffix);
        File oldfile = new File(pathToJapJar);
          if( !oldfile.renameTo(newfile)||(newfile == null) )
             {
               System.out.println("Renaming failed!");
               rename = true;
               renameSuccess = false;
             }
        rename = true;
        renameSuccess = true;
     }
    //observe the steps and set the Icon
     public void run()
     {
       m_labelStep1.setIcon(arrow);
       // do the renaming
       while(!rename)
       {
         try{
          //Thread.currentThread().sleep(20);
              observeUpdateThread.sleep(100);
             }//try
             catch (java.lang.InterruptedException ie){}//catch
       }//while
       if(renameSuccess)
         {
            m_labelStep1.setIcon(blank);
         }//if
       progressBar.setValue(250);
     }//run




     public static void main( String[] args )
     {
       JFrame parent = new JFrame("parent");
       //JAPDownloadWizardPage jdw = new JAPDownloadWizardPage();
      // parent.getContentPane().add(jdw);
       parent.pack();
       parent.setVisible(true);

     }
}
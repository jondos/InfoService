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
import javax.swing.JOptionPane;

import JAPUtil;
import JAPConstants;
import JAPController;
import anon.infoservice.*;
import update.JAPUpdateWizard;

import java.io.File;
import java.io.OutputStream;
import java.io.*;
import java.lang.Thread;

import java.net.URL;

import java.math.BigInteger;

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
    //which version chose the user
    private String version;
    //which type dev or rel?
    private int type;

    private File jnlpFile;

    private Thread observeUpdateThread;

    private JAPController japController;

    private GridBagLayout gridBagDownload;
    private GridBagConstraints constraintsDownload;

    private JAPUpdateWizard updateWizard;

    private int countPackages = 0;
    private int value =0;
    private byte[] buff;
    private OutputStream os_jarFile;

    public JAPDownloadWizardPage(){}

    public JAPDownloadWizardPage(String version, int type, JAPUpdateWizard updateWizard)
      {
        this.version = version;
        this.type = type;
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

        m_labelStep5 = new JLabel("<html><b>4. Überschreiben der alten Jap.jar</b></html>");
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

           progressBar = new JProgressBar(0,500);
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
       // downloadUpdate();
      }

     public void renameJapJar(String prefix, String suffix)
     {  m_labelIconStep1.setIcon(arrow);
        File newfile = new File(prefix+JAPConstants.aktVersion2+suffix);
        File oldfile = new File(pathToJapJar);
        byte[]buffer = new byte[2048];
        //just copy the File and then rename the copy
          try
          {
             FileInputStream fis = new FileInputStream(oldfile);
             FileOutputStream fos = new FileOutputStream(newfile);
             while (fis.read(buffer)!=-1)
                {
                    fos.write(buffer);
                }
             fis.close();
             fos.flush();
             fos.close();
          }catch(Exception e)
          {
          e.printStackTrace();
          }
        /*  if( !oldfile.renameTo(newfile)||(newfile == null) )
             {
               System.out.println("Renaming failed!");
               rename = true;
               renameSuccess = false;
             }
        rename = true;
        renameSuccess = true;*/
        m_labelIconStep1.setIcon(blank);
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

    public synchronized void downloadUpdate()
      {
        InfoService infoService;
        URL jarUrl;
    //    if(japController.getInfoService()!=null)
      //          {
        //              infoService = japController.getInfoService();
          //      }else
           //     {

                      infoService = new InfoService("infoservice.inf.tu-dresden.de",6543);
                      JAPVersionInfo japVersionInfo = infoService.getJAPVersionInfo(type);
                      jarUrl = japVersionInfo.getJarUrl();
                      //os_jarFile = new OutputStream();
                     // m_labelIconStep2.setIcon(arrow);
             //   }
            try
            {
                infoService.retrieveURL(new URL("http","fg",3,"aktJap"),// jarUrl,
                new DownloadListener()
                {
                    public int progress(byte[] data,int lenData,int lenTotal,int state)
                        {

                            if(state == 1)
                              {
                                 //write data into the RAM

                                 countPackages += lenData;
                                // the Download has the Zone from 5 to 455 in the ProgressBar
                                 value = (450 * countPackages)/lenTotal;
                                 progressBar.setValue(value);
                                 progressBar.repaint();
                              }else if(state == 2)
                              {
                                  //tell the user that the download aborted
                                  showInformationDialog("Fehler beim Download des Updates.");
                                  //m_labelIconStep2.setIcon(blank);
                                  return 0;
                              }else if(state == 3)
                              {
                                 // m_labelIconStep2.setIcon(blank);
                                  return 0;
                              }
                            //System.out.println(value+" value"+countPackages+" Countpak" +lenTotal+ " Total");
                            return 0;
                        }
                });

            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            //m_labelIconStep2.setIcon(blank);
            //System.out.println("Hallo");
      }

     public void showInformationDialog(String message)
        {
            JOptionPane.showMessageDialog((Component)this, message);
        }

     public void createNewJAPJar()
     {
        //get the buffer where the data is stored
        //create a new File "Jap_"+newversion+".jar"

     }



     public static void main( String[] args )
     {
       JFrame parent = new JFrame("parent");
       //JAPDownloadWizardPage jdw = new JAPDownloadWizardPage("version",new JAPUpdateWizard("version"));
       //parent.getContentPane().add(jdw);
       //parent.pack();
       parent.setVisible(true);

     }
}
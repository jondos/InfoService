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

public class JAPDownloadWizardPage extends BasicWizardPage
  {
    protected JLabel m_labelStatus, m_labelInformation;
    // Labels indicating the Steps of the current Update
    protected JLabel m_labelStep1;
    protected JLabel m_labelStep2, m_labelStep3, m_labelStep4, m_labelStep5;
    //labels as placeholders for the icon indicating which step is the current job
    protected JLabel m_labelIconStep1;
    protected JLabel m_labelIconStep2, m_labelIconStep3, m_labelIconStep4, m_labelIconStep5;
    protected ImageIcon arrow, blank;
    protected JProgressBar progressBar;
    protected JPanel m_panelProgressBar;

    private String version;
    private int type;


    private Thread observeUpdateThread;

   // private JAPController japController;

    private GridBagLayout gridBagDownload;
    private GridBagConstraints constraintsDownload;



    public JAPDownloadWizardPage(){}

    public JAPDownloadWizardPage(String version, int type)
      {
        this.version = version;
        this.type = type;
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
        m_labelIconStep1.setPreferredSize(new Dimension(arrow.getIconWidth(),arrow.getIconHeight()));
        m_labelIconStep1.setMinimumSize(new Dimension(arrow.getIconWidth(),arrow.getIconHeight()));
        m_labelIconStep1.setVisible(false);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 2;
        constraintsDownload.gridheight = 1;
        constraintsDownload.gridwidth = 1 ;
        constraintsDownload.anchor = GridBagConstraints.WEST;
        gridBagDownload.setConstraints(m_labelIconStep1, constraintsDownload);
        m_panelComponents.add(m_labelIconStep1, constraintsDownload);

        m_labelStep1 = new JLabel();
       // m_labelStep1.setIcon(arrow);
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
        m_labelIconStep2.setVisible(false);
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
        m_labelIconStep3.setVisible(false);
        constraintsDownload.gridx = 0;
        constraintsDownload.gridy = 4;
        gridBagDownload.setConstraints(m_labelIconStep3, constraintsDownload);
        m_panelComponents.add(m_labelIconStep3, constraintsDownload);

        m_labelStep3 = new JLabel("<html><b>3. Erzeugen der neuen Jap.jar als Jap_"+version+".jar</b></html>");
        constraintsDownload.gridx = 1;
        constraintsDownload.gridy = 4;
        gridBagDownload.setConstraints(m_labelStep3, constraintsDownload);
        m_panelComponents.add(m_labelStep3, constraintsDownload);


        m_labelIconStep5 = new JLabel();
        m_labelIconStep5.setIcon(arrow);
        m_labelIconStep5.setVisible(false);
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



     public void showInformationDialog(String message)
        {
            JOptionPane.showMessageDialog((Component)this, message);
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
package update;
import gui.wizard.BasicWizardPage;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.*;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import JAPUtil;
import JAPConstants;
import JAPMessages;

public class JAPWelcomeWizardPage extends BasicWizardPage implements ActionListener
  {
    private JTextField m_tfJapPath=null;
    private JLabel m_labelClickNext;
    //search the folder for saving the new jap.jar
    private JButton m_chooseFolder_bttn = null;
    private File selectedFile;
    private String pathSelectedFile;
    public static String pathToJapJar;
    private JAPUpdateWizard updateWizard;

    private JarFileFilter jarFileFilter = new JarFileFilter();

    private final String COMMAND_SEARCH = "Durchsuchen";
    final JFileChooser m_fileChooser = new JFileChooser(System.getProperty("user.dir", ""));
    boolean checkPage = false;

    public JAPWelcomeWizardPage()
      {
        //this.updateWizard = updateWizard;
        setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
        setPageTitle(JAPMessages.getString("updateWelcomeWizardPageTitle"));

        GridBagLayout m_panelComponentsLayout = new GridBagLayout();
        GridBagConstraints m_panelConstraints = new GridBagConstraints();

        m_panelComponents.setLayout(m_panelComponentsLayout);


        JLabel label = new JLabel(JAPMessages.getString("updateIntroductionMessage"));
        m_panelConstraints.weightx = 1.0;
        m_panelConstraints.weighty = 1.0;
        m_panelConstraints.gridx = 0;
        m_panelConstraints.gridy = 0;
        m_panelConstraints.gridwidth = 2;
        m_panelConstraints.anchor = GridBagConstraints.NORTH;
        m_panelComponentsLayout.setConstraints(label, m_panelConstraints);
        m_panelComponents.add(label);


        m_tfJapPath=new JTextField(20);
        //m_tfJapPath.
        m_panelConstraints.anchor = GridBagConstraints.WEST;
        m_panelConstraints.gridx = 0;
        m_panelConstraints.gridy = 1;
        m_panelConstraints.gridwidth = 1 ;
        m_panelComponentsLayout.setConstraints(m_tfJapPath, m_panelConstraints);
        m_panelComponents.add(m_tfJapPath, m_panelConstraints);
        m_tfJapPath.setText(System.getProperty("user.dir",""));


        m_chooseFolder_bttn = new JButton(JAPMessages.getString("updateM_chooseFolder_bttn"));
        m_panelConstraints.anchor = GridBagConstraints.EAST;
        m_panelConstraints.gridx = 1;
        m_panelConstraints.gridy = 1;
        m_panelComponentsLayout.setConstraints(m_chooseFolder_bttn, m_panelConstraints);
        m_panelComponents.add(m_chooseFolder_bttn, m_panelConstraints);
        m_chooseFolder_bttn.addActionListener(this);
        m_chooseFolder_bttn.setActionCommand(COMMAND_SEARCH);

        m_labelClickNext = new JLabel(JAPMessages.getString("updateM_labelClickNext"));
        m_panelConstraints.anchor = GridBagConstraints.WEST;
        m_panelConstraints.gridx = 0;
        m_panelConstraints.gridy = 2;
        m_panelConstraints.gridwidth = 2;
        m_panelComponentsLayout.setConstraints(m_labelClickNext, m_panelConstraints);
        m_panelComponents.add(m_labelClickNext,m_panelConstraints);

      }
      // is a file chosen ?
      public boolean checkPage()
      {
         // needed for testing whether the user typed in a correct File
         File testFile;
            if(!m_tfJapPath.getText().equals(""))
                {//test whether it's a file
                  testFile = new File(m_tfJapPath.getText());
                  if(testFile.isFile() && testFile.exists())
                  {
                    checkPage = true;
                  }else
                  {
                    checkPage = false;
                  }
                }
          return checkPage;
      }

      // there is sthing wrong with the selection
      public void showInformationDialog(String message)
      {
          JOptionPane.showMessageDialog((Component)this, message);
      }

      private void createFileChooser()
      {
          this.setEnabled(false);
          final JFrame m_fileChooserDialog = new JFrame("Directory");
          final JFileChooser m_fileChooser = new JFileChooser(System.getProperty("user.dir", ""));
         // int returnval = m_fileChooser.showOpenDialog(m_fileChooserDialog);
          m_fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int returnVal = m_fileChooser.showOpenDialog(m_fileChooserDialog);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                     selectedFile = m_fileChooser.getSelectedFile();
                    System.out.println(selectedFile.getName());
                    m_fileChooserDialog.dispose();

                } else {

                }
            }
        });

          //m_fileChooserDialog.getContentPane().add(m_fileChooser);
          m_fileChooserDialog.setVisible(true);
          m_fileChooserDialog.pack();
      }

      public String getSelectedFile()
      {
          if(selectedFile!= null)
            {
              return this.selectedFile.getAbsolutePath();
            }else if(selectedFile == null && !m_tfJapPath.getText().equals(""))
            {
             //checkPage = true;
             return m_tfJapPath.getText();
            }else{// what now?
             return m_tfJapPath.getText();
                  }
      }

      public void actionPerformed(ActionEvent e)
      {
        if(e.getActionCommand().equals(COMMAND_SEARCH))
          {

             //final JFileChooser m_fileChooser = new JFileChooser(System.getProperty("user.dir", ""));
             m_fileChooser.setDialogTitle(JAPMessages.getString("updateM_fileChooserTitle"));
             m_fileChooser.setApproveButtonText(JAPMessages.getString("updateM_fileChooserApprove_bttn"));
             m_fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
             //m_fileChooser.setFileFilter(jarFileFilter);
             m_fileChooser.addChoosableFileFilter(jarFileFilter);


             int returnVal = m_fileChooser.showOpenDialog(this);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                     selectedFile = m_fileChooser.getSelectedFile();
                        if((!selectedFile.isFile()))
                          {
                            m_fileChooser.cancelSelection();

                            showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogNotAFile"));
                            m_tfJapPath.setText("");
                            checkPage = false;

                          }else if(!selectedFile.exists())
                          {
                           if(m_tfJapPath.getText().equals(""))
                              {
                                  m_fileChooser.cancelSelection();
                                  showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogFileNotExists"));
                                  m_tfJapPath.setText("");
                                  checkPage = false;
                              }else
                              {//user wrote sthing in the textfield --> test wheter it exists
                                  m_tfJapPath.getText();

                                  checkPage = true;
                              }
                          }
                          else
                          {
                    System.out.println(selectedFile.getName());
                    checkPage = true;
                    m_tfJapPath.setText(selectedFile.getAbsolutePath());

                    //updateWizard.setSelectedFile(selectedFile.getAbsolutePath());


                          }//else

                }

           }
     }

}
package update;


import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizard;
import java.io.File;

import JAPController;
public final class JAPUpdateWizard extends gui.wizard.BasicWizard
  {
    public JAPWelcomeWizardPage welcomePage;
    public JAPDownloadWizardPage downloadPage;
    public String version;
    public String selectedFile;

    public JAPUpdateWizard(String version)
      {
        this.version = version;
        setWizardTitle("JAP Update Wizard");
        welcomePage = new JAPWelcomeWizardPage(this);
        downloadPage = new JAPDownloadWizardPage( version, this );
        addWizardPage(0,welcomePage);
        addWizardPage(1,downloadPage);
        BasicWizardHost host=new BasicWizardHost(JAPController.getView(),this);
        invokeWizard(host);
      }

    public JAPWelcomeWizardPage getWelcomeWizardPage()
        {
            return welcomePage;
        }

    public void setSelectedFile (String selectedFile)
      {
        this.selectedFile = selectedFile;
        downloadPage.setPath(selectedFile);
      }

    public String getSelectedFile()
      {
        return selectedFile;
      }
  }
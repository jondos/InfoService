package update;


import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizard;
import gui.wizard.*;
import java.io.File;
import java.util.Vector;


import JAPController;
public final class JAPUpdateWizard extends gui.wizard.BasicWizard
  {
    public JAPWelcomeWizardPage welcomePage;
    public JAPDownloadWizardPage downloadPage;
    public String version;
    public String selectedFile;
    private Vector m_Pages;
    private int type;

    public JAPUpdateWizard(String version, int type)
      {
        this.version = version;
        this.type = type;
        setWizardTitle("JAP Update Wizard");
        welcomePage = new JAPWelcomeWizardPage(this);
        downloadPage = new JAPDownloadWizardPage( version, type, this );
        addWizardPage(0,welcomePage);
        addWizardPage(1,downloadPage);
        m_Pages = getPageVector();
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
        //downloadPage.setPath(selectedFile);
      }

    public String getSelectedFile()
      {
        return selectedFile;
      }

    // called by the WizardHost when the User's clicked next
    public void startUpdate()
      {
        //start the Update
       // downloadPage.initialize();
        downloadPage.setPath(selectedFile);
      }

     public WizardPage next(WizardPage currentPage, WizardHost host)
   {

      int pageIndex=m_Pages.indexOf(currentPage);
      pageIndex++;
      host.setBackEnabled(true);
      if(pageIndex==m_Pages.size()-1)
        {
          host.setFinishEnabled(true);
          host.setNextEnabled(false);

        }
      ///host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      //if it is the DownloadWizardPage
      if(pageIndex == 1)
        {
            startUpdate();
            host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
            //host.setNextEnabled(true);

            downloadPage.downloadUpdate();

        }else
        {
            host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
        }

      return null;
   }


  }
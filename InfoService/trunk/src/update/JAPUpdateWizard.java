package update;

import gui.wizard.BasicWizard;
import gui.wizard.BasicWizardHost;

import JAPController;
final class JAPUpdateWizard extends BasicWizard
  {
    public JAPUpdateWizard()
      {
        setWizardTitle("JAP Update Wizard");
        addWizardPage(0,new JAPWelcomeWizardPage());
        addWizardPage(1,new JAPDownloadWizardPage());
        BasicWizardHost host=new BasicWizardHost(JAPController.getView(),this);
        invokeWizard(host);
      }
  }
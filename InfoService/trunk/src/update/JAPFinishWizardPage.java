package update;

import JAPConstants;
import JAPUtil;

import gui.wizard.BasicWizardPage;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import JAPMessages;

public class JAPFinishWizardPage extends BasicWizardPage
{

private JLabel labelFinish;
private GridBagLayout gridBagFinish;
private GridBagConstraints constraintsFinish;

  public JAPFinishWizardPage(String name)
  {

    setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
    setPageTitle("Update-Wizard beenden");
    gridBagFinish = new GridBagLayout();
    constraintsFinish = new GridBagConstraints();

    m_panelComponents.setLayout(gridBagFinish);
    labelFinish = new JLabel(JAPMessages.getString("updateFinishMessage"));
    constraintsFinish.gridx = 0;
    constraintsFinish.gridy = 0;
    constraintsFinish.gridwidth =2;
    constraintsFinish.anchor = GridBagConstraints.CENTER;
    gridBagFinish.setConstraints(labelFinish,constraintsFinish);
    m_panelComponents.add(labelFinish, constraintsFinish);
    this.setVisible(true);
    //createGridBagLayout();
  // this.setIcon(this.getIcon());
   //this.setComponentPanel();
   //this.setPageTitle(name);
   //this.createBorder(name);


  }



}
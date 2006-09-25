package update;

import JAPConstants;
import JAPUtil;

import gui.wizard.BasicWizardPage;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import JAPMessages;

public class JAPFinishWizardPage extends BasicWizardPage
{

private JLabel labelFinish;
public JTextField tf_BackupOfJapJar;
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
    constraintsFinish.gridwidth = 2;
    constraintsFinish.anchor = GridBagConstraints.CENTER;
    gridBagFinish.setConstraints(labelFinish,constraintsFinish);
    m_panelComponents.add(labelFinish, constraintsFinish);

    tf_BackupOfJapJar = new JTextField();
    tf_BackupOfJapJar.setEditable(false);
    tf_BackupOfJapJar.setBorder(new EmptyBorder(new Insets(3,0,0,0)));
    constraintsFinish.gridx = 0;
    constraintsFinish.gridy = 1;
    constraintsFinish.gridwidth = 2;
    constraintsFinish.anchor = GridBagConstraints.WEST;
    constraintsFinish.fill = GridBagConstraints.HORIZONTAL;
    gridBagFinish.setConstraints(tf_BackupOfJapJar,constraintsFinish);
    m_panelComponents.add(tf_BackupOfJapJar, constraintsFinish);

    this.setVisible(true);
    //createGridBagLayout();
  // this.setIcon(this.getIcon());
   //this.setComponentPanel();
   //this.setPageTitle(name);
   //this.createBorder(name);


  }



}
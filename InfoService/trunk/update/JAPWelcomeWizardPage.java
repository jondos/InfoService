package update;
import gui.wizard.BasicWizardPage;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import JAPUtil;
import JAPConstants;

public class JAPWelcomeWizardPage extends BasicWizardPage
  {
    private JTextField m_tfJapPath=null;
    public JAPWelcomeWizardPage()
      {
        setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
        setPageTitle("Informationen zum Update");
        m_panelComponents.setLayout(new GridLayout(2,1));
        m_panelComponents.add(new JLabel("<html>Um das Update auf eine andere Version durchführen zu können,<BR>werden noch einige Informationen benötigt.<BR>Unten sehen Sie, welches JAP-Programm aktualisiert wird.<BR>Bitte überprüfen Sie, dass es sich um die richtige Datei handelt<BR>bzw. ändern Sie dies entsprechend.</html>"));
        m_tfJapPath=new JTextField(40);
        m_panelComponents.add(m_tfJapPath);
        m_tfJapPath.setText(System.getProperty("user.dir",""));
      }
  }
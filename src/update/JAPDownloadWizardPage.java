package update;
import gui.wizard.BasicWizardPage;
import java.awt.GridLayout;
import javax.swing.JLabel;
import JAPUtil;
import JAPConstants;

public class JAPDownloadWizardPage extends BasicWizardPage
  {
    JLabel m_labelStatus;
    public JAPDownloadWizardPage()
      {
        setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
        setPageTitle("Download");
        m_panelComponents.setLayout(new GridLayout(2,1));
        m_panelComponents.add(new JLabel("<html>Die neue JAP.jar Datei wird nun heruntergeladen.<BR>Bitte haben Sie etwa Geduld bis der Download abgeschlossen ist.</html>"));
        m_labelStatus=new JLabel();
        m_panelComponents.add(m_labelStatus);
      }
}
package jap;
import javax.swing.JPanel;
import java.awt.Window;

public interface IJAPMainView extends JAPObserver
{
	public void create(boolean bWithPay);
	public void localeChanged();
	public void registerViewIconified(Window viewIconified);
	public void disableSetAnonMode();
	public JPanel getMainPanel();
	public void doSynchronizedUpdateValues();
}

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.Frame;
import javax.swing.JLabel;

public final class JAPSplash extends Window 
	{
    public  JAPSplash (JAPModel m)
			{
				super(new Frame());
				setBackground(Color.black);
				JLabel waitLabel = new JLabel(m.getString("loading"), JLabel.CENTER);
				waitLabel.setOpaque(false);
				waitLabel.setForeground(Color.white);

				add(new JLabel(m.loadImageIcon(m.SPLASHFN, true)), BorderLayout.NORTH);
				add(waitLabel, BorderLayout.SOUTH);
	
				pack();
				m.centerFrame(this);
				setVisible(true);
		}
}



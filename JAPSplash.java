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
				JLabel waitLabel = new JLabel(m.getString("loading"), JLabel.CENTER);
				waitLabel.setBackground(Color.black);
				waitLabel.setForeground(Color.white);

				setBackground(Color.black);
				add(new JLabel(m.loadImageIcon(m.SPLASHFN, true)), BorderLayout.NORTH);
				add(new JLabel(m.loadImageIcon(m.BUSYFN, true)), BorderLayout.CENTER);
				add(waitLabel, BorderLayout.SOUTH);
	
				pack();
				m.centerFrame(this);
				setVisible(true);
		}
}



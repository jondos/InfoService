import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.Frame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

public final class JAPSplash extends Window  {
	public  JAPSplash (JAPModel m) {
		super(new Frame());
		setBackground(Color.black);
		
		JLabel splashLabel = new JLabel(m.loadImageIcon(m.SPLASHFN, false));
		
		JPanel p1 = new JPanel();
		p1.setBackground(Color.black);
		p1.setForeground(Color.white);

		JLabel waitLabel   = new JLabel(m.getString("loading"), JLabel.CENTER);
	//	waitLabel.setOpaque(false);
		waitLabel.setBackground(Color.black);
		waitLabel.setForeground(Color.white);

		ImageIcon busy = m.loadImageIcon(m.BUSYFN, false);
		JLabel busyLabel = new JLabel(busy);
		busyLabel.setBackground(Color.black);
		
		p1.add(busyLabel);
		p1.add(waitLabel);

		add(splashLabel, BorderLayout.NORTH);
		add(p1, BorderLayout.CENTER);

		pack();
		m.centerFrame(this);
		setVisible(true);
		toFront();
	}
}



import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public final class JAPLoading implements ActionListener, VersioncheckerProgress {
    private JButton b1,b2;
    private JLabel head;
    private JProgressBar p;
    private JTextArea pane;
		private JDialog dialog;
		private String returnValue;

    public  JAPLoading (JAPModel model,Frame parent) {
		dialog = new JDialog(parent);
		dialog.setTitle(model.getString("Message"));
			
		JPanel iconPanel = new JPanel();
		iconPanel.setBorder(new EmptyBorder(5,5,0,0));
		JLabel icnLabel = new JLabel(model.loadImageIcon(model.DOWNLOADFN, true));
		iconPanel.add(icnLabel);
		
		JPanel buttonPanel = new JPanel();
		b1 = new JButton("Cancel");
		b2 = new JButton("OK");
		buttonPanel.add(b1);
		buttonPanel.add(b2);
		b1.addActionListener(this);
		b2.addActionListener(this);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout() );
		mainPanel.setBorder(new EmptyBorder(10,10,10,10));
		head = new JLabel();
		head.setFont(new Font(head.getFont().getName(), head.getFont().getStyle(), head.getFont().getSize()+2));
		JPanel headPanel = new JPanel();
		headPanel.setLayout(new BorderLayout() );
		headPanel.setBorder(new EmptyBorder(0,0,20,0));
		headPanel.add(head, BorderLayout.NORTH);
		mainPanel.add(headPanel, BorderLayout.NORTH);
		pane = new JTextArea(3,40);
		pane.setBackground(dialog.getContentPane().getBackground());
		pane.setEditable(false);
		mainPanel.add(pane, BorderLayout.CENTER);
		p = new JProgressBar(JProgressBar.HORIZONTAL,0, 100);
		p.setStringPainted(true);
		mainPanel.add(p, BorderLayout.SOUTH);

		dialog.getContentPane().add(iconPanel, BorderLayout.WEST);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
		dialog.pack();
		model.centerFrame(dialog);
		dialog.setResizable(false);
	}
	
	public void progress(int percent) {
		if ((percent >= 0) && (percent <=100)) {
			p.setValue(percent);
			p.setString(String.valueOf(percent)+" %");
			if (percent==100)
				dialog.setVisible(false);
		}
	}
	
	public String message(String htext, String text, String b1text, String b2text, boolean modal, boolean progress) {
		returnValue = null;
		dialog.setModal(modal);
		dialog.setTitle(htext);
		head.setText(htext);
		pane.setText(text);
		if (b1text != null) {
			b1.setText(b1text);
			b1.setVisible(true);
		}
		else
			b1.setVisible(false);
		if (b2text != null) {
			b2.setText(b2text);
			b2.setVisible(true);
		}
		else
			b2.setVisible(false);
		p.setVisible(progress);
		dialog.setVisible(true);
		return returnValue;
	}
	
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == b1) {
			returnValue = b1.getText();
			dialog.setVisible(false);
		} else if (event.getSource() == b2) {
			returnValue = b2.getText();
			dialog.setVisible(false);
		}
	}
		
	public static void main() {
		JAPDebug d = new JAPDebug();
		JAPModel model = new JAPModel(); 
		JAPLoading l = new JAPLoading(model,new Frame());
		String answer;

		answer = l.message(model.getString("newVersionAvailableTitle"),
						   model.getString("newVersionAvailable"),
						   model.getString("newVersionNo"),
						   model.getString("newVersionYes"),
						   true,false);
		System.out.println(answer);

		answer = l.message(model.getString("downloadingProgressTitle"),
						   model.getString("downloadingProgress"),
						   null,
						   null,
						   false,true);
		int i;
		for (i = 0 ; i<101; i++) 
			for (int k=0; k<2000; k++) 
				l.progress(i);
		System.out.println(answer);
		
		answer = l.message(model.getString("newVersionAvailableTitle"),
						   model.getString("newVersionLoaded"),
						   null,
						   "OK",
						   true,false);
		System.out.println(answer);

		answer = l.message(model.getString("youShouldUpdateTitle"),
						   model.getString("youShouldUpdate")+model.getString("infoURL"),
						   null,
						   "OK",
						   true,false);
		System.out.println(answer);
		
	}
	
}


import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.GridLayout;
import java.awt.Event;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

/** This is a simple Message Box. It works with JAVA 1.1 without Swing.*/
final public class JAPAWTMsgBox extends WindowAdapter implements ActionListener
{
	private Dialog d;
	private JAPAWTMsgBox(Frame parent,String msg,String title)
		{
			d=new Dialog(parent,title,true);
			d.addWindowListener(this);
			GridLayout g=new GridLayout(0,1,0,0);
			Panel p=new Panel();
			p.setLayout(g);
			StringTokenizer st=new StringTokenizer(msg,"\n");
			while(st.hasMoreElements())
				{
					Label l=new Label(st.nextToken());
					p.add(l);
				}
			p.add(new Label(" "));
			d.add("Center",p);
			Button b=new Button("   Ok   ");
			b.addActionListener(this);
			p=new Panel();
			p.add(b);
			d.add("South",p);
			p=new Panel();
	//		p.resize(7,7);
			p.setSize(7,7);
			d.add("North",p);
			p=new Panel();
		//	p.resize(7,7);
			p.setSize(7,7);
			d.add("West",p);
			p=new Panel();
			p.setSize(7,7);
		//	p.resize(7,7);
			d.add("East",p);
			d.setResizable(false);
			d.pack();
			JAPModel.centerFrame(d);
		}
	
	/** Shows a Message Box.
	 * @param parent The owner of the Message Box.
	 * @param msg The Message to Display. Multiple line can be separate by \n
	 * @param title The Title of the Message Box (Title of the Window displayed.)
	 */
	final static public int MsgBox(Frame parent,String msg,String title)
		{
			JAPAWTMsgBox msgbox=new JAPAWTMsgBox(parent,msg,title);
			msgbox.d.show(/*true*/);
			return 0;
		}
	/*   public boolean action(Event evt, Object what) {
        if ("   Ok   ".equals(what)) {
            dispose();
            return true;
        }
        return false;
    }*/

	public void windowClosing(WindowEvent e)
		{
			d.dispose();
		}
	
	public void actionPerformed(ActionEvent e)
		{
			d.dispose();
		}
}


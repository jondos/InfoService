import java.awt.Frame;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Point;
import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JEditorPane;

import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.AttributeSet;
//import javax.swing.text.html.I
public class JAPAbout extends JDialog
	{
		class ScrollerPane extends JScrollPane implements Runnable
			{
				Dimension dimension;
				JEditorPane textArea;
				Thread t;
				private volatile boolean bRun;
				public ScrollerPane(int w,int h)
					{
						setSize(w,h);
						dimension=new Dimension(w,h);
						setMaximumSize(dimension);
						setMinimumSize(dimension);
						setBorder(null);
						setVerticalScrollBarPolicy(this.VERTICAL_SCROLLBAR_NEVER);
						setHorizontalScrollBarPolicy(this.HORIZONTAL_SCROLLBAR_NEVER);
						InputStream in=getClass().getResourceAsStream(JAPMessages.getString("htmlfileAbout"));
						byte[] buff=new byte[1500];
						int len=0;
						int aktIndex=0;
						try{
						while((len=in.read(buff,aktIndex,1500-aktIndex))>0&&aktIndex<1500)
								{
									aktIndex+=len;
								}}catch(Exception e){};	
						textArea=new JEditorPane("text/html",new String(buff).trim());
						getViewport().add(textArea);
						textArea.setOpaque(false);
						//textArea.setSize(w,textArea.getPreferredSize().height);
						//textArea.setMaximumSize(textArea.getSize());
						//textArea.setMinimumSize(textArea.getSize());
						//textArea.setPreferredSize(textArea.getSize());
						textArea.setEditable(false);
						textArea.setHighlighter(null);
						textArea.setEnabled(false);

						t=new Thread(this);
						t.setPriority(Thread.MAX_PRIORITY);
				}
				
				public void startIt()
					{
						t.start();
					}
				
				public void stopIt()
					{
						bRun=false;
						try{t.join();}catch(Exception e){};
					}
				
				public void run()
					{
	/*					Document d = textArea.getDocument();
						HTMLDocument doc = (HTMLDocument) d;
						HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
							for (; iter.isValid(); iter.next()) {
								AttributeSet a = iter.getAttributes();
								String nm = (String) a.getAttribute(HTML.Attribute.NAME);
								if ((nm != null) && nm.equals("begin"))
									{
		    // found a matching reference in the document.
		    System.out.println("found begin");
									try {
			Rectangle r = textArea.modelToView(iter.getStartOffset());
			if (r != null) {
			    // the view is visible, scroll it to the 
			    // center of the current visible area.
			  System.out.println(r.y);  
				Rectangle vis = textArea.getVisibleRect();
			    //r.y -= (vis.height / 2);
			    r.height = vis.height;
			    textArea.scrollRectToVisible(r);
			}
		    } catch (Exception ble) {
						ble.printStackTrace();
										getToolkit().beep();
		    }
								}}
		*/
						int i=100;
						Point p=new Point(0,i);
						getViewport().setViewPosition(p);
						try{Thread.sleep(1000);}catch(Exception e){}
						bRun=true;
						while(bRun)
							{
							
								if(i>=textArea.getPreferredSize().height/*-dimension.height*/)
									{	
										i=0;
									}
								//textArea.scrollRectToVisible(rec);
								//textArea.getView
								p.y=i;
								getViewport().setViewPosition(p);
								i++;
								try{Thread.sleep(65);}catch(Exception e){}
							}									
					}
			}
		ScrollerPane sp;
		
		public JAPAbout(Frame parent)
			{
				super(parent,"Info...",false);
				addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {OKPressed();}
				});	
				ImageIcon imageSplash=JAPUtil.loadImageIcon("images/splash.gif",false);
				JLabel labelSplash=new JLabel(imageSplash);
				JLabel version=new JLabel("Version: "+JAPModel.aktVersion);
				version.setFont(new Font("Sans",Font.PLAIN,11));
				version.setForeground(Color.black);
				version.setSize(version.getPreferredSize());
				JButton bttnOk=new JButton("Ok");
				bttnOk.setMnemonic('O');
				bttnOk.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				labelSplash.setSize(350,173);
				labelSplash.add(version);
				labelSplash.setLayout(null);
				labelSplash.add(bttnOk);
				bttnOk.setSize(bttnOk.getPreferredSize());
				int x=350-5-bttnOk.getSize().width;
				int y=173-5-bttnOk.getSize().height;
				bttnOk.setLocation(x,y);
				version.setLocation(x-version.getSize().width-10,y+version.getSize().height/2);
				
				setContentPane(labelSplash);
				
				sp=new ScrollerPane(180,90);
				getLayeredPane().setLayout(null);
				getLayeredPane().add(sp);
				sp.setLocation(5,70);
				setResizable(false);
				pack();
				this.setLocationRelativeTo(parent);
				setVisible(true);
				sp.startIt();
			}
		
		private void OKPressed()
			{
				sp.stopIt();
				dispose();
			}
	}

import java.awt.Frame;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
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
						setOpaque(false);
						getViewport().setOpaque(false);
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
	
						int i=100;
						Point p=new Point(0,i);
						getViewport().setViewPosition(p);
						try{Thread.sleep(1000);}catch(Exception e){}
						bRun=true;
						while(bRun)
							{
							
								try{
								if(i>=textArea.getPreferredSize().height/*-dimension.height*/)
									{	
										i=0;
									}
								//textArea.scrollRectToVisible(rec);
								//textArea.getView
								p.y=i;
								getViewport().setViewPosition(p);
								i++;}
								catch(Throwable  t)
									{									 
										//t.printStackTrace();
									}
								try{Thread.sleep(75);}catch(Exception e){}
							}									
					}
			}
		ScrollerPane sp;
		
		public JAPAbout(Frame parent)
			{
				super(parent,"Info...",false);
				Cursor oldCursor=parent.getCursor();
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {OKPressed();}
				});	
				ImageIcon imageSplash=JAPUtil.loadImageIcon("images/info.gif",false);
				JLabel labelSplash=new JLabel(imageSplash);
				JLabel verstxt=new JLabel("Version:");
				JLabel version=new JLabel(JAPModel.aktVersion);
				verstxt.setFont(new Font("Sans",Font.PLAIN,9));
				verstxt.setForeground(Color.black);
				verstxt.setSize(verstxt.getPreferredSize());
				version.setFont(new Font("Sans",Font.PLAIN,9));
				version.setForeground(Color.black);
				version.setSize(version.getPreferredSize());
				JButton bttnOk=new JButton("Ok");
				bttnOk.setMnemonic('O');
				bttnOk.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				labelSplash.setLayout(null);
				labelSplash.setSize(370,173);
				labelSplash.add(version);
				labelSplash.add(verstxt);
				labelSplash.add(bttnOk);
				bttnOk.setSize(bttnOk.getPreferredSize());
				int x=350-5-bttnOk.getSize().width;
				int y=173-5-bttnOk.getSize().height;
				bttnOk.setLocation(x,y);
				version.setLocation(x-version.getSize().width-8,y-2+version.getSize().height);
				verstxt.setLocation(version.getLocation().x, version.getLocation().y-11);
				
				setContentPane(labelSplash);
				
				sp=new ScrollerPane(220,173-52);
				getLayeredPane().setLayout(null);
				getLayeredPane().add(sp);
				sp.setLocation(4,52);
				setLocation(-380,-200);
				setVisible(true);   //we have to ensure that the window is visible before the
				setResizable(false); //get the insets - also the window must look like it should
				Insets in=getInsets(); //so for instance we need the 'NoResizable'-Border
				setResizable(true); //we want to resize
				setSize(370+in.left+in.right,173+in.bottom+in.top);
				setResizable(false); //but the user shouldn't
				setLocationRelativeTo(parent); //showing centerd to JAP-Main
				toFront();
				sp.startIt();
				parent.setCursor(oldCursor);
			}
		
		private void OKPressed()
			{
				sp.stopIt();
				dispose();
			}
	}

import java.awt.Frame;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Toolkit;
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

final class JAPAbout extends JDialog implements Runnable
	{
	
		private final static int ABOUT_DY=173;
		private final static int ABOUT_DX=350;
		private Cursor oldCursor;
		private Frame parent;
		private ScrollerPane sp;
		
		private final class ScrollerPane extends JScrollPane implements Runnable
			{
				private Dimension dimension;
				private JEditorPane textArea;
				private Thread t;
				private volatile boolean bRun;
				protected ScrollerPane(int w,int h)
					{
						setSize(w,h);
						dimension=new Dimension(w,h);
						setMaximumSize(dimension);
						setMinimumSize(dimension);
						setPreferredSize(dimension);
				//		setLayout(null);
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
						
						textArea=new JEditorPane();
						textArea.setLayout(null);
						setOpaque(false);
						getViewport().setOpaque(false);
						getViewport().add(textArea);
						textArea.setOpaque(false);
						textArea.setEditable(false);
						textArea.setHighlighter(null);
						textArea.setEnabled(false);
						textArea.setContentType("text/html");
						textArea.setText(new String(buff).trim());
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
	
						int i=0;//;dimension.height;
						Point p=new Point(0,i);
						bRun=true;
						int height;
						while(true)
							{
								try
									{
										height=textArea.getPreferredSize().height;
										break;
									}
								catch(Throwable to)
									{
										try{t.sleep(200);}catch(Exception e){}
									}
							}
						while(bRun)
							{
							
//								try{
								if(i>height)
									{	
										i=-dimension.height;
									}
								p.y=i;
								getViewport().setViewPosition(p);
								i++;
//						}
//								catch(Throwable  to1)
//									{									 
//									}
								try{t.sleep(95);}catch(Exception e){}
							}									
					}
			}
		public JAPAbout(Frame p)
			{
				super(p,"Info...",false);
				parent=p;
				oldCursor=parent.getCursor();
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				Thread theThread=new Thread(this);
				theThread.setPriority(Thread.MAX_PRIORITY);
				theThread.start(); //we have to do all the initalisation in a seperate thread in order
				//to not block the Event-Loop Thread !!! 
			}
		
		public void run()
			{
				addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {OKPressed();}
				});	
				setLocation(-380,-200);
				setVisible(true);   //now we have to ensure that the window is visible before the
				ImageIcon imageSplash=JAPUtil.loadImageIcon(JAPModel.ABOUTFN,false);//loading the Background Image
				JLabel labelSplash=new JLabel(imageSplash); //we use a JLabel to show the Background Image
				JLabel verstxt=new JLabel("Version:");
				JLabel version=new JLabel(JAPModel.aktVersion);
				verstxt.setFont(new Font("Sans",Font.PLAIN,9));
				verstxt.setForeground(Color.black);
				verstxt.setSize(verstxt.getPreferredSize());  //we set the Size of the Version-Label so that the Text 'Version' would exactly fit
				version.setFont(new Font("Sans",Font.PLAIN,9));
				version.setForeground(Color.black);
				version.setSize(version.getPreferredSize()); //resizing the VersionNumber-Label
				JButton bttnOk=new JButton("Ok");
				bttnOk.setMnemonic('O');
				bttnOk.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				labelSplash.setLayout(null); //the BackgroundImage-Label don't nedd a LayoutManager - we use absoult positioning instead
				labelSplash.setSize(ABOUT_DX,ABOUT_DY); //the set the Label size to the Size of the Image
				labelSplash.add(version); //the add the Version Number...
				labelSplash.add(verstxt); //..and text
				labelSplash.add(bttnOk); //the add the Ok-Button
				bttnOk.setSize(bttnOk.getPreferredSize()); //resizing the OK-Button
				int x=ABOUT_DX-5-bttnOk.getSize().width; //calculating the coordinates for the Button...
				int y=ABOUT_DY-5-bttnOk.getSize().height; //.. it should appear 5 Points away from the right and bottom border
				bttnOk.setLocation(x,y); //the set the Position of the Button
				//Now the set the Position of the VersionNumber...
				//...it should appear 5 Points above the bottom border (as the OK-Button does)...
				//...and 5 Points away from the OK-Button
				version.setLocation(x-5-version.getSize().width, ABOUT_DY-5-version.getSize().height);
				//Finaly the set the Position of the 'Version'-Text...
				//...it should appear at the same height as the OK-Button...
				//...and it should be in a row with the Anonym-O-Meter left border
				verstxt.setLocation(225,y);
				
				setContentPane(labelSplash); //Setting the BackgroundImage-Label with the Version-Texts and OK-button as
				labelSplash.setDoubleBuffered(false);
				
				sp=new ScrollerPane(210,173-72); //Creating a new scrolling HTML-Pane with the specified size
				getLayeredPane().setLayout(null); //we have to add the HTML-Pane at the LayerdPane, because it
				getLayeredPane().add(sp);        //should appear over the Background image
				sp.setLocation(5,62);           //setting the position of the HTML-Pane
				//Now we do a little bit tricky...
				
				//First we move the Dialog to a position were it is not seen on the Screen...
				setLocation(-380,-200);
				setVisible(true);   //now we have to ensure that the window is visible before the
				setResizable(false); //get the insets (the border around the window) - also the window must look like it should
				Insets in=getInsets(); //so for instance we need the 'NoResizable'-Border
				setResizable(true); //now we want to resize the whole dialog
				
				//We do not use pack() because it doesnt work well on Windows!
				
				setSize(ABOUT_DX+in.left+in.right,ABOUT_DY+in.bottom+in.top);// so what the background image does exactly fit
				setResizable(false); //but the user shouldn't resize the Dialog again
				//setLocationRelativeTo(parent); //now showing centerd to JAP-Main
				JAPUtil.centerFrame(this);
				toFront();
				sp.startIt(); //starting the scrolling...
				parent.setCursor(oldCursor);
			}
		
		private void OKPressed()
			{
				sp.stopIt();
				dispose();
			}
	}

package gui;

import java.awt.Image;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JButton;

import jap.JAPConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Canvas;
public final class JAPAboutAutoScroller extends Canvas/*JPanel*/ implements Runnable
	{
		private Image m_imgOffScreen;
		private Image m_imgBackground;
		private Image m_imgDoubleBuffer;
		private Image m_imgBackgroundPicture;
//    private Graphics m_graphicsOffScreen;
		private int m_iScrollAreaWidth;
		private int m_iScrollAreaHeight;
		private int m_iScrollAreaX;
		private int m_iScrollAreaY;
		private int m_iaktY;
		private int m_iTextHeight;
		private int m_iWidth;
		private int m_iHeight;
		private JEditorPane m_textArea;
		private Thread m_Thread;
		private int m_msSleep;
		private volatile boolean m_bRun;
		private Object oSync;
		private boolean isPainting;
		private JButton m_bttnOk;

		public JAPAboutAutoScroller(int width,int height,Image background,
														int scrollareax,int scrollareay,
														int scrollareawidth,int scrollareaheight,
														String htmlText)
			{
				//super(null,false);
				oSync=new Object();
				isPainting=false;
				m_iScrollAreaWidth=scrollareawidth;
				m_iScrollAreaHeight=scrollareaheight;
				m_iScrollAreaX=scrollareax;
				m_iScrollAreaY=scrollareay;
				m_iWidth=width;
				m_iHeight=height;
				setSize(width,height);
				this.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if(m_bttnOk.getBounds().contains(e.getPoint()))
							m_bttnOk.doClick();
					}
				});
				m_imgBackgroundPicture=background;
				m_textArea=new JEditorPane();
				m_textArea.setOpaque(false);
				m_textArea.setEditable(false);
				m_textArea.setHighlighter(null);
				m_textArea.setEnabled(false);
				m_textArea.setSize(m_iScrollAreaWidth,10000);
				m_textArea.setContentType("text/html");
				m_textArea.setText(htmlText.trim());

				m_iTextHeight=m_textArea.getPreferredSize().height;
				System.out.println(m_iTextHeight);

				m_bttnOk=new JButton("Ok");
				m_bttnOk.setMnemonic('O');
				m_bttnOk.setOpaque(false);
				m_bttnOk.setSelected(true);
				m_bttnOk.setSize(m_bttnOk.getPreferredSize()); //resizing the OK-Button

				m_Thread=new Thread(this);
				m_bRun=false;
			}

		public void addActionListener(ActionListener l)
			{
				m_bttnOk.addActionListener(l);
			}

		public synchronized void startScrolling(int msScrollTime)
			{
				if(m_bRun)
					return;
				m_msSleep=msScrollTime;
				m_Thread.start();
			}

		public synchronized void stopScrolling()
			{
				m_bRun=false;
				try{m_Thread.join();}catch(Exception e){};
			}


		/**
			* override update to *not* erase the background before painting
			*/
		public void update(Graphics g)
			{
				paint(g);
			}

		public void paint(Graphics g1)
			{
				if(g1==null)
					return;
				synchronized(oSync)
					{
						if(isPainting)
							return;
						isPainting=true;
					}

				if(m_imgOffScreen == null)
					{
						m_imgOffScreen = createImage(m_iScrollAreaWidth,m_iTextHeight+2*m_iScrollAreaHeight);
						Graphics graphicsOffScreen = m_imgOffScreen.getGraphics();
						//graphicsOffScreen.setClip(0,0,m_iScrollAreaWidth,m_iTextHeight);
						try{
						m_textArea.paint(graphicsOffScreen);
						}
						catch(Exception e)
							{
								System.out.println("Erroe");
								graphicsOffScreen.dispose();
								m_imgOffScreen=null;
								isPainting=false;
								return;
							}
						graphicsOffScreen.dispose();

						m_imgBackground=createImage(m_iWidth,m_iHeight);
						Graphics g2=m_imgBackground.getGraphics();
						g2.drawImage(m_imgBackgroundPicture,0,0,null);
						int x=m_iWidth-5-m_bttnOk.getSize().width; //calculating the coordinates for the Button...
						int y=m_iHeight-5-m_bttnOk.getSize().height; //.. it should appear 5 Points away from the right and bottom border
						m_bttnOk.setLocation(x,y); //the set the Position of the Button

						Font f=new Font("Sans",Font.PLAIN,9);
						g2.setFont(f);
						g2.setColor(Color.black);
						FontMetrics fm=g2.getFontMetrics();
						int w=fm.stringWidth("Version:");
						//Finaly we do the same for the 'Version'-Text
						g2.drawString("Version",x-5-w,y);
						//Now the set the Position of the VersionNumber...
						//...it should appear 5 Points above the bottom border (as the OK-Button does)...
						//...and 5 Points away from the OK-Button
						w=fm.stringWidth(JAPConstants.aktVersion);
						g2.drawString(JAPConstants.aktVersion,x-5-w, m_iHeight-5-fm.getHeight());
						g2.translate(x,y);
						m_bttnOk.paint(g2);
						g2.dispose();

						m_imgDoubleBuffer=createImage(m_iWidth,m_iHeight);
					}

				Graphics g=m_imgDoubleBuffer.getGraphics();
				g.drawImage(m_imgBackground,0,0,null);
				if(m_iaktY<=m_iScrollAreaHeight)
					g.drawImage(m_imgOffScreen, m_iScrollAreaX,m_iScrollAreaY+m_iScrollAreaHeight-m_iaktY,m_iScrollAreaX+m_iScrollAreaWidth,m_iScrollAreaY+m_iScrollAreaHeight,0,0,m_iScrollAreaWidth,m_iaktY ,null);
				else
					g.drawImage(m_imgOffScreen, m_iScrollAreaX, m_iScrollAreaY,m_iScrollAreaWidth+m_iScrollAreaX,m_iScrollAreaHeight+m_iScrollAreaY,0,m_iaktY-m_iScrollAreaHeight,m_iScrollAreaWidth,m_iaktY ,null);
				g.dispose();
				g1.drawImage(m_imgDoubleBuffer,0,0,null);
				isPainting=false;
			}

		public void run()
			{
				m_iaktY=0;
				m_bRun=true;
				while(m_bRun)
					{
						m_iaktY++;
						//repaint();
						paint(getGraphics());
						try
							{
								Thread.sleep(m_msSleep);
							}
						catch(Exception e){};
						if(m_iaktY>m_iTextHeight+m_iScrollAreaHeight)
							m_iaktY=0;
					}
			}
	}
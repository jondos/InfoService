package gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import jap.JAPUtil;
public class MyPanel extends JPanel
{
  JPanel mainPanel;
  JPanel smallPanel;
  JPanel fullPanel;
  JToggleButton bttn;
  CardLayout l;
  public MyPanel()
  {
	GridBagLayout gbl=new GridBagLayout();
	GridBagConstraints c=new GridBagConstraints();
	setLayout(gbl);
 //  	setBackground(Color.DARK_GRAY);
	bttn=new JToggleButton(JAPUtil.loadImageIcon("arrow.gif",true));
	bttn.setSelectedIcon(JAPUtil.loadImageIcon("arrow90.gif",true));
	bttn.setBorderPainted(false);
	bttn.setContentAreaFilled(false);
	bttn.setFocusPainted(false);
	c.insets=new Insets(0,0,0,0);
	c.anchor=GridBagConstraints.NORTHWEST;
	bttn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
		  {if(bttn.isSelected())
			l.last(mainPanel);
				else
			  l.first(mainPanel);
		  }});
	add(bttn,c);
	c.gridx=1;
	c.fill=GridBagConstraints.BOTH;
	c.weightx=1;
	c.weighty=1;
	c.insets=new Insets(0,0,0,0);
  mainPanel=new JPanel();
	//addComponentListener(this);
	l=new CardLayout();
	mainPanel.setLayout(l);
	//mainPanel.setSize(200,200);
	//mainPanel.setLocation(20,5);
	mainPanel.setBackground(Color.blue);
	add(mainPanel,c);
	smallPanel=new JPanel();
	smallPanel.setBackground(Color.green);
	smallPanel.add(new JButton("Help"));
	mainPanel.add(smallPanel,"FIRST");
	fullPanel=new JPanel();
	fullPanel.add(new JTextArea(10,10));
	fullPanel.setBackground(Color.red);
	mainPanel.add(fullPanel,"FULL");
  }

public void setFullPanel(JPanel p)
{
	mainPanel.remove(1);
	fullPanel=p;
	mainPanel.add(fullPanel,"FULL");
}
  public Dimension getPreferredSize()
  {
	  if(bttn.isSelected())
		  return fullPanel.getPreferredSize();
	  else
		  return smallPanel.getPreferredSize();
  }

}

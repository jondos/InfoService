package gui;

import java.awt.GridLayout;
import java.util.StringTokenizer;
import javax.swing.JPanel;
import javax.swing.JLabel;
public class JAPMultilineLabel extends JPanel
	{
		public JAPMultilineLabel(String s)
			{
				GridLayout g=new GridLayout(0,1,0,0);
				setLayout(g);
				StringTokenizer st=new StringTokenizer(s,"\n");
				while(st.hasMoreElements())
					{
						JLabel l=new JLabel(st.nextToken());
						add(l);
					}
			}
	}
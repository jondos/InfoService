/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package pay.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jap.JAPController;
import jap.JAPModel;
import jap.JAPObserver;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import pay.control.PayControl;
import pay.event.ModelEvent;
import pay.event.ModelListener;
import pay.util.PayText;

/**
 * Klasse zum darstellen der UserDaten Buttons zum Passwort ändern etc.
 * Außerdem dient sie der Benutzerinteraktion bezüglich der genannten Daten.
 * @author Grischan Glänzel
 */

public class UserDatenView extends JPanel implements ModelListener, JAPObserver, ActionListener
{

	private JAPModel japModel;
	private JAPController japController;
	Component elter;

	KLabel ssl;
	//KLabel kontoDat;
	//KLabel pw;


//Componeneten
	JCheckBox savePw;
	JButton changePassword;
	JButton ausfahren;

// Componenten für Zusatz Menu
	JPanel zusatzMenu;
	Component platzhalter;
	KLabel host;
	KLabel port;
	JButton reset;
	JButton save;
	JButton exportAccountFile;
	JButton importAccountFile;

	//UserDaten user;

	MyGridBag grid;
	boolean zusatzMenuAusgefahren = false;
	private UserDatenView view;
	;

	public void initUserData()
	{
		host.setDText(japModel.getBIHost());
		port.setDText(Integer.toString(japModel.getBIPort()));
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, japModel.getBIHost());
	}

	public void setUserData(String biHost, int biPort)
	{
		host.setDText(biHost);
		port.setDText(Integer.toString(biPort));
	}

	public UserDatenView(Container parent)
	{
		this.elter = parent;
		japModel = JAPModel.create();
		japController = JAPController.create();
		setLayout(new GridBagLayout());
		MyGridBag grid = new MyGridBag();
		savePw = new JCheckBox(PayText.get("saveAccountsEncrypted"));
		changePassword = new JButton(PayText.get("changePassword"));
		ausfahren = new JButton("erweitert");

		savePw.setSelected(Pay.getInstance().isAccountFileEncrypted());

		add(changePassword, grid.feld(0, 0).size(1, 1).fill(grid.HORIZONTAL));
		add(savePw, grid.feld(1, 0));
		add(Box.createHorizontalStrut(20), grid.feld(2, 0));
		add(ausfahren, grid.feld(3, 0));
		add(Box.createVerticalStrut(30), grid.feld(0, 1).remain());

// zusatz Menu
		zusatzMenu = new JPanel();
		platzhalter = Box.createVerticalStrut(30);
		zusatzMenu.setLayout(new GridBagLayout());
		grid = new MyGridBag();
		host = new KLabel(PayText.get("host"));
		port = new KLabel(PayText.get("port"));
		//reset = new JButton(PayText.get("reset"));
		//save = new JButton(PayText.get("save"));
		//ssl = new KLabel(" : ");
		exportAccountFile = new JButton(PayText.get("exportAccountFile"));
		importAccountFile = new JButton(PayText.get("importAccountFile"));

		initUserData();

		zusatzMenu.add(host, grid.feld(0, 0).inset(0, 0, 0, 2).size(1, 2));
		zusatzMenu.add(port, grid.feld(1, 0));
		//zusatzMenu.add(reset,grid.feld(2,0).size(1,1));
		//zusatzMenu.add(save,grid.feld(2,1));
		zusatzMenu.add(exportAccountFile, grid.feld(2, 0).size(1, 1).fill(grid.HORIZONTAL));
		zusatzMenu.add(importAccountFile, grid.feld(2, 1));

		add(zusatzMenu, grid.feld(0, 2).remain());
		remove(zusatzMenu);

		ausfahren.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				zusatzMenu();
			}
		});
		/*
		   port.addActionListener(new ActionListener(){
		 public void actionPerformed(ActionEvent e){
		  try{
		   user.Port = Integer.parseInt(e.getActionCommand());
		  } catch (Exception ex){port.setText(""+user.Port);}
		 }
		   });

		   reset.addActionListener(new ActionListener(){
		 public void actionPerformed(ActionEvent e){
		   user = user.reset();
		   setUserD(user);
		   save.doClick();
		   LogHolder.log(this," reset",Log.TEST);
		 }
		   });

		   save.addActionListener(new ActionListener(){
		 public void actionPerformed(ActionEvent e){
		  updateViewAndData();
		  LogHolder.log(this,"save ",Log.TEST);
		 }
		   });
		 */
		savePw.addActionListener(new PayControl.EncryptAccountFileListener());
		changePassword.addActionListener(new PayControl.ChangeAccountFilePWListener());

		importAccountFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(getParent());
				Pay.getInstance().importAccountFile(chooser.getSelectedFile().getAbsolutePath());
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, " importAccFile");
			}
		});
		exportAccountFile.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(getParent());
				Pay.getInstance().exportAccountFile(chooser.getSelectedFile().getAbsolutePath());
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "exportAccountFile to " + chooser.getSelectedFile().getAbsolutePath());
			}
		});

		//setPreferredSize(new Dimension(200,200));
	}

	public void zusatzMenu()
	{
		grid = new MyGridBag();
		if (zusatzMenuAusgefahren)
		{
			remove(zusatzMenu);
			zusatzMenuAusgefahren = false;
			elter.repaint();
		}
		else
		{
			add(zusatzMenu, grid.feld(0, 2).remain());
			zusatzMenuAusgefahren = true;
			elter.repaint();
			elter.setSize(getWidth() + 1, getHeight() + 1);
		}

	}

	public void modelUpdated(ModelEvent me)
	{
		savePw.setSelected(Pay.getInstance().isAccountFileEncrypted());
	}

	public void valuesChanged()
	{
		host.setDText(japModel.getBIHost());
		port.setDText(Integer.toString(japModel.getBIPort()));
	}

	public void actionPerformed(ActionEvent e)
	{
		japController.setBIHost(host.getText());
		japController.setBIPort(Integer.parseInt(port.getText()));
	}

	public void channelsChanged(int i)
	{}

	public void transferedBytes(int i)
	{}

	private void updateViewAndData()
	{
		//LogHolder.log(this,"Alte UserDaten: "+user,Log.TEST);
		//UserDaten ud = user.write(host.getText(),Integer.parseInt(port.getText()));
		//LogHolder.log(this,"Neue User daten: "+ud,Log.TEST);
		//repaint();
	}

	private class KLabel extends JPanel
	{
		String st;
		JLabel one;
		JTextField two;
		public KLabel(String st)
		{
			setLayout(new GridLayout(2, 1));
			one = new JLabel();
			two = new JTextField();
			one.setText(st);
			add(one);
			add(two);
		}

		public void setDText(String ds)
		{
			//two.setForeground(Color.black);
			//setBackground(Color.white);
			two.setText(ds);
		}

		public void addActionListener(ActionListener al)
		{
			two.addActionListener(al);
		}

		public void setText(String st)
		{
			two.setText(st);
		}

		public String getText()
		{
			return two.getText();
		}
	}

}

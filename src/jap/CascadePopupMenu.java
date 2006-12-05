/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package jap;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ComponentAdapter;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import java.awt.Component;
import java.awt.Window;
import javax.swing.JComponent;
import java.awt.Point;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import anon.infoservice.MixCascade;
import javax.swing.ImageIcon;
import java.util.Enumeration;
import anon.infoservice.Database;
import java.util.Vector;
import java.awt.Font;
import gui.GUIUtils;
import java.awt.Dimension;
import java.util.Hashtable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeListener;
import anon.infoservice.NewCascadeIDEntry;
import java.awt.Color;

/**
 *
 * @author Rolf Wendolsky
 */
public class CascadePopupMenu
{
	private static final int MAX_CASCADE_NAME_LENGTH = 30;
	private final Color m_newCascadeColor = new Color(255, 255, 170);

	private ExitHandler m_exitHandler;
	private JPopupMenu m_popup;
	private Hashtable m_menuItems;
	private ActionListener m_cascadeItemListener;
	private TrustModel m_trustModel;
	private int m_headerHeight = 0;

	public CascadePopupMenu()
	{
		this (new JPopupMenu());
	}

	public CascadePopupMenu(JPopupMenu a_popup)
	{
		if (a_popup == null)
		{
			throw new IllegalArgumentException("Given argument is null!");
		}
		m_popup = a_popup;
		m_popup.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (SwingUtilities.isRightMouseButton(a_event))
				{
					m_popup.setVisible(false);
				}
			}
			public void mouseExited(MouseEvent a_event)
			{
				Component component = m_popup.getComponentAt(a_event.getPoint());
				if (component == null || component.getParent() != m_popup)
				{
					m_exitHandler.exited();
				}
			}
		});


		m_menuItems = new Hashtable();
		m_cascadeItemListener = new CascadeItemListener();
		registerExitHandler(null);
	}

	public Point getRelativePosition(Point a_pointOnScreen)
	{
		return GUIUtils.getRelativePosition(a_pointOnScreen, m_popup);
	}

	public Point getMousePosition()
	{
		//m_popup.getMousePosition()
		return GUIUtils.getMousePosition(m_popup);
	}

	public void registerExitHandler(ExitHandler a_exitHandler)
	{
		if (a_exitHandler != null)
		{
			m_exitHandler = a_exitHandler;
		}
		else
		{
			m_exitHandler = new ExitHandler()
			{
				public void exited()
				{
					// do nothing
				}
			};
		}
	}

	public void show(Component a_parent, Point a_pointOnScreen)
	{
		Point location = calculateLocationOnScreen(a_parent, a_pointOnScreen);
		Window parentWindow = GUIUtils.getParentWindow(a_parent);
		m_popup.show(a_parent, location.x - parentWindow.getLocation().x,
					 location.y - parentWindow.getLocation().y);
	}

	public void setLocation(Point a_point)
	{
		m_popup.setLocation(a_point);
	}

	public TrustModel getTrustModel()
	{
		return m_trustModel;
	}

	public int getHeaderHeight()
	{
		return m_headerHeight;
	}

	public boolean update(TrustModel a_trustModel)
	{
		boolean updated = false;

		if (a_trustModel == null)
		{
			throw new IllegalArgumentException("Given argument is null!");
		}
		m_trustModel = a_trustModel;

		Hashtable hashCascades = Database.getInstance(MixCascade.class).getEntryHash();
		MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();
		if (currentCascade != null)
		{
			hashCascades.put(currentCascade.getId(), currentCascade);
		}
		Enumeration cascades = hashCascades.elements();

		if (cascades.hasMoreElements())
		{
			MixCascade cascade;
			JMenuItem menuItem;

			ImageIcon icon;
			Vector userDefined = new Vector();

			m_popup.removeAll();
			m_menuItems.clear();


			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.CENTER;
			panel.add(new JLabel(m_trustModel.getName()), constraints);
			m_popup.add(panel);
			JPopupMenu.Separator separator = new JPopupMenu.Separator();
			m_popup.add(separator);
			m_headerHeight = panel.getPreferredSize().height + separator.getPreferredSize().height;
			//m_headerHeight = m_popup.getPreferredSize().height;

			while (cascades.hasMoreElements())
			{
				cascade = (MixCascade) cascades.nextElement();
				if (!m_trustModel.isTrusted(cascade))
				{
					continue;
				}

				if (cascade.isPayment())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT);
				}
				else if (cascade.isUserDefined())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUELL);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET);
				}
				menuItem = new JMenuItem(
								GUIUtils.trim(cascade.toString(), MAX_CASCADE_NAME_LENGTH), icon);
				if (isNewCascade(cascade))
				{
					menuItem.setBackground(m_newCascadeColor);
				}
				if (currentCascade != null && currentCascade.equals(cascade))
				{
					menuItem.setFont(new Font(menuItem.getFont().getName(), Font.BOLD,
											  menuItem.getFont().getSize()));
					m_popup.insert(menuItem, 2);
				}
				else
				{
					menuItem.setFont(new Font(menuItem.getFont().getName(), Font.PLAIN,
											  menuItem.getFont().getSize()));
					if (cascade.isUserDefined())
					{
						userDefined.addElement(menuItem);
					}
					else
					{
						m_popup.add(menuItem);
					}
				}
				menuItem.addActionListener(m_cascadeItemListener);
				m_menuItems.put(menuItem, cascade);
				updated = true;
			}
			for (int i = 0; i < userDefined.size(); i++)
			{
				m_popup.add( (JMenuItem) userDefined.elementAt(i));
			}
		}
		return updated;
	}

	public Point calculateLocationOnScreen(Component a_parent, Point a_pointOnScreen)
	{
			int x = a_pointOnScreen.x;
			int y = a_pointOnScreen.y;
			GUIUtils.Screen screen = GUIUtils.getCurrentScreen(a_parent);
			Dimension size = m_popup.getPreferredSize();
			if (x + size.width > screen.getX() + screen.getWidth())
			{
				x = screen.getX() + screen.getWidth() - size.width;
			}
			if (y + size.height > screen.getY() + screen.getHeight())
			{
				y = screen.getY() + screen.getHeight() - size.height;
			}

			// optimize the place on the screen
			x = Math.max(x, screen.getX());
			y = Math.max(y, screen.getY());

			return new Point(x, y);
	}

	public int getWidth()
	{
		return (int)m_popup.getPreferredSize().width;
	}

	public boolean isVisible()
	{
		return m_popup.isVisible();
	}

	public void setVisible(boolean a_bVisible)
	{
		m_popup.setVisible(a_bVisible);
	}

	public interface ExitHandler
	{
		public void exited();
	}

	private boolean isNewCascade(MixCascade a_cascade)
	{
		if ( (Database.getInstance(NewCascadeIDEntry.class).getNumberOfEntries() * 2 <
			  Database.getInstance(MixCascade.class).getNumberOfEntries()) &&
			 Database.getInstance(NewCascadeIDEntry.class).getEntryById(
					  a_cascade.getMixIDsAsString()) != null)
		{
			   return true;
		}
		return false;
	}

	private class CascadeItemListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a_event)
		{
			MixCascade cascade = (MixCascade)m_menuItems.get(a_event.getSource());
			if (cascade != null)
			{
				TrustModel.setCurrentTrustModel(m_trustModel);
				JAPController.getInstance().setCurrentMixCascade(cascade);
				m_popup.setVisible(false);
			}
		}
	}
}

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
package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.util.Random;

/**
 *
 * @author Rolf Wendolsky
 */
public class PopupMenu
{
	private ExitHandler m_exitHandler;
	private JPopupMenu m_popup;

	public PopupMenu()
	{
		this (new JPopupMenu());
	}

	public PopupMenu(JPopupMenu a_popup)
	{
		if (a_popup == null)
		{
			throw new IllegalArgumentException("Given argument is null!");
		}

		m_popup = a_popup;
		m_popup.setName(Double.toString(new Random().nextDouble()));
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

		registerExitHandler(null);
	}

	public final void addPopupMenuListener(PopupMenuListener a_listener)
	{
		m_popup.addPopupMenuListener(a_listener);
	}

	public final void removePopupMenuListener(PopupMenuListener a_listener)
	{
		m_popup.removePopupMenuListener(a_listener);
	}




	public final Point getRelativePosition(Point a_pointOnScreen)
	{
		return GUIUtils.getRelativePosition(a_pointOnScreen, m_popup);
	}

	public final Point getMousePosition()
	{
		//m_popup.getMousePosition()
		return GUIUtils.getMousePosition(m_popup);
	}

	public final void registerExitHandler(ExitHandler a_exitHandler)
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

	public final void show(Component a_parent, Point a_pointOnScreen)
	{
		Point location = calculateLocationOnScreen(a_parent, a_pointOnScreen);
		Window parentWindow = GUIUtils.getParentWindow(a_parent);
		m_popup.show(a_parent, location.x - parentWindow.getLocation().x,
					 location.y - parentWindow.getLocation().y);
	}

	public final void setLocation(Point a_point)
	{
		m_popup.setLocation(a_point);
	}

	protected final JPopupMenu getPopupMenu()
	{
		return m_popup;
	}

	public final Point calculateLocationOnScreen(Component a_parent, Point a_pointOnScreen)
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

	public final int getWidth()
	{
		return (int)m_popup.getPreferredSize().width;
	}

	public final int getHeight()
	{
		return (int)m_popup.getPreferredSize().height;
	}


	public final boolean isVisible()
	{
		return m_popup.isVisible();
	}

	public final void setVisible(boolean a_bVisible)
	{
		m_popup.setVisible(a_bVisible);
	}

	public interface ExitHandler
	{
		public void exited();
	}
}

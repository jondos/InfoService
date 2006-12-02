package jap;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
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

/**
 *
 * @author Rolf Wendolsky
 */
public class CascadePopupMenu
{
	private static final int MAX_CASCADE_NAME_LENGTH = 30;

	private ExitHandler m_exitHandler;
	private JPopupMenu m_popup;
	private Hashtable m_menuItems;
	private ActionListener m_cascadeItemListener;

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

	public void update()
	{
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
			panel.add(new JLabel("Test"), constraints);
			m_popup.add(panel);

			m_popup.addSeparator();
			while (cascades.hasMoreElements())
			{
				cascade = (MixCascade) cascades.nextElement();
				//if (!JAPModel.getInstance().getTrustModel().isTrusted(cascade))
				{
					//continue;
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
			}
			for (int i = 0; i < userDefined.size(); i++)
			{
				m_popup.add( (JMenuItem) userDefined.elementAt(i));
			}
		}
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



	public interface ExitHandler
	{
		public void exited();
	}

	private class CascadeItemListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a_event)
		{
			MixCascade cascade = (MixCascade)m_menuItems.get(a_event.getSource());
			if (cascade != null)
			{
				JAPController.getInstance().setCurrentMixCascade(cascade);
				m_popup.setVisible(false);
			}
		}
	}
}

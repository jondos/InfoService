package gui;

import javax.swing.JComboBox;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import jap.JAPUtil;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import javax.swing.*;
import javax.swing.ListCellRenderer;
import jap.JAPConstants;
import jap.JAPMessages;
import anon.infoservice.MixCascade;

public class JAPMixCascadeComboBox extends JComboBox
{
	private final static String ITEM_AVAILABLE_CASCADES = "ITEM_AVAILABLE_CASCADES";
	private final static String ITEM_USER_CASCADES = "ITEM_USER_CASCADES";
	private final static String ITEM_NO_SERVERS_AVAILABLE = "ITEM_NO_SERVERS_AVAILABLE";

	class JAPMixCascadeComboBoxModel extends DefaultComboBoxModel
	{
		public void setSelectedItem(Object anObject)
		{
			if (anObject.equals(ITEM_AVAILABLE_CASCADES) ||
				anObject.equals(ITEM_USER_CASCADES)||
				anObject.equals(ITEM_NO_SERVERS_AVAILABLE))
			{
				return;
			}
			super.setSelectedItem(anObject);
		}
	}

	class JAPMixCascadeComboBoxListCellRender implements ListCellRenderer
	{
		private JLabel m_componentNoServer;
		private JLabel m_componentAvailableServer;
		private JPanel m_componentUserServer;
		private JLabel m_componentUserDefinedCascade;
		private JLabel m_componentAvailableCascade;
		public JAPMixCascadeComboBoxListCellRender()
		{
			m_componentNoServer = new JLabel(JAPMessages.getString("ngMixComboNoServers"));
			m_componentNoServer.setIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_ERROR, true));
			m_componentNoServer.setBorder(new EmptyBorder(2, 3, 2, 3));
			m_componentNoServer.setForeground(Color.red);

			m_componentAvailableServer = new JLabel(JAPMessages.getString("ngMixComboAvailableServers"));
			m_componentAvailableServer.setOpaque(true);
			m_componentAvailableServer.setHorizontalAlignment(JLabel.LEFT);
			m_componentAvailableServer.setBorder(new EmptyBorder(1, 3, 1, 3));

			m_componentUserServer = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			JLabel l = new JLabel(JAPMessages.getString("ngMixComboUserServers"));
			l.setBorder(new EmptyBorder(1, 3, 1, 3));
			l.setHorizontalAlignment(JLabel.LEFT);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.weightx = 1;
			c.gridy = 0;
			c.insets = new Insets(3, 0, 2, 0);
			m_componentUserServer.add(new JSeparator(), c);
			c.gridy = 1;
			c.insets = new Insets(0, 0, 0, 0);
			m_componentUserServer.add(new JSeparator(), c);
			c.gridy = 2;
			c.insets = new Insets(0, 0, 0, 0);
			m_componentUserServer.add(l, c);
			m_componentUserDefinedCascade = new JLabel(JAPUtil.loadImageIcon("servermanuell.gif", true));
			m_componentUserDefinedCascade.setOpaque(true);
			m_componentUserDefinedCascade.setHorizontalAlignment(JLabel.LEFT);
			m_componentUserDefinedCascade.setBorder(new EmptyBorder(1, 3, 1, 3));
			m_componentAvailableCascade = new JLabel(JAPUtil.loadImageIcon("serverfrominternet.gif", true));
			m_componentAvailableCascade.setHorizontalAlignment(JLabel.LEFT);
			m_componentAvailableCascade.setOpaque(true);
			m_componentAvailableCascade.setBorder(new EmptyBorder(1, 3, 1, 3));
		}

		public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			if (value == null)
			{
				return new JLabel();
			}
			if (value.equals(ITEM_AVAILABLE_CASCADES))
			{
				//m_componentAvailableServer.setFont(m_fontSmallFont);
				return m_componentAvailableServer;
			}
			else if (value.equals(ITEM_USER_CASCADES))
			{
				return m_componentUserServer;
			}
			else if (value.equals(ITEM_NO_SERVERS_AVAILABLE))
				return m_componentNoServer;
			MixCascade cascade = (MixCascade) value;
			JLabel l;
			if (cascade.isUserDefined())
			{
				l = this.m_componentUserDefinedCascade;
			}
			else
			{
				l = this.m_componentAvailableCascade;
			}
			l.setText(cascade.getName());
			if (isSelected)
			{
				l.setBackground(list.getSelectionBackground());
				l.setForeground(list.getSelectionForeground());
			}
			else
			{
				l.setBackground(list.getBackground());
				l.setForeground(list.getForeground());
			}
			return l;
		}
	}

	public JAPMixCascadeComboBox()
	{
		super();
		setModel(new JAPMixCascadeComboBoxModel());
		setRenderer(new JAPMixCascadeComboBoxListCellRender());
		super.addItem(ITEM_AVAILABLE_CASCADES);
	}

	public void addItem(Object o)
	{
	}

	public void addMixCascade(MixCascade cascade)
	{
		JAPMixCascadeComboBoxModel model = (JAPMixCascadeComboBoxModel) getModel();
		if (cascade.isUserDefined())
		{
			if (model.getIndexOf(ITEM_USER_CASCADES) < 0)
			{
				super.addItem(ITEM_USER_CASCADES);
			}
			super.addItem(cascade);
		}
		else
		{
			int index = model.getIndexOf(ITEM_USER_CASCADES);
			if (index < 0)
			{
				super.addItem(cascade);
			}
			else
			{
				super.insertItemAt(cascade, index);
			}
		}
	}

	public void removeAllItems()
	{
		super.removeAllItems();
		super.addItem(ITEM_AVAILABLE_CASCADES);
	}

	public void setNoDataAvailable()
	{
		super.insertItemAt(ITEM_NO_SERVERS_AVAILABLE,1);
	}
}

package gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import anon.crypto.JAPCertificate;
import jap.JAPConstants;
import jap.JAPUtil;

final public class CAListCellRenderer extends JLabel implements ListCellRenderer
{
	final static ImageIcon enabledIcon = JAPUtil.loadImageIcon(JAPConstants.CERTENABLEDICON, false);
	final static ImageIcon disabledIcon = JAPUtil.loadImageIcon(JAPConstants.CERTDISABLEDICON, false);

	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.

	public Component getListCellRendererComponent(
		JList list,
		Object value, // value to display
		int index, // cell index
		boolean isSelected, // is the cell selected
		boolean cellHasFocus) // the list and the cell have the focus
	{
		JAPCertificate j = (JAPCertificate) value;
		String issuerCN = (String) j.getIssuer().getValues().elementAt(0);
		setText(issuerCN);

//         setIcon((s.length() > 10) ? longIcon : shortIcon);
		if (isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		if (j.getEnabled())
		{
			setIcon(enabledIcon);
		}
		else
		{
			setForeground(Color.red);
			setIcon(disabledIcon);
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);
		return this;
	}
}

package gui;

import jap.JAPConstants;
import jap.JAPUtil;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import anon.crypto.CertificateInfoStructure;
import org.bouncycastle.asn1.x509.X509Name;
import java.util.Vector;

final public class CAListCellRenderer extends JLabel implements ListCellRenderer
{
	final static ImageIcon enabledIcon = JAPUtil.loadImageIcon(JAPConstants.CERTENABLEDICON, false);
	final static ImageIcon disabledIcon = JAPUtil.loadImageIcon(JAPConstants.CERTDISABLEDICON, false);

	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.

	public Component getListCellRendererComponent(
		JList list,
		Object value, // value to display
		int a_index, // cell index
		boolean isSelected, // is the cell selected
		boolean cellHasFocus) // the list and the cell have the focus
	{
		CertificateInfoStructure j = (CertificateInfoStructure) value;
		X509Name name = j.getCertificate().getSubject();
		Vector oids = name.getOIDs();
		int index = 0;
		boolean found = false;
		for (index = 0; index < oids.size(); index++)
		{
			if (oids.elementAt(index).equals(X509Name.CN))
			{
				found = true;
				break;
			}
		}
		String subjectCN;
		if (found)
		{
			subjectCN = (String) name.getValues().elementAt(index);
		}
		else
		{
			subjectCN = name.toString();
		}
		setText(subjectCN);

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
		if (j.isEnabled())
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

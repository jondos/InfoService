package gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import anon.crypto.CertificateInfoStructure;


public final class CertPathListCellRenderer extends JLabel implements ListCellRenderer
{

	private int m_itemcount = 0;
	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.

	public Component getListCellRendererComponent(
		JList list,
		Object value, // value to display
		int a_index, // cell index
		boolean isSelected, // is the cell selected
		boolean cellHasFocus) // the list and the cell have the focus
	{
		m_itemcount++;
		CertificateInfoStructure j = (CertificateInfoStructure) value;
		String subjectCN = j.getCertificate().getSubject().getCommonName();
		if (subjectCN == null)
		{
			subjectCN = j.getCertificate().getSubject().toString();
		}
		/*String s = new String();
		for(int i = 0; i < a_index; i++)
		{
			s += "     ";
		}
		setText(s+subjectCN);*/
		setText(subjectCN);
		setEnabled(list.isEnabled());

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
			if(j.getCertificate().getValidity().isValid(new Date()))
			{
				setIcon (GUIUtils.loadImageIcon(CertDetailsDialog.IMG_CERTENABLEDICON, false));
			}
			else
			{
				//setForeground(Color.orange);
				setIcon(GUIUtils.loadImageIcon(CertDetailsDialog.IMG_WARNING, false));
			}
		}
		else
		{
			setForeground(Color.red);
			setIcon(GUIUtils.loadImageIcon(CertDetailsDialog.IMG_CERTDISABLEDICON, false));
		}
		//if the element is the last element in the cert Path (the mix certificate) the text is bold
		if(j.equals(list.getModel().getElementAt((list.getModel().getSize())-1)))
		{
			setFont(new Font(this.getFont().getName(), Font.BOLD, this.getFont().getSize()));
		}
		else
		{
			setFont(list.getFont());
		}
	    setOpaque(true);
		return this;
	}
}

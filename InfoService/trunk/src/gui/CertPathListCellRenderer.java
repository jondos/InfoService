package gui;


import java.util.Date;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import anon.crypto.CertificateInfoStructure;


public final class CertPathListCellRenderer implements ListCellRenderer
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
		JPanel cell = new JPanel(new GridBagLayout());
		JLabel spaceLbl = new JLabel();
		JLabel certIconLabel = new JLabel();
		JLabel certTextLabel = new JLabel();
		GridBagConstraints constraints = new GridBagConstraints();
		char[] space;

		if (a_index > 0)
		{
			space = new char[a_index];
			for (int i = 0; i < space.length; i++)
			{
				space[i] = 'T';
			}
			spaceLbl.setText(new String(space));
		}

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;

		cell.add(spaceLbl, constraints);
		constraints.gridx++;
		cell.add(certIconLabel, constraints);
		constraints.gridx++;
		cell.add(certTextLabel, constraints);
		constraints.gridx++;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		cell.add(new JLabel(), constraints);

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
		certTextLabel.setText(subjectCN);
		certTextLabel.setEnabled(list.isEnabled());
		certIconLabel.setEnabled(list.isEnabled());
		spaceLbl.setEnabled(list.isEnabled());

		if (isSelected)
		{
			certTextLabel.setBackground(list.getSelectionBackground());
			certTextLabel.setForeground(list.getSelectionForeground());
			cell.setBackground(list.getSelectionBackground());
			cell.setForeground(list.getSelectionForeground());
			spaceLbl.setBackground(list.getSelectionBackground());
			spaceLbl.setForeground(list.getSelectionBackground());

		}
		else
		{
			certTextLabel.setBackground(list.getBackground());
			certTextLabel.setForeground(list.getForeground());
			cell.setBackground(list.getBackground());
			cell.setForeground(list.getForeground());
			spaceLbl.setBackground(list.getBackground());
			spaceLbl.setForeground(list.getBackground());
		}
		if (j.isEnabled())
		{
			if(j.getCertificate().getValidity().isValid(new Date()))
			{
				certIconLabel.setIcon (GUIUtils.loadImageIcon(CertDetailsDialog.IMG_CERTENABLEDICON, false));
			}
			else
			{
				//setForeground(Color.orange);
				certIconLabel.setIcon(GUIUtils.loadImageIcon(CertDetailsDialog.IMG_WARNING, false));
			}
		}
		else
		{
			certTextLabel.setForeground(Color.red);
			certIconLabel.setIcon(GUIUtils.loadImageIcon(CertDetailsDialog.IMG_CERTDISABLEDICON, false));
		}
		//if the element is the last element in the cert Path (the mix certificate) the text is bold
		if(j.equals(list.getModel().getElementAt((list.getModel().getSize())-1)))
		{
			certTextLabel.setFont(new Font(
						 certTextLabel.getFont().getName(), Font.BOLD, certTextLabel.getFont().getSize()));
		}
		else
		{
			certTextLabel.setFont(list.getFont());
		}
	    certTextLabel.setOpaque(true);
		return cell;
	}
}

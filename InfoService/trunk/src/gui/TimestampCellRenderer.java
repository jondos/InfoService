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
package gui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.sql.Timestamp;

/**
 * our own renderer for Date table cells. Shows dates in a not-so-ugly format.
 * @author Bastian Voigt
 */
public class TimestampCellRenderer extends JLabel implements TableCellRenderer
{
	public TimestampCellRenderer()
	{
		super();
		this.setOpaque(false); //MUST do this for background to show up.
		this.setHorizontalAlignment(SwingConstants.RIGHT);
//		this.setFont();
	}

	public Component getTableCellRendererComponent(
		JTable table, Object date,
		boolean isSelected, boolean hasFocus,
		int row, int column)
	{
		if(!(date instanceof Timestamp))
		{
			setText("Error - not a Timestamp!");
			return this;
		}
		Timestamp theDate = (Timestamp) date;
		this.setText(theDate.getDay() + "." + (theDate.getMonth() + 1) + "." + (theDate.getYear() + 1900));
		if (isSelected)
		{

		}
		return this;
	}
}

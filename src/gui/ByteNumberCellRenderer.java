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

/**
 * A table cell renderer for number-of-bytes values.
 * Renders KByte/MByte/GByte values in an easy human-readable format.
 * @author Bastian Voigt
 * @version 1.0
 */
public class ByteNumberCellRenderer extends JLabel implements TableCellRenderer
{
	public ByteNumberCellRenderer()
	{
		setOpaque(false);
		setHorizontalAlignment(SwingConstants.RIGHT);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		boolean hasFocus, int row, int column)
	{
		if(!(value instanceof Long))
		{
			setText("Error - no Long!");
			return this;
		}
		long l = ( (Long) value).longValue() * 100;
		int log = 1;
		while ( (l >= 102400) && (log <= 4))
		{
			l /= 1024;
			log++;
		}
		long fract = l % 100;
		long abs = l / 100;
		String unit;
		switch (log)
		{
			case 1:
				unit = " Bytes";
				break;
			case 2:
				unit = " KB";
				break;
			case 3:
				unit = " MB";
				break;
			case 4:
			default:
				unit = " GB";
				break;
		}
		setText(Long.toString(abs) + "." + Long.toString(fract) + unit);
		return this;
	}
}

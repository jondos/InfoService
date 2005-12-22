/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jap.pay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;

/**
 * This class is an extended progress bar that uses coin images for displaying
 * progress
 */
public class CoinstackProgressBar extends JProgressBar
{
	protected Image coinImage;
	protected int y_offset = 6;
	protected int x_offset = 6;
	protected int y_factor;
	protected int[] x_shift =
		{
		0, 3, 1, -2, -1, 0, -1, 0};
	protected int x_pos;
	protected int y_pos;
	protected int img_height;
	protected int img_width;
	protected int height;
	protected int width;

	public CoinstackProgressBar(ImageIcon imageIcon, int min, int max)
	{
		coinImage = imageIcon.getImage();
		prepareImage(coinImage, this);
		//check if image was loaded (necessary to determine correct height and width)
		while ( (checkImage(coinImage, this) & ALLBITS) != ALLBITS)
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException iex)
			{}
		}
		img_height = coinImage.getHeight(this);
		img_width = coinImage.getWidth(this);
		y_factor = img_height / 3;
		setMinimum(min);
		setMaximum(max);
		width = 2 * x_offset + img_width + 4 + 3;
		height = 2 * y_offset + img_height + y_factor * (max - min - 1);
	}

	public void paint(Graphics g)
	{
		//calculate height (necessary if setMaximum or setMinimum was called)
		height = 2 * y_offset + img_height + y_factor * (getMaximum() - getMinimum() - 1);
		//set color of lines
		g.setColor(Color.gray);
		//draw vertical line
		x_pos = x_offset;
		y_pos = height - y_offset;
		int y_pos_end = y_pos - (img_height + (y_factor) * (getMaximum() - getMinimum() - 1));
		g.drawLine(x_pos, y_pos, x_pos, y_pos_end);
		//draw horizontal lines
		int y_rule_middle = y_pos - (y_pos - y_pos_end) / 2;
		g.drawLine(x_pos, y_pos, x_pos + 3, y_pos);
		g.drawLine(x_pos, y_pos_end, x_pos + 3, y_pos_end);
		g.drawLine(x_pos, y_rule_middle, x_pos + 3, y_rule_middle);
		//no coin to draw if mimimum value
		if (getValue() == getMinimum())
		{
			return;
		}
		//draw coin
		int x_pos = x_offset + 4;
		int y_pos = height - y_offset - img_height + 1;
		for (int i = 0; i < (getValue() - getMinimum()); i++)
		{
			x_pos = x_pos + x_shift[i % x_shift.length];
			g.drawImage(coinImage, x_pos, y_pos, this);
			y_pos = y_pos - y_factor;
		}
	}

	public Dimension getPreferredSize()
	{
		return new Dimension(width, height);
	}
}

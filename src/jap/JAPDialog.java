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

package jap;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This is the generic implementation for a modal, user resizeable dialog. Use the root panel
 * (getRootPanel() method) for customization.
 */
public class JAPDialog
{

	/**
	 * Stores the instance of JDialog for internal use.
	 */
	private JDialog m_internalDialog;

	/**
	 * This stores the root panel of this dialog. All elements of the dialog are placed on this
	 * panel (or subpanels).
	 */
	private JPanel m_rootPanel;

	/**
	 * This stores the parent component of this dialog.
	 */
	private JComponent m_parentComponent;

	/**
	 * Creates a new instance of JAPDialog. It is user resizable and modal.
	 *
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_strTitle The title String for this dialog.
	 */
	public JAPDialog(JComponent a_parentComponent, String a_strTitle)
	{
		m_parentComponent = a_parentComponent;
		JOptionPane optionPane = new JOptionPane();
		m_internalDialog = optionPane.createDialog(a_parentComponent, a_strTitle);
		m_internalDialog.getContentPane().removeAll();
		m_rootPanel = new JPanel();
		m_internalDialog.getContentPane().add(m_rootPanel);
		m_internalDialog.setResizable(true);
	}

	/**
	 * Shows the dialog (set it to visible).
	 */
	public void show()
	{
		m_internalDialog.show();
	}

	/**
	 * Hides the dialog (set it to invisible).
	 */
	public void hide()
	{
		m_internalDialog.hide();
	}

	/**
	 * Set the dialog to the optimal size and center it over the parent component.
	 */
	public void align()
	{
		/* set the optimal size */
		m_internalDialog.pack();
		/* center the dialog over the parent component, tricky: for getting the absolut position
		 * values, we create a new Dialog (is centered over the parent) and use it for calculating
		 * our own location
		 */
		JOptionPane optionPane = new JOptionPane();
		JDialog dummyDialog = optionPane.createDialog(m_parentComponent, null);
		Rectangle dummyBounds = dummyDialog.getBounds();
		Dimension ownSize = m_internalDialog.getSize();
		Point ownLocation = new Point( (int) (Math.max(dummyBounds.x +
			( (dummyBounds.width - ownSize.width) / 2), 0)),
									  (int) (Math.max(dummyBounds.y +
			( (dummyBounds.height - ownSize.height) / 2), 0)));
		m_internalDialog.setLocation(ownLocation);
	}

	/**
	 * This returns the root panel for this dialog. Use this method for inserting elements on the
	 * root panel.
	 *
	 * @return The root panel of this dialog.
	 */
	public JPanel getRootPanel()
	{
		return m_rootPanel;
	}

	/**
	 * Disables the possibility of closing the dialog via the close-button in the dialog's
	 * title bar.
	 */
	public void disableManualClosing()
	{
		m_internalDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}

}

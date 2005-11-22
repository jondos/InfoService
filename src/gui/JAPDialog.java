/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.Rectangle;
import java.awt.Container;
import java.awt.event.WindowListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * This is the generic implementation for a modal, user resizeable dialog. Use the root panel
 * (getRootPanel() method) for customization.
 */
public class JAPDialog
{
	private boolean m_bIsDisplayable = true;

	/**
	 * Stores the instance of JDialog for internal use.
	 */
	private JDialog m_internalDialog;

	/**
	 * This stores the parent component of this dialog.
	 */
	private Component m_parentComponent;

	/**
	 * Creates a new instance of JAPDialog. It is user resizable and modal.
	 *
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_strTitle The title String for this dialog.
	 */
	public JAPDialog(Component a_parentComponent, String a_strTitle)
	{
		m_parentComponent = a_parentComponent;
		JOptionPane optionPane = new JOptionPane();
		m_internalDialog = optionPane.createDialog(a_parentComponent, a_strTitle);
		m_internalDialog.getContentPane().removeAll();
		m_internalDialog.setResizable(true);
	}

	public final Container getContentPane()
	{
		return m_internalDialog.getContentPane();
	}

	/**
	 * Shows or hides the dialog. If shown, the dialog is centered over the parent component.
	 * Subclasses may override this method to change this centering behaviour.
	 * @param a_bVisible 'true' shows the dialog; 'false' hides it
	 */
	public void setVisible(boolean a_bVisible)
	{
		setVisible(a_bVisible, true);
	}

	/**
	 * Shows or hides the dialog.
	 * @param a_bVisible 'true' shows the dialog; 'false' hides it
	 * @param a_bCenterOnParentComponent if true, the dialog is centered on the parent component;
	 * otherwise, it is centered on the parent window
	 */
	public final void setVisible(boolean a_bVisible, boolean a_bCenterOnParentComponent)
	{
		if (a_bVisible)
		{
			if (!m_bIsDisplayable)
			{
				throw new RuntimeException("Dialog has been disposed and cannot be made visible!");
			}
			else
			{
				align(a_bCenterOnParentComponent);
			}
		}
		m_internalDialog.setVisible(a_bVisible);
	}

	/**
	 * Disposes the dialog (set it to invisible and releases all resources).
	 */
	public final void dispose()
	{
		m_internalDialog.dispose();
	}

	/**
	 * Returns the size of the dialog window.
	 * @return the size of the dialog window
	 */
	public final Dimension getSize()
	{
		return m_internalDialog.getSize();
	}

	/**
	 * Sets the size of the dialog window.
	 * @param a_width the new window width
	 * @param a_height the new window height
	 */
	public final void setSize(int a_width, int a_height)
		{
			m_internalDialog.setSize(a_width, a_height);
	}

	/**
	 * Allows to set the dialog resizable or fixed-sized.
	 * @param a_bResizable true if the dialog should become resizable; false otherwise
	 */
	public void setResizable(boolean a_bResizable)
	{
		m_internalDialog.setResizable(a_bResizable);
	}

	/**
	 * Returns the dialog's location on the screen.
	 * @return the dialog's location on the screen
	 */
	public final Point getLocation()
	{
		return m_internalDialog.getLocation();
	}

	/**
	 * Defines the reaction of this dialog on a klick on the close button in the dialog's title bar.
	 * @param a_windowAction insert an element of javax.swing.WindowConstants
	 * @see javax.swing.WindowConstants
	 */
	public final void setDefaultCloseOperation(int a_windowAction)
	{
		m_internalDialog.setDefaultCloseOperation(a_windowAction);
	}

	/**
	 * Adds a WindowListener to the dialog.
	 * @param a_listener a WindowListener
	 * @see java.awt.event.WindowListener
	 */
	public final void addWindowListener(WindowListener a_listener)
	{
		m_internalDialog.addWindowListener(a_listener);
	}

	/**
	 * Removes a specific WindowListener from the dialog.
	 * @param a_listener a WindowListener
	 * @see java.awt.event.WindowListener
	 */
	public final void removeWindowListener(WindowListener a_listener)
	{
		m_internalDialog.removeWindowListener(a_listener);
	}

	/**
	 * Sets the dialog to the optimal size.
	 */
	public final void pack()
	{
		m_internalDialog.pack();
	}

	/**
	 * Centers the dialog over the parent component or the parent window.
	 * @param a_bCenterOnParentComponent if true, the dialog is centered on the parent component;
	 * otherwise, it is centered on the parent window
	 */
	private void align(boolean a_bCenterOnParentComponent)
	{
		if (!a_bCenterOnParentComponent)
		{
			alignOnWindow();
		}
		else
		{
			alignOnComponent();
		}
	}

	/**
	 * Centers the dialog over the parent component.
	 */
	private void alignOnComponent()
	{
		/* center the dialog over the parent component, tricky: for getting the absolut position
		 * values, we create a new Dialog (is centered over the parent) and use it for calculating
		 * our own location
		 */
		JOptionPane optionPane = new JOptionPane();
		JDialog dummyDialog = optionPane.createDialog(m_parentComponent, null);
		Rectangle dummyBounds = dummyDialog.getBounds();
		Dimension ownSize = m_internalDialog.getSize();
		Point ownLocation = new Point( (Math.max(dummyBounds.x +
												 ( (dummyBounds.width - ownSize.width) / 2), 0)),
									  (Math.max(dummyBounds.y +
												( (dummyBounds.height - ownSize.height) / 2), 0)));
		m_internalDialog.setLocation(ownLocation);
	}

	/**
	 * Centers the dialog over the parent window.
	 */
	private void alignOnWindow()
	{
		Component parent = m_parentComponent;
		// find the first parent that is a window
		while (parent != null && ! (parent instanceof Window))
		{
			parent = parent.getParent();
		}
		GUIUtils.positionWindow(m_internalDialog, (Window) parent);
	}

}

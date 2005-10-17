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

import gui.SwingWorker;
import gui.BusyWindow;
import java.awt.Component;
import anon.pay.PayAccountsFile;
import anon.pay.BI;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JOptionPane;
import jap.*;

/** This class creates a payment account and shows a busy window meanwhile
 *  @author Tobias Bayer
 */
public class AccountCreator extends SwingWorker
{
	private BI m_bi;
	private BusyWindow m_bw;
	private Vector m_changeListeners;
	private Component m_parent;

	public AccountCreator(BI a_bi, Component a_parent)
	{
		m_bi = a_bi;
		m_changeListeners = new Vector();
		m_parent = a_parent;
		if (m_parent != null)
		{
			m_bw = new BusyWindow(m_parent, JAPMessages.getString("creatingAccount"));
			m_bw.setSwingWorker(this);
		}
	}

	/**
	 * Compute the value to be returned by the <code>get</code> method.
	 *
	 * @return Object
	 */
	public Object construct()
	{
		try
		{
			PayAccountsFile.getInstance().createAccount(m_bi, true, JAPConstants.PI_SSLON);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			JOptionPane.showMessageDialog(
				m_parent,
				JAPMessages.getString("Error creating account: ") + e,
				JAPMessages.getString("error"), JOptionPane.ERROR_MESSAGE
				);
		}

		return null;
	}

	public void finished()
	{
		fireStateChanged();
		if (m_bw != null)
		{
			m_bw.dispose();
		}
	}

	protected void fireStateChanged()
	{
		Object cl;
		Enumeration e = m_changeListeners.elements();
		while (e.hasMoreElements())
		{
			cl = e.nextElement();
			( (ChangeListener) cl).stateChanged(new ChangeEvent(this));
		}
	}

	public void addChangeListener(ChangeListener a_cl)
	{
		m_changeListeners.addElement(a_cl);
	}

}

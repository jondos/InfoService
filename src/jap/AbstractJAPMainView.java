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

import javax.swing.JFrame;
import gui.JAPDll;
import javax.swing.SwingUtilities;
import java.lang.reflect.*;
public abstract class AbstractJAPMainView extends JFrame implements IJAPMainView
{
	protected String m_Title;
	protected MyViewUpdate m_runnableValueUpdate;
	protected JAPController m_Controller;

	final private class MyViewUpdate implements Runnable
	{
		public void run()
		{
			updateValues();
		}
	}

	public AbstractJAPMainView(String s,JAPController a_controller)
	{
		super(s);
		m_Controller=a_controller;
		m_Title=s;
		m_runnableValueUpdate=new MyViewUpdate();
	}
	protected void exitProgram()
	{
		JAPController.goodBye(true); // call the final exit procedure of JAP
	}

	public void hideWindowInTaskbar()
{
	synchronized (m_runnableValueUpdate) //updateValues may change the Titel of the Window!!
	{
		setTitle(Double.toString(Math.random())); //ensure that we have an uinque title
		JAPDll.hideWindowInTaskbar(getTitle());
		setTitle(m_Title);
	}
}
public void valuesChanged(boolean bSync)
{
//	synchronized (m_runnableValueUpdate)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			updateValues();
		}
		else
		{
			if(bSync)
				try
				{
					SwingUtilities.invokeAndWait(m_runnableValueUpdate);
				}
				catch (InvocationTargetException ex)
				{
				}
				catch (InterruptedException ex)
				{
				}
			else
				SwingUtilities.invokeLater(m_runnableValueUpdate);
		}
	}
}

private void updateValues()
{
	synchronized (m_runnableValueUpdate)
	{
		doSynchronizedUpdateValues();
	}

}}

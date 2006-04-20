/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package gui.dialog;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JLabel;

import gui.GUIUtils;

/**
 * This is a dialog that executes a given Thread or Runnable if it is shown on screen. It has an optional
 * cancel button. A click on the dialog close button or on the cancel button will interrupt the Thread.
 * The Thread may watch for this event but does not need to. The default behaviour is that, if the Thread
 * is interrupted and has finished, the previous content pane is shown. If it is not interrupted and has
 * finished, the next content pane is shown.
 * <P>Warning: The Thread should not call dispose() on the dialog, as this may lead to a deadlock or
 * an Exception!</P>
 *
 * @author Rolf Wendolsky
 */
public class WorkerContentPane extends DialogContentPane implements
	DialogContentPane.IWizardSuitableNoWizardButtons
{
	/** @todo rename the image according to coding standards */
	public static final String IMG_BUSY = "busy.gif";

	private Thread m_workerThread;
	private Runnable m_workerRunnable;
	private Thread m_internalThread;
	private boolean m_bInterruptThreadSafe = true;

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, "", null, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, String a_strTitle,
							 Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, a_strTitle, null, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText,
							 DialogContentPane a_previousContentPane, Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, "", a_previousContentPane, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, String a_strTitle,
							 DialogContentPane a_previousContentPane, Runnable a_workerRunnable)
	{
		super(a_parentDialog, a_strText, new Layout(a_strTitle),
			  new Options(OPTION_TYPE_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG);
		m_workerRunnable = a_workerRunnable;

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JLabel(GUIUtils.loadImageIcon(IMG_BUSY, true)), BorderLayout.CENTER);

		addComponentListener(new WorkerComponentListener());
	}

	/**
	 * Returns true if the content pane only does a close or move operation if the thread has stopped.
	 * Otherwise, the close or move operation is performed if the thread has been interrupted by closing the
	 * dialog window or clicking on the cancel button. If you want this, the calling thread is recommended to
	 * call <CODE> joinThread() </CODE> to safely wait for the end of the thread before starting a new one.
	 * Remember that a call to updateDialog() of this content pane gives an undefined result if the previously
	 * startet thread has not stopped yet.
	 * @return true if the content pane only does a close or move operation if thetThread has stopped.
	 * Otherwise, the close or move operation is performed if the thread has been interrupted by closing the
	 * dialog window or clicking on the cancel button
	 */
	public final boolean isInterruptThreadSafe()
	{
		return m_bInterruptThreadSafe;
	}

	/**
	 * Defines this content pane should interrupt the wrapped thread safe or not if the user closed the parent
	 * dialog or clicked cancel.
	 * @param a_bInterruptThreadSafe true if the content pane only does a close or move operation if the
	 * thread has stopped. Otherwise, the close or move operation is performed if the thread has been
	 * interrupted by closing the dialog window or clicking on the cancel button. If you want this, the
	 * calling thread is recommended to call <CODE> joinThread() </CODE> to safely wait for the end of the
	 * thread before starting a new one. Remember that a call to updateDialog() of this content pane
	 * gives an undefined result if the previously started thread has not stopped yet.
	 */
	public final void setInterruptThreadSafe(boolean a_bInterruptThreadSafe)
	{
		m_bInterruptThreadSafe = a_bInterruptThreadSafe;
	}

	/**
	 * The caller waits until the thread has stopped. This is only needed if isInterruptThreadSafe()
	 * returns false and the user closes the dialog or clicks cancel, otherwise this is done automatically.
	 * Remember that a new thread started by this content pane is blocked until the old thread has stopped
	 * and, if startet, it will overwrite the getValue() result of the old thread.
	 */
	public final void joinThread()
	{
		try
		{
			if (m_workerThread != null)
			{
				m_workerThread.join();
			}
		}
		catch (InterruptedException a_e)
		{
		}
	}

	/**
	 * Returns if the content pane is ready to start a new thread. If it is not ready, no thread will
	 * be started when the content pane shows up. Subclasses may override this method to hide and show
	 * this content pane while a thread is running. Please note that this method should always return
	 * <CODE> true </CODE> after the thread has stopped.
	 * @return if the content pane is ready to start a new thread; true by default
	 */
	public boolean isReady()
	{
		return true;
	}

	/**
	 * Returns true by default, that means this worker content pane is skipped if a move from the next
	 * content pane to this one is done.
	 * @return true
	 */
	public boolean isSkippedAsPreviousContentPane()
	{
		return true;
	}

	/**
	 * Interrupts the Thread.
	 * @return CheckError[]
	 */
	public CheckError[] checkCancel()
	{
		interruptWorkerThread();
		return null;
	}

	/**
	 * Interrupts the thread.
	 */
	public void interruptWorkerThread()
	{
		if (m_workerThread != null)
		{
			if (!m_workerThread.isInterrupted())
			{
				m_workerThread.interrupt();
			}
			if (isInterruptThreadSafe())
			{
				joinThread();
			}
		}
	}

	/**
	 * A wrapper for the worker runnable.
	 */
	private class InternalThread extends Thread
	{
		private Thread m_thread;

		public InternalThread(Runnable a_runnable)
		{
			super(a_runnable,"WorkerContentPane - InternalThread");
			if (a_runnable instanceof Thread)
			{
				m_thread = (Thread) a_runnable;
			}
		}

		public void interrupt()
		{
			if (m_thread != null)
			{
				m_thread.interrupt();
			}
			super.interrupt();
		}

		public boolean isInterrupted()
		{
			boolean bInterrupted = super.isInterrupted();

			if (m_thread != null)
			{
				bInterrupted = bInterrupted || m_thread.isInterrupted();
			}

			return bInterrupted;
		}
	}

	private class WorkerComponentListener extends ComponentAdapter implements Runnable
	{
		public void componentHidden(ComponentEvent a_event)
		{
			if (isReady())
			{
				interruptWorkerThread();
			}
		}

		/**
		 * Starts the internal Thread if the content pane becomes visible.
		 * If the Thread starts, the return value is reset to RETURN_VALUE_UNINITIALIZED. If the
		 * Thread is finished without interruption, the return value is set to RETURN_VALUE_OK.
		 * @param a_event ComponentEvent
		 */
		public void componentShown(ComponentEvent a_event)
		{
			if (isVisible() && isReady())
			{
				m_internalThread = new Thread(this,"WorkerContentPane - componentShown()");
				m_internalThread.setDaemon(true);
				m_internalThread.start();
			}
		}

		public synchronized void run()
		{
			setButtonValue(RETURN_VALUE_UNINITIALIZED);

			m_workerThread = new InternalThread(m_workerRunnable);
			try
			{
				// give the GUI some extra time to show up
				Thread.sleep(200);
			}
			catch (InterruptedException a_e)
			{
				interruptWorkerThread();
				m_workerThread = null;
				moveToPreviousContentPane();
				notifyAll();
				return;
			}
			m_workerThread.setDaemon(true);
			m_workerThread.start();

			try
			{
				m_workerThread.join();
			}
			catch (InterruptedException a_e)
			{
				interruptWorkerThread();
			}

			if (m_workerThread.isInterrupted())
			{
				// don't do anything if the cancel (or any other) button was clicked
				if (getButtonValue() == RETURN_VALUE_UNINITIALIZED)
				{
					if ( (getDefaultButtonOperation() &
						  (ON_CLICK_DISPOSE_DIALOG | ON_CANCEL_DISPOSE_DIALOG)) > 0)
					{
						closeDialog(true);
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_HIDE_DIALOG | ON_CANCEL_HIDE_DIALOG)) > 0)
					{
						closeDialog(false);
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_SHOW_PREVIOUS_CONTENT | ON_CANCEL_SHOW_PREVIOUS_CONTENT)) > 0)
					{
						moveToPreviousContentPane();
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_SHOW_NEXT_CONTENT | ON_CANCEL_SHOW_NEXT_CONTENT)) > 0)
					{
						moveToNextContentPane();
					}
				}
			}
			else
			{
				interruptWorkerThread();
				setButtonValue(RETURN_VALUE_OK);
				moveToNextContentPane();
			}

			m_workerThread = null;
			notifyAll();
		}
	}

	public Object getValue()
	{
		if (m_workerRunnable instanceof ReturnThread)
		{
			return ( (ReturnThread) m_workerRunnable).getValue();
		}
		else
		{
			return null;
		}
	}

	public static abstract class ReturnThread extends Thread
	{
		public abstract Object getValue();
	}

}

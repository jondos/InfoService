package jap;

import javax.swing.JFrame;
import gui.JAPDll;
import javax.swing.SwingUtilities;
public abstract class AbstractJAPMainView extends JFrame implements IJAPMainView
{
	protected String m_Title;
	protected MyViewUpdate m_runnableValueUpdate;

	final private class MyViewUpdate implements Runnable
	{
		public void run()
		{
			updateValues();
		}
	}

	public AbstractJAPMainView(String s)
	{
		super(s);
		m_Title=s;
		m_runnableValueUpdate=new MyViewUpdate();
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
public void valuesChanged()
{
	synchronized (m_runnableValueUpdate)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			updateValues();
		}
		else
		{
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

package jap;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

abstract class AbstractJAPSetServerModeAsync implements Runnable
{
	private static Object oSyncObject = new Object(); //for synchronisation of setMode(true/false)
	private static Object oSetThreadIDSyncObject = new Object(); //for synchronisation of setMode(true/false)
	private static int ms_ModeAsyncLastStarted = -1;
	private static int ms_ModeAsyncLastFinished = -1;

	private boolean ServerModeSelected = false;
	private int id = 0;

	public AbstractJAPSetServerModeAsync(boolean b)
	{
		synchronized (oSetThreadIDSyncObject)
		{
			ServerModeSelected = b;
			ms_ModeAsyncLastStarted++;
			id = ms_ModeAsyncLastStarted;

			new Thread(this, this.getClass().getName()).start();
		}
	}

	/** @todo Still very bugy, because mode change is async done but not
	 * all properties (like currentMixCascade etc.)are synchronized!!
	 *
	 */

	public void run()
	{
		synchronized (oSyncObject)
		{
			while (id != ms_ModeAsyncLastFinished + 1)
			{
				try
				{
					oSyncObject.wait();
				}
				catch (InterruptedException ieo)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.THREAD,
								  "Waiting for becoming current SetServerModeAsnyc Thread intterrupted!");
				}
			}
			try
			{
				setServerMode(ServerModeSelected);
			}
			catch (Throwable t)
			{
			}
			ms_ModeAsyncLastFinished++;
			oSyncObject.notifyAll();
		}
	}

	/** Overridew this. It is called from a thread to enable/disable something. The class ensures, that
	 * this method is called in the order in which the class was created.*/
	abstract void setServerMode(boolean b);

} //end of class SetAnonModeAsync

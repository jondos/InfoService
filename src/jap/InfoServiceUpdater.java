/*
 Copyright (c) 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in bisnary form must reproduce the above copyright notice,
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

import java.util.Enumeration;
import java.util.Vector;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 *
 *
 * @author Rolf Wendolsky
 */
public class InfoServiceUpdater
{
	private Thread m_updateThread;
	private boolean m_successfulUpdate = false;

	public InfoServiceUpdater()
	{
		m_updateThread = new Thread(new Runnable()
		{
			public void run()
			{
				while (!Thread.currentThread().isInterrupted())
				{
					synchronized (Thread.currentThread())
					{
						try
						{
							Thread.currentThread().notify();
							if (JAPModel.getInstance().isInfoServiceDisabled())
							{
								Thread.currentThread().wait();
							}
							else
							{
								Thread.currentThread().wait(60000);
							}
						}

						catch (InterruptedException a_e)
						{
							Thread.currentThread().notifyAll();
							break;
						}
						if (Thread.currentThread().isInterrupted())
						{
							Thread.currentThread().notifyAll();
							break;
						}
					}

					LogHolder.log(LogLevel.INFO, LogType.THREAD, "Updating InfoService list.");

					synchronized (InfoServiceHolder.getInstance())
					{
						Vector downloadedInfoServices = InfoServiceHolder.getInstance().getInfoServices();
						if (downloadedInfoServices == null)
						{
							LogHolder.log(LogLevel.ERR, LogType.THREAD, "InfoService update failed!");
							m_successfulUpdate = false;

						}
						else
						{
							LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
										  "InfoService update was successful.");
							m_successfulUpdate = true;
							/* we have successfully downloaded the list of running infoservices -> update the
							 * internal database of known infoservices
							 */
							Enumeration infoservices = downloadedInfoServices.elements();
							while (infoservices.hasMoreElements())
							{
								InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (
									infoservices.nextElement());
								Database.getInstance(InfoServiceDBEntry.class).update(currentInfoService);
								InfoServiceDBEntry preferredInfoService = InfoServiceHolder.getInstance().
									getPreferredInfoService();
								if (preferredInfoService != null)
								{
									/* if the current infoservice is equal to the preferred infoservice, update the
									 * preferred infoservice also
									 */
									if (preferredInfoService.equals(currentInfoService))
									{
										InfoServiceHolder.getInstance().setPreferredInfoService(
											currentInfoService);
									}
								}
							}
							/* now remove all non user-defined infoservices, which were not updated, from the
							 * database of known infoservices
							 */
							Enumeration knownInfoServices = Database.getInstance(InfoServiceDBEntry.class).
								getEntryList().elements();
							while (knownInfoServices.hasMoreElements())
							{
								InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (
									knownInfoServices.nextElement());
								if (!currentInfoService.isUserDefined() &&
									!downloadedInfoServices.contains(currentInfoService))
								{
									/* the InfoService was fetched from the Internet earlier, but it is not in the list
									 * fetched from the Internet this time -> remove that InfoService from the database
									 * of known InfoServices
									 */
									Database.getInstance(InfoServiceDBEntry.class).remove(
										currentInfoService);
								}
							}
						}
					}
				}
				LogHolder.log(LogLevel.INFO, LogType.THREAD, "InfoService update thread stopped.");
			}
		}, "InfoService Update Thread");
		m_updateThread.setDaemon(true);
		m_updateThread.start();
	}


	public synchronized boolean update()
	{
		synchronized (m_updateThread)
		{
			m_updateThread.notify();
			try
			{
				m_updateThread.wait();
			}
			catch (InterruptedException a_e)
			{
				return false;
			}
			return m_successfulUpdate;
		}
	}

	public void stop()
	{
		while (m_updateThread.isAlive())
		{
			synchronized (m_updateThread)
			{
				m_updateThread.notifyAll();
				m_updateThread.interrupt();
			}
			try
			{
				m_updateThread.join();
			}
			catch (InterruptedException a_e)
			{
				// ignore
			}
		}

	}


}

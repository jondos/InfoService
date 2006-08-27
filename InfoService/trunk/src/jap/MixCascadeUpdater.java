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

import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Updates the list of available MixCascades.
 * @author Rolf Wendolsky
 */
public class MixCascadeUpdater extends AbstractDatabaseUpdater
{
	private static final int UPDATE_INTERVAL_MS = 5 * 60000;

	public MixCascadeUpdater()
	{
		super(new ConstantUpdateInterval(UPDATE_INTERVAL_MS));
	}

	public Class getUpdatedClass()
	{
		return MixCascade.class;
	}

	protected AbstractDatabaseEntry getPreferredEntry()
	{
		return JAPController.getInstance().getCurrentMixCascade();
	}

	protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
	{
		if (a_preferredEntry instanceof MixCascade)
		{
			JAPController.getInstance().setCurrentMixCascade((MixCascade)a_preferredEntry);
		}
	}

	/**
	 * Removes all MixInfo entries that exist without a cascade.
	 */
	protected boolean doCleanup(Hashtable a_newEntries)
	{
		boolean bUpdated = super.doCleanup(a_newEntries);

		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Do MixInfo database cleanup.");

		Vector mixes = Database.getInstance(MixInfo.class).getEntryList();
		Vector cascades = Database.getInstance(MixCascade.class).getEntryList();
		// do not remove mixes of current cascade
		cascades.addElement(JAPController.getInstance().getCurrentMixCascade());
		MixInfo currentMix;
		Vector currentCascadeMixes;

		loop:
		for (int i = 0; i < mixes.size(); i++)
		{
			currentMix = (MixInfo)mixes.elementAt(i);
			if (Database.getInstance(MixCascade.class).getEntryById(currentMix.getId()) != null)
			{
				continue;
			}
			for (int j = 0; j < cascades.size(); j++)
			{
				currentCascadeMixes = ((MixCascade)cascades.elementAt(j)).getMixIds();
				for (int k = 1; k < currentCascadeMixes.size(); k++)
				{
					if (currentCascadeMixes.elementAt(k).equals(currentMix.getId()))
					{
						continue loop;
					}
				}
			}
			Database.getInstance(MixInfo.class).remove(currentMix);
			LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Cleaned MixInfo DB entry: " + currentMix.getId());
		}

		return bUpdated;
	}

	protected Hashtable getUpdatedEntries()
	{
		return  InfoServiceHolder.getInstance().getMixCascades();
	}

}

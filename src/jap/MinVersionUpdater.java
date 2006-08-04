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

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.Database;
import java.util.Hashtable;
import anon.infoservice.JAPMinVersion;

/**
 *
 * @author Rolf Wendolsky
 */
public class MinVersionUpdater extends AbstractDatabaseUpdater
{
	private static final int UPDATE_INTERVAL_MS = 1000 * 60 * 60 * 24; // one day (update once per day)
	private static final int UPDATE_INTERVAL_MS_SHORT = 1000 * 60 * 8; // 8 minutes

	public MinVersionUpdater()
	{
		super(new DynamicUpdateInterval());
	}

	public Class getUpdatedClass()
	{
		return JAPMinVersion.class;
	}

	protected Hashtable getUpdatedEntries()
	{
		Hashtable hashtable = new Hashtable();
		JAPMinVersion version = InfoServiceHolder.getInstance().getNewVersionNumber();
		if (version != null)
		{
			hashtable.put(version.getId(), version);
		}
		return  hashtable;
	}

	private static class DynamicUpdateInterval implements IUpdateInterval
	{
		public int getUpdateInterval()
		{
			if (Database.getInstance(JAPMinVersion.class).getNumberofEntries() > 0)
			{
				return UPDATE_INTERVAL_MS;
			}
			else
			{
				return UPDATE_INTERVAL_MS_SHORT;
			}
		}
	}
}

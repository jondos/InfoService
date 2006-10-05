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
package infoservice;

import java.util.Vector;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation of the announcment of our own infoservice. It generates
 * periodicly (default: 10 minutes) a new InfoServiceDBEntry for our own infoservices and
 * propagates it to all other infoservices (via InfoServiceDatabase.update()).
 */
public class InfoServicePropagandist implements Runnable
{
	private static long ms_serialNumber;

  /**
   * Stores whether ther is already an instance of InfoServicePropagandist running or not.
   */
  private static boolean alreadyRunning = false;

  /**
   * Generates an instance of InfoServicePropagandist if there isn't already one running.
   */
  public static void generateInfoServicePropagandist()
  {
    if (alreadyRunning == false)
    {
      alreadyRunning = true;
	  ms_serialNumber = System.currentTimeMillis();
      InfoServicePropagandist propaganda = new InfoServicePropagandist();
      Thread propagandist = new Thread(propaganda);
      propagandist.start();
    }
  }

  /**
   * This is the propaganda thread, which propagates our infoservice to all neighbour
   * infoservices every Constants.ANNOUNCE_PERIOD (default: 10 minutes).
   */
  public void run()
  {
    while (true)
    {
      Vector virtualListeners = Configuration.getInstance().getVirtualListeners();
      if (virtualListeners.size() > 0)
      {
        InfoServiceDBEntry generatedOwnEntry =
			new InfoServiceDBEntry(Configuration.getInstance().getOwnName(),
								   Configuration.getInstance().getID(),
			virtualListeners, Configuration.getInstance().holdForwarderList(), false,
			System.currentTimeMillis(), ms_serialNumber);
        /* put the own entry in the database -> it is forwarded automatically to all neighbour
         * infoservices, which are also in the database
         */
        Database.getInstance(InfoServiceDBEntry.class).update(generatedOwnEntry);
        /* send it also to all initial neighbour infoservices -> they will always find us, after
         * they come up
         */
        InfoServiceDistributor.getInstance().addJobToInititalNeighboursQueue(generatedOwnEntry);
        LogHolder.log(LogLevel.DEBUG, LogType.MISC,
                "Updating and propagating own InfoServerDBEntry.");
      }
      else
      {
        /* we need a listener-interface */
        LogHolder.log(LogLevel.EMERG, LogType.MISC,
          "There is no virtual listener interface configurated. Shutdown InfoService!");
        System.out.println(
          "There is no virtual listener interface configurated. Shutdown InfoService!");
        System.exit( -1);
      }
      /* sleep for one announce period and then announce again */
      try
      {
        Thread.sleep(Constants.ANNOUNCE_PERIOD);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

}

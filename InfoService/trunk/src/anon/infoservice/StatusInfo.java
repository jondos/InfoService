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
package anon.infoservice;

import org.w3c.dom.Element;

/**
 * Holds the information of a mixcascade status.
 */
public class StatusInfo
{

	/**
	 * This is the ID of the mixcascade to which this status belongs.
	 */
	private String mixCascadeId;

	/**
	 * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this status
	 * message.
	 */
	private long lastUpdate;

	/**
	 * Stores the number of active users in the corresponding mixcascade.
	 */
	private int nrOfActiveUsers;

	/**
	 * Stores the current risk for using this mix cascade. This is a value between 0 an 100 and it
	 * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
	 * client.
	 */
	private int currentRisk;

	/**
	 * Stores the current traffic situation for the mixcascade.
	 */
	private int trafficSituation;

	/**
	 * This is the number of packets, which are mixed through the cascade since their startup.
	 */
	private long mixedPackets;

	/**
	 * This is the calculated anonymity level (from number of active users, current traffic
	 * and cascade length). It is a value between 0 and 5.
	 */
	private int anonLevel;

	/**
	 * Returns a new StatusInfo with dummy values (everything is set to -1). The LastUpdate time is
	 * set to the current system time. This function is used every time, we can't get the StatusInfo
	 * from the infoservice of when a new MixCascade is constructed.
	 *
	 * @param mixCascadeId The ID of the MixCascade the StatusInfo belongs to.
	 *
	 * @return The new dummy StatusInfo.
	 */
	public static StatusInfo createDummyStatusInfo(String mixCascadeId)
	{
		return (new StatusInfo(mixCascadeId, System.currentTimeMillis(), -1, -1, -1, -1, -1));
	}

	/**
	 * Creates a new StatusInfo from XML description (MixCascadeStatus node).
	 *
	 * @param statusNode The MixCascadeStatus node from an XML document.
	 * @param mixCascadeLength The number of mixes in the mixcascade. We need this for
	 *                         calculating the anonymity level.
	 */
	public StatusInfo(Element statusNode, int mixCascadeLength) throws Exception
	{
		/* get all the attributes of MixCascadeStatus */
		mixCascadeId = statusNode.getAttribute("id");
		/* get the values */
		currentRisk = Integer.parseInt(statusNode.getAttribute("currentRisk"));
		mixedPackets = Long.parseLong(statusNode.getAttribute("mixedPackets"));
		nrOfActiveUsers = Integer.parseInt(statusNode.getAttribute("nrOfActiveUsers"));
		trafficSituation = Integer.parseInt(statusNode.getAttribute("trafficSituation"));
		lastUpdate = Long.parseLong(statusNode.getAttribute("LastUpdate"));
		/* calculate then anonymity level */
		anonLevel = -1;
		if ( (mixCascadeLength >= 0) && (nrOfActiveUsers >= 0) && (trafficSituation >= 0))
		{
			double userFactor = Math.min( ( (double) nrOfActiveUsers) / 500.0, 1.0);
			double trafficFactor = Math.min( ( (double) trafficSituation) / 100.0, 1.0);
			double mixFactor = 1.0 - Math.pow(0.5, mixCascadeLength);
			/* get the integer part of the product -> 0 <= anonLevel <= 5 because mixFactor is always < 1.0 */
			anonLevel = (int) (userFactor * trafficFactor * mixFactor * 6.0);
		}
	}

	/**
	 * Constructs a StatusInfo out of the single values.
	 *
	 * @param mixCascadeId The ID of the mixcascade this StatusInfo belongs to.
	 * @param lastUpdate The time this StatusInfo was created.
	 * @param nrOfActiveUsers The number of active users in the cascade.
	 * @param currentRisk The risk calculated by the cascade (between 0 and 100).
	 * @param trafficSituation The amount of traffic in the cascade.
	 * @param mixedPackets The number of packets the cascade has mixed since startup.
	 * @param anonLevel The anonymity level calculated by the JAP client (between 0 and 5).
	 */
	public StatusInfo(String mixCascadeId, long lastUpdate, int nrOfActiveUsers, int currentRisk,
					  int trafficSituation, long mixedPackets, int anonLevel)
	{
		this.mixCascadeId = mixCascadeId;
		this.lastUpdate = lastUpdate;
		this.nrOfActiveUsers = nrOfActiveUsers;
		this.currentRisk = currentRisk;
		this.trafficSituation = trafficSituation;
		this.mixedPackets = mixedPackets;
		this.anonLevel = anonLevel;
	}

	/**
	 * Returns the mixcascade ID of this status.
	 *
	 * @return The mixcascade ID of this status.
	 */
	public String getId()
	{
		return mixCascadeId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the mixcascade has sent this
	 * StatusInfo to an InfoService.
	 *
	 * @return The send time of this StatusInfo from the mixcascade.
	 *
	 */
	public long getLastUpdate()
	{
		return lastUpdate;
	}

	/**
	 * Returns the number of active users in the corresponding mixcascade.
	 *
	 * @return The number of active users in the corresponding mixcascade.
	 */
	public int getNrOfActiveUsers()
	{
		return nrOfActiveUsers;
	}

	/**
	 * Returns the current risk for using this mix cascade. This is a value between 0 an 100 and it
	 * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
	 * client.
	 *
	 * @return The current risk for the mixcascade.
	 */
	public int getCurrentRisk()
	{
		return currentRisk;
	}

	/**
	 * Returns the current traffic situation for the mixcascade.
	 *
	 * @return The current traffic situation for the mixcascade.
	 */
	public int getTrafficSituation()
	{
		return trafficSituation;
	}

	/**
	 * Returns the number of packets, which are mixed through the cascade since their startup.
	 *
	 * @return The number of mixed packets.
	 */
	public long getMixedPackets()
	{
		return mixedPackets;
	}

	/**
	 * Returns the calculated anonymity level (from number of active users, current traffic
	 * and cascade length). It is a value between 0 and 5.
	 *
	 * @return The current anonymity level.
	 */
	public int getAnonLevel()
	{
		return anonLevel;
	}

}

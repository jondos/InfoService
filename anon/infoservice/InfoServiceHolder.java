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

import java.util.Vector;
import anon.crypto.JAPCertificateStore;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class holds the instances of the InfoService class for the JAP client and is a singleton.
 */
public class InfoServiceHolder
{

	/**
	 * Function number for fetchInformation() - getMixCascades().
	 */
	private static final int GET_MIXCASCADES = 1;

	/**
	 * Function number for fetchInformation() - getInfoServices().
	 */
	private static final int GET_INFOSERVICES = 2;

	/**
	 * Function number for fetchInformation() - getMixInfo().
	 */
	private static final int GET_MIXINFO = 3;

	/**
	 * Function number for fetchInformation() - getStatusInfo().
	 */
	private static final int GET_STATUSINFO = 4;

	/**
	 * Function number for fetchInformation() - getNewVersionNumber().
	 */
	private static final int GET_NEWVERSIONNUMBER = 5;

	/**
	 * Function number for fetchInformation() - getJAPVersionInfo().
	 */
	private static final int GET_JAPVERSIONINFO = 6;

	/**
	 * Stores the instance of InfoServiceHolder (Singleton).
	 */
	private static InfoServiceHolder infoServiceHolderInstance = null;

	/**
	 * Stores the prefered InfoService. This InfoService is asked first for every information.
	 */
	private InfoService preferedInfoService;

	/**
	 * Stores the certificate store against the signatures of the XML documents are tested. If the
	 * value is null, no signature testing is done.
	 */
	private JAPCertificateStore m_certificateStore;

	/**
	 * Stores, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the prefered infoservice is used.
	 */
	private boolean m_changeInfoServices;

	/**
	 * This creates a new instance of InfoServiceHolder. This is only used for setting some
	 * values. Use InfoServiceHolder.getInstance() for getting an instance of this class.
	 */
	private InfoServiceHolder()
	{
		preferedInfoService = null;
		m_certificateStore = null;
		m_changeInfoServices = true;
	}

	/**
	 * Returns the instance of InfoServiceHolder (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The InfoServiceHolder instance.
	 */
	public static InfoServiceHolder getInstance()
	{
		if (infoServiceHolderInstance == null)
		{
			infoServiceHolderInstance = new InfoServiceHolder();
		}
		return infoServiceHolderInstance;
	}

	/**
	 * Sets the prefered InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @param preferedInfoService The prefered InfoService.
	 */
	public void setPreferedInfoService(InfoService preferedInfoService)
	{
		if (preferedInfoService != null)
		{
			synchronized (this)
			{
				this.preferedInfoService = preferedInfoService;
			}
			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "InfoServiceHolder: setPreferedInfoService: Prefered InfoService is now: " +
						  preferedInfoService.getName());
		}
	}

	/**
	 * Returns the prefered InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @return The prefered InfoService or null, if no prefered InfoService is set.
	 */
	public InfoService getPreferedInfoService()
	{
		InfoService preferedInfoService = null;
		synchronized (this)
		{
			preferedInfoService = this.preferedInfoService;
		}
		return preferedInfoService;
	}

	/**
	 * Sets the certificate store against the signatures of the XML documents are tested. If you
	 * supply null, no signature testing is done.
	 *
	 * @param a_certificateStore The certificate store for testing the signatures or null, if
	 *                           signature testing shall be disabled.
	 */
	public void setCertificateStore(JAPCertificateStore a_certificateStore)
	{
		synchronized (this)
		{
			m_certificateStore = a_certificateStore;
		}
	}

	/**
	 * Returns the certificate store against the signatures of the XML documents are tested. A value
	 * of null means that no signature testing is done.
	 *
	 * @return The certificate store for testing the signatures or null, if signature testing is
	 * disabled.
	 */
	public JAPCertificateStore getCertificateStore()
	{
		JAPCertificateStore r_certificateStore = null;
		synchronized (this)
		{
			r_certificateStore = m_certificateStore;
		}
		return r_certificateStore;
	}

	/**
	 * Sets, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the prefered infoservice is used.
	 *
	 * @param a_changeInfoServices Whether there are automatic changes of the infoservice.
	 */
	public void setChangeInfoServices(boolean a_changeInfoServices)
	{
		synchronized (this)
		{
			m_changeInfoServices = a_changeInfoServices;
		}
	}

	/**
	 * Returns, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the prefered infoservice is used for requests.
	 *
	 * @return Whether there are automatic changes of the infoservice.
	 */
	public boolean isChangeInfoServices()
	{
		boolean r_changeInfoServices = true;
		synchronized (this)
		{
			r_changeInfoServices = m_changeInfoServices;
		}
		return r_changeInfoServices;
	}

	/**
	 * Fetches every information from the infoservices. If we can't get the information from the
	 * prefered infoservice, all other known infoservices are asked automatically until an
	 * infoservice has the information. If we can't get the information from any infoservice, an
	 * Exception is thrown.
	 *
	 * @param functionNumber Specifies the InfoService function to call. Look at the constants
	 *                       defined in this class.
	 * @param arguments If an InfoService function needs arguments, these are in here.
	 *
	 * @return The needed information.
	 */
	private Object fetchInformation(int functionNumber, Vector arguments) throws Exception
	{
		InfoService currentInfoService = null;
		currentInfoService = getPreferedInfoService();
		Vector infoServiceList = null;
		if (m_changeInfoServices)
		{
			/* get the whole infoservice list */
			infoServiceList = InfoServiceDatabase.getInstance().getInfoServiceList();
		}
		else
		{
			/* use an empty list -> only prefered infoservice is used */
			infoServiceList = new Vector();
		}
		while ( (infoServiceList.size() > 0) || (currentInfoService != null))
		{
			if (currentInfoService == null)
			{
				/* take a new one from the list */
				currentInfoService = (InfoService) (infoServiceList.firstElement());
				LogHolder.log(LogLevel.INFO, LogType.NET,
							  "InfoServiceHolder: fetchInformation: Trying InfoService: " +
							  currentInfoService.getName());
			}
			try
			{
				Object result = null;
				/* try to get the information from currentInfoService */
				if (functionNumber == GET_MIXCASCADES)
				{
					result = currentInfoService.getMixCascades();
				}
				if (functionNumber == GET_INFOSERVICES)
				{
					result = currentInfoService.getInfoServices();
				}
				if (functionNumber == GET_MIXINFO)
				{
					result = currentInfoService.getMixInfo( (String) (arguments.elementAt(0)));
				}
				if (functionNumber == GET_STATUSINFO)
				{
					result = currentInfoService.getStatusInfo( (String) (arguments.elementAt(0)),
						( (Integer) (arguments.elementAt(1))).intValue());
				}
				if (functionNumber == GET_NEWVERSIONNUMBER)
				{
					result = currentInfoService.getNewVersionNumber();
				}
				if (functionNumber == GET_JAPVERSIONINFO)
				{
					result = currentInfoService.getJAPVersionInfo( ( (Integer) (arguments.elementAt(0))).
						intValue());
				}
				/* no error occured -> success -> update the prefered infoservice and exit */
				InfoService preferedInfoService = getPreferedInfoService();
				if (preferedInfoService != null)
				{
					if (!currentInfoService.getId().equals(preferedInfoService.getId()))
					{
						/* update only, if it is another infoservice */
						setPreferedInfoService(currentInfoService);
					}
				}
				else
				{
					/* if no prefered infoservice set -> set current infoservice */
					setPreferedInfoService(currentInfoService);
				}
				return result;
			}
			catch (Exception e)
			{
				/* if there was an error, remove currentInfoService from the list and try another
				 * infoservice
				 */
				infoServiceList.removeElement(currentInfoService);
				currentInfoService = null;
			}
		}
		/* could not find an infoservice with the needed information */
		throw (new Exception(
			"InfoServiceHolder: fetchInformation: No InfoService with the needed information available."));
	}

	/**
	 * Get a Vector of all mixcascades the prefered infoservice knows. If we can't get a the
	 * information from prefered infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of mixcascades.
	 */
	public Vector getMixCascades()
	{
		try
		{
			return (Vector) (fetchInformation(GET_MIXCASCADES, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getMixCascades: No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get a Vector of all infoservices the prefered infoservice knows. If we can't get a the
	 * information from prefered infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of infoservices.
	 */
	public Vector getInfoServices()
	{
		try
		{
			return (Vector) (fetchInformation(GET_INFOSERVICES, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getInfoServices: No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the MixInfo for the mix with the given ID. If we can't get a the information from
	 * prefered infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned. You should not call this method directly, better
	 * call the method in MixCascade to get the MixInfo.
	 *
	 * @param mixId The ID of the mix to get the MixInfo for.
	 *
	 * @return The MixInfo for the mix with the given ID.
	 */
	public MixInfo getMixInfo(String mixId)
	{
		try
		{
			Vector arguments = new Vector();
			arguments.addElement(mixId);
			return (MixInfo) (fetchInformation(GET_MIXINFO, arguments));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getMixInfo: No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the StatusInfo for the mixcascade with the given ID. If we can't get a the information
	 * from prefered infoservice, another known infoservice is asked. If we can't get the
	 * information from any infoservice, null is returned. You should not call this method directly,
	 * better call the method in MixCascade to get the current status.
	 *
	 * @param cascadeId The ID of the mixcascade to get the StatusInfo for.
	 * @param cascadeLength The length of the mixcascade (number of mixes). We need this for
	 *                      calculating the AnonLevel in the StatusInfo.
	 *
	 * @return The current StatusInfo for the mixcascade with the given ID.
	 */
	public StatusInfo getStatusInfo(String cascadeId, int cascadeLength)
	{
		try
		{
			Vector arguments = new Vector();
			arguments.addElement(cascadeId);
			arguments.addElement(new Integer(cascadeLength));
			return (StatusInfo) (fetchInformation(GET_STATUSINFO, arguments));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getStatusInfo: No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the version String of the current JAP version from the infoservice. This function is
	 * called to check, whether updates of the JAP are available. If we can't get a the information
	 * from prefered infoservice, another known infoservice is asked. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The version String (fromat: nn.nn.nnn) of the current JAP version.
	 */
	public String getNewVersionNumber()
	{
		try
		{
			return (String) (fetchInformation(GET_NEWVERSIONNUMBER, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getNewVersionNumber: No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Returns the JAPVersionInfo for the specified type. The JAPVersionInfo is generated from
	 * the JNLP files received from the infoservice. If we can't get a the information from prefered
	 * infoservice, another known infoservice is asked. If we can't get the information from any
	 * infoservice, null is returned.
	 *
	 * @param japVersionType Selects the JAPVersionInfo (release / development). Look at the
	 *                       Constants in JAPVersionInfo.
	 *
	 * @return The JAPVersionInfo of the specified type.
	 */
	public JAPVersionInfo getJAPVersionInfo(int japVersionType)
	{
		try
		{
			Vector arguments = new Vector();
			arguments.addElement(new Integer(japVersionType));
			return (JAPVersionInfo) (fetchInformation(GET_JAPVERSIONINFO, arguments));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getJAPVersionInfo: No InfoService with the needed information available.");
			return null;
		}
	}

}
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

import java.util.Enumeration;
import java.util.Vector;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceDatabase;
import anon.infoservice.InfoServiceHolder;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation for the infoservice savepoint. It is needed for restoring an old or
 * the default configuration, if the user presses "Cancel" or "Reset to defaults".
 */
public class JAPConfInfoServiceSavePoint implements IJAPConfSavePoint
{

	/**
	 * The Vector of all known infoservices.
	 */
	private Vector m_knownInfoServices;

	/**
	 * The prefered infoservice.
	 */
	private InfoService m_preferedInfoService;

	/**
	 * Whether automatic infoservice requests are disabled or not.
	 */
	private boolean m_automaticInfoServiceRequestsDisabled;

	/**
	 * Whether automatic changes of infoservice are enabled (if the default infoservice fails).
	 */
	private boolean m_automaticInfoServiceChanges;

	/**
	 * The timeout in seconds for infoservice communication.
	 */
	private int m_infoserviceTimeout;

	/**
	 * This method will store the current infoservice configuration in this savepoint.
	 */
	public void createSavePoint()
	{
		m_knownInfoServices = InfoServiceDatabase.getInstance().getEntryList();
		m_preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
		m_automaticInfoServiceRequestsDisabled = JAPModel.isInfoServiceDisabled();
		m_automaticInfoServiceChanges = InfoServiceHolder.getInstance().isChangeInfoServices();
		m_infoserviceTimeout = HTTPConnectionFactory.getInstance().getTimeout();
	}

	/**
	 * Restores the old infoservice configuration (stored with the last call of createSavePoint()).
	 */
	public void restoreSavePoint()
	{
		/* remove all infoservices from database and load the stored ones */
		InfoServiceDatabase.getInstance().removeAll();
		Enumeration infoServices = m_knownInfoServices.elements();
		while (infoServices.hasMoreElements())
		{
			InfoServiceDatabase.getInstance().update( (InfoService) (infoServices.nextElement()));
		}
		InfoServiceHolder.getInstance().setPreferedInfoService(m_preferedInfoService);
		JAPController.setInfoServiceDisabled(m_automaticInfoServiceRequestsDisabled);
		InfoServiceHolder.getInstance().setChangeInfoServices(m_automaticInfoServiceChanges);
		HTTPConnectionFactory.getInstance().setTimeout(m_infoserviceTimeout);
	}

	/**
	 * Loads the default infoservice configuration.
	 */
	public void restoreDefaults()
	{
		/* remove all infoservices from database and set prefered infoservice to the default
		 * infoservice
		 */
		InfoServiceDatabase.getInstance().removeAll();
		try
		{
			InfoService defaultInfoService = new InfoService(
				JAPConstants.defaultInfoServiceName,
				JAPConstants.defaultInfoServiceID,
				JAPConstants.defaultInfoServiceHostName, JAPConstants.defaultInfoServicePortNumber);
			InfoServiceHolder.getInstance().setPreferedInfoService(defaultInfoService);
		}
		catch (Exception e)
		{
			/* should not happen, if it happens, we can't do anything */
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
				"JAPConfInfoServiceSavePoint: restoreDefaults: Cannot create the default infoservice.");
		}
		JAPController.setInfoServiceDisabled(JAPConstants.DEFAULT_INFOSERVICE_DISABLED);
		InfoServiceHolder.getInstance().setChangeInfoServices(JAPConstants.DEFAULT_INFOSERVICE_CHANGES);
		HTTPConnectionFactory.getInstance().setTimeout(JAPConstants.DEFAULT_INFOSERVICE_TIMEOUT);
	}

}

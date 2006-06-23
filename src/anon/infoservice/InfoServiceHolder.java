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
package anon.infoservice;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.XMLUtil;
import anon.util.Util;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class holds the instances of the InfoService class for the JAP client and is a singleton.
 * The instance of this class is observable and will send a notification with an
 * InfoServiceHolderMessage, if the preferred InfoService or the InfoService management policy
 * were changed.
 */
public class InfoServiceHolder extends Observable
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
	 * Function number for fetchInformation() - getTorNodesList().
	 */
	private static final int GET_TORNODESLIST = 7;

	/**
	 * Function number for fetchInformation() - getForwarder().
	 */
	private static final int GET_FORWARDER = 8;

	/**
	 * Function number for fetchInformation() - getPaymentInstances().
	 */
	private static final int GET_PAYMENT_INSTANCES = 9;

	/**
	 * Function number for fetchInformation() - getPaymentInstance().
	 */
	private static final int GET_PAYMENT_INSTANCE = 10;

	/**
	 * Function number for fetchInformation() - getMixminionNodesList().
	 */
	private static final int GET_MIXMINIONNODESLIST = 11;

	private static final int GET_CASCADEINFO = 12;

	/**
	 * Stores the name of the root node of the XML settings for this class.
	 */
	private static final String XML_SETTINGS_ROOT_NODE_NAME = "InfoServiceManagement";

	/**
	 * Stores the instance of InfoServiceHolder (Singleton).
	 */
	private static InfoServiceHolder ms_infoServiceHolderInstance = null;

	/**
	 * Stores the preferred InfoService. This InfoService is asked first for every information.
	 */
	private InfoServiceDBEntry m_preferredInfoService;

	/**
	 * Stores, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used.
	 */
	private boolean m_changeInfoServices;

	/**
	 * This creates a new instance of InfoServiceHolder. This is only used for setting some
	 * values. Use InfoServiceHolder.getInstance() for getting an instance of this class.
	 */
	private InfoServiceHolder()
	{
		m_preferredInfoService = null;
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
		synchronized (InfoServiceHolder.class)
		{
			if (ms_infoServiceHolderInstance == null)
			{
				ms_infoServiceHolderInstance = new InfoServiceHolder();
			}
		}
		return ms_infoServiceHolderInstance;
	}

	/**
	 * Returns the name of the XML node used to store all settings of the InfoServiceHolder
	 * instance. This name can be used to find the XML node within a document when the settings
	 * shall be loaded.
	 *
	 * @return The name of the XML node created when storing the settings.
	 */
	public static String getXmlSettingsRootNodeName()
	{
		return XML_SETTINGS_ROOT_NODE_NAME;
	}

	/**
	 * Sets the preferred InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @param a_preferredInfoService The preferred InfoService.
	 */
	public synchronized void setPreferredInfoService(InfoServiceDBEntry a_preferredInfoService)
	{
		if (a_preferredInfoService != null)
		{
			/* also if m_preferredInfoService.equals(a_preferredInfoService), there is the possibility
			 * that some values of the infoservice, like listener interfaces or the name have been
			 * changed, so we always update the internal stored pererred infoservice
			 */
			m_preferredInfoService = a_preferredInfoService;
			setChanged();
			notifyObservers(new InfoServiceHolderMessage(InfoServiceHolderMessage.
				PREFERRED_INFOSERVICE_CHANGED, m_preferredInfoService));

			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "Preferred InfoService is now: " + m_preferredInfoService.getName());
		}
	}

	/**
	 * Returns the preferred InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @return The preferred InfoService or null, if no preferred InfoService is set.
	 */
	public InfoServiceDBEntry getPreferredInfoService()
	{
		return m_preferredInfoService;
	}

	/**
	 * Sets, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used.
	 *
	 * @param a_changeInfoServices Whether there are automatic changes of the infoservice.
	 */
	public void setChangeInfoServices(boolean a_changeInfoServices)
	{
		synchronized (this)
		{
			if (m_changeInfoServices != a_changeInfoServices)
			{
				m_changeInfoServices = a_changeInfoServices;
				setChanged();
				notifyObservers(new InfoServiceHolderMessage(InfoServiceHolderMessage.
					INFOSERVICE_MANAGEMENT_CHANGED, new Boolean(m_changeInfoServices)));
			}
		}
	}

	/**
	 * Returns, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used for requests.
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
	 * Returns a Vector of InfoServices with all known infoservices (including the preferred
	 * infoservice), which have a forwarder list.
	 *
	 * @return The Vector of all known infoservices with a forwarder list, maybe this Vector is
	 *         empty.
	 */
	public Vector getInfoservicesWithForwarderList()
	{
		Vector primaryInfoServices = new Vector();
		/* check the preferred infoservice */
		InfoServiceDBEntry currentPreferredInfoService = getPreferredInfoService();
		if (currentPreferredInfoService.hasPrimaryForwarderList() == true)
		{
			primaryInfoServices.addElement(currentPreferredInfoService);
		}
		Enumeration infoservices = Database.getInstance(InfoServiceDBEntry.class).getEntryList().elements();
		while (infoservices.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (infoservices.nextElement());
			if (currentInfoService.hasPrimaryForwarderList())
			{
				if (currentInfoService.getId().equals(currentPreferredInfoService.getId()) == false)
				{
					/* we have already the preferred infoservice in the list -> only add other infoservices */
					primaryInfoServices.addElement(currentInfoService);
				}
			}
		}
		return primaryInfoServices;
	}

	/**
	 * Fetches every information from the infoservices. If we can't get the information from the
	 * preferred infoservice, all other known infoservices are asked automatically until an
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
		InfoServiceDBEntry currentInfoService = null;
		Random random = new Random();
		int askInfoServices = 1;
		currentInfoService = getPreferredInfoService();
		Vector infoServiceList = null;
		if (m_changeInfoServices)
		{
			/* get the whole infoservice list */
			infoServiceList = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		}
		else
		{
			/* use an empty list -> only preferred infoservice is used */
			infoServiceList = new Vector();
		}

		Object result = null;
		/**
		 * @todo This is a first hack for the fact that not only one IS should be asked but
		 * a lot of IS...
		 */
		if (functionNumber == GET_INFOSERVICES || functionNumber == GET_MIXCASCADES
			|| functionNumber == GET_STATUSINFO)
		{
			result = new Hashtable();
			// try up to three InfoServices
			if (functionNumber == GET_STATUSINFO)
			{
				askInfoServices = 2;
			}
			else
			{
				askInfoServices = 3;
			}
		}


		while ( ( (infoServiceList.size() > 0) || (currentInfoService != null)) &&
				!Thread.currentThread().isInterrupted())
		{
			if (currentInfoService == null)
			{
				/* randomly take a new one from the list */
				currentInfoService = (InfoServiceDBEntry) (infoServiceList.elementAt(
					Math.abs(random.nextInt()) % infoServiceList.size()));
			}
			LogHolder.log(LogLevel.INFO, LogType.NET,
							  "Trying InfoService: " + currentInfoService.getName());
			try
			{
				Hashtable tempHashtable = null;

				/* try to get the information from currentInfoService */
				if (functionNumber == GET_MIXCASCADES)
				{
					tempHashtable = currentInfoService.getMixCascades();
				}
				else if (functionNumber == GET_INFOSERVICES)
				{
					tempHashtable = currentInfoService.getInfoServices();
				}
				else if (functionNumber == GET_MIXINFO)
				{
					result = currentInfoService.getMixInfo( (String) (arguments.elementAt(0)));
				}
				else if (functionNumber == GET_STATUSINFO)
				{
					tempHashtable = new Hashtable();
					StatusInfo info =
						currentInfoService.getStatusInfo( (String) (arguments.elementAt(0)),
						( (Integer) (arguments.elementAt(1))).intValue());
					if (info != null)
					{
						tempHashtable.put(info.getId(), info);
					}

				}
				else if (functionNumber == GET_NEWVERSIONNUMBER)
				{
					result = currentInfoService.getNewVersionNumber();
				}
				else if (functionNumber == GET_JAPVERSIONINFO)
				{
					result = currentInfoService.getJAPVersionInfo( ( (Integer) (arguments.elementAt(0))).
						intValue());
				}
				else if (functionNumber == GET_TORNODESLIST)
				{
					result = currentInfoService.getTorNodesList();
				}
				else if (functionNumber == GET_MIXMINIONNODESLIST)
				{
					result = currentInfoService.getMixminionNodesList();
				}
				else if (functionNumber == GET_FORWARDER)
				{
					result = currentInfoService.getForwarder();
				}
				else if (functionNumber == GET_PAYMENT_INSTANCES)
				{
					result = currentInfoService.getPaymentInstances();
				}
				else if (functionNumber == GET_PAYMENT_INSTANCE)
				{
					result = currentInfoService.getPaymentInstance( (String) arguments.firstElement());
				}
				else if (functionNumber == GET_CASCADEINFO)
				{
					result = currentInfoService.getMixCascadeInfo((String) arguments.firstElement());
				}

				if ((tempHashtable == null && result == null) ||
					(tempHashtable != null && tempHashtable.size() == 0))
				{
					LogHolder.log(LogLevel.INFO, LogType.NET,
							  "IS " + currentInfoService.getName() + " did not have the requested info!");
					infoServiceList.removeElement(currentInfoService);
					currentInfoService = null;
					continue;
				}
				else if (tempHashtable != null)
				{
					Enumeration newEntries = ((Hashtable)tempHashtable).elements();
					AbstractDatabaseEntry currentEntry;
					AbstractDatabaseEntry hashedEntry;
					while (newEntries.hasMoreElements())
					{
						currentEntry = (AbstractDatabaseEntry)newEntries.nextElement();
						if (((Hashtable)result).containsKey(currentEntry.getId()))
						{
							hashedEntry =
								(AbstractDatabaseEntry)((Hashtable)result).get(currentEntry.getId());
							if (hashedEntry.getExpireTime() >= currentEntry.getExpireTime())
							{
								continue;
							}
						}
						((Hashtable)result).put(currentEntry.getId(), currentEntry);
					}

					askInfoServices--;
					if (askInfoServices == 0)
					{
						break;
					}
					infoServiceList.removeElement(currentInfoService);
					currentInfoService = null;
					continue;
				}

				break;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.INFO, LogType.NET,
							  "Contacting IS " + currentInfoService.getName() + " produced an error!");
				LogHolder.log(LogLevel.DEBUG, LogType.NET, e);
				/* if there was an error, remove currentInfoService from the list and try another
				 * infoservice
				 */
				infoServiceList.removeElement(currentInfoService);
				currentInfoService = null;
			}
		}

		if (result != null && (!(result instanceof Hashtable) || ((Hashtable)result).size() > 0) )
		{
			if (functionNumber == GET_STATUSINFO)
			{
				result = ((Hashtable) result).elements().nextElement();
			}

			return result;
		}
		/* could not find an infoservice with the needed information */
		throw (new Exception(
			"No InfoService with the needed information available."));
	}

	/**
	 * Get a Vector of all mixcascades the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of mixcascades.
	 */
	public Hashtable getMixCascades()
	{
		try
		{
			return (Hashtable) (fetchInformation(GET_MIXCASCADES, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get a Vector of all payment instances the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of payment instances.
	 */
	public Vector getPaymentInstances()
	{
		try
		{
			return (Vector) (fetchInformation(GET_PAYMENT_INSTANCES, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "InfoServiceHolder: getPaymentInstances: No InfoService with the needed information available.");
			return null;
		}
	}

	/** Get information for a particular payment instance identified by a_piID
	 *
	 * @return Payment Instance information
	 */

	public PaymentInstanceDBEntry getPaymentInstance(String a_piID) throws Exception
	{

		Vector args = new Vector();
		args.addElement(a_piID);
		return (PaymentInstanceDBEntry) (fetchInformation(GET_PAYMENT_INSTANCE, args));
	}

	/**
	 * Get a Vector of all infoservices the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of infoservices.
	 */
	public Hashtable getInfoServices()
	{
		try
		{
			return (Hashtable) (fetchInformation(GET_INFOSERVICES, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the MixInfo for the mix with the given ID. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
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
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the StatusInfo for the mixcascade with the given ID. If we can't get a the information
	 * from preferred infoservice, another known infoservice is asked. If we can't get the
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
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the version String of the current JAP version from the infoservice. This function is
	 * called to check, whether updates of the JAP are available. If we can't get a the information
	 * from preferred infoservice, another known infoservice is asked. If we can't get the
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
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Returns the JAPVersionInfo for the specified type. The JAPVersionInfo is generated from
	 * the JNLP files received from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
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
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Get the list with the tor nodes from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @return The raw tor nodes list as it is distributed by the tor directory servers.
	 */
	public String getTorNodesList()
	{
		try
		{
			return (String) (fetchInformation(GET_TORNODESLIST, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	public MixCascade getMixCascadeInfo(String a_cascadeID)
	{
		try
		{
			return (MixCascade) (fetchInformation(GET_CASCADEINFO, Util.toVector(a_cascadeID)));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
}

	/**
	 * Get the list with the mixminion nodes from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @return The raw mixminion nodes list as it is distributed by the mixminion directory servers.
	 */
	public String getMixminionNodesList()
	{
		try
		{
			return (String) (fetchInformation(GET_MIXMINIONNODESLIST, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Downloads a forwarder entry from a infoservice. If that infoservice has no forwarder list,
	 * it will ask another infoservice with such a list and returns the answer to us. If we can't
	 * get the information from preferred infoservice, another known infoservice is asked. If we
	 * can't get the information from any infoservice, null is returned.
	 *
	 * @return The JapForwarder node of the answer of the infoservice's getforwarder command.
	 */
	public Element getForwarder()
	{
		try
		{
			return (Element) (fetchInformation(GET_FORWARDER, null));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information available.");
			return null;
		}
	}

	/**
	 * Returns all settings (including the database of known infoservices) as an XML node.
	 *
	 * @param a_doc The parent document for the created XML node.
	 *
	 * @return The settings of this instance of InfoServiceHolder as an XML node.
	 */
	public Element getSettingsAsXml(Document a_doc)
	{
		Element infoServiceManagementNode = a_doc.createElement(XML_SETTINGS_ROOT_NODE_NAME);
		Element infoServicesNode = InfoServiceDBEntry.toXmlElement(a_doc,
			Database.getInstance(InfoServiceDBEntry.class));
		Element preferredInfoServiceNode = a_doc.createElement("PreferredInfoService");
		Element changeInfoServicesNode = a_doc.createElement("ChangeInfoServices");
		synchronized (this)
		{
			InfoServiceDBEntry preferredInfoService = getPreferredInfoService();
			if (preferredInfoService != null)
			{
				preferredInfoServiceNode.appendChild(preferredInfoService.toXmlElement(a_doc));
			}
			XMLUtil.setValue(changeInfoServicesNode, isChangeInfoServices());
		}
		infoServiceManagementNode.appendChild(infoServicesNode);
		infoServiceManagementNode.appendChild(preferredInfoServiceNode);
		infoServiceManagementNode.appendChild(changeInfoServicesNode);
		return infoServiceManagementNode;
	}

	/**
	 * Restores the settings of this instance of InfoServiceHolder with the settings stored in the
	 * specified XML node.
	 *
	 * @param a_infoServiceManagementNode The XML node for loading the settings from. The name of
	 *                                    the needed XML node can be obtained by calling
	 *                                    getXmlSettingsRootNodeName().
	 */
	public void loadSettingsFromXml(Element a_infoServiceManagementNode) throws Exception
	{
		/* parse the whole InfoServiceManagement node */
		Element infoServicesNode = (Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
			"InfoServices"));
		if (infoServicesNode == null)
		{
			throw (new Exception("InfoServiceHolder: loadSettingsFromXml: No InfoServices node found."));
		}
		/* InfoServices node found -> load it into the database of known infoservices */
		InfoServiceDBEntry.loadFromXml(infoServicesNode, Database.getInstance(InfoServiceDBEntry.class));
		Element preferredInfoServiceNode = (Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
			"PreferredInfoService"));
		if (preferredInfoServiceNode == null)
		{
			throw (new Exception(
				"InfoServiceHolder: loadSettingsFromXml: No PreferredInfoService node found."));
		}
		Element infoServiceNode = (Element) (XMLUtil.getFirstChildByName(preferredInfoServiceNode,
			"InfoService"));
		InfoServiceDBEntry preferredInfoService = null;
		if (infoServiceNode != null)
		{
			/* there is a preferred infoservice -> parse it */
			preferredInfoService = new InfoServiceDBEntry(infoServiceNode);
		}
		Element changeInfoServicesNode = (Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
			"ChangeInfoServices"));
		if (changeInfoServicesNode == null)
		{
			throw (new Exception("InfoServiceHolder: loadSettingsFromXml: No ChangeInfoServices node found."));
		}
		synchronized (this)
		{
			/* we have collected all values -> set them */
			if (preferredInfoService != null)
			{
				setPreferredInfoService(preferredInfoService);
			}
			setChangeInfoServices(XMLUtil.parseValue(changeInfoServicesNode, isChangeInfoServices()));
		}
	}

}

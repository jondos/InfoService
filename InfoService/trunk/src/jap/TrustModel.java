/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import java.security.SignatureException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.client.BasicTrustModel;
import anon.client.ITrustModel.TrustException;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import gui.JAPMessages;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.client.ITrustModel;
import anon.infoservice.BlacklistedCascadeIDEntry;


/**
 * This is the general trust model for JAP.
 *
 * @author Rolf Wendolsky
 * @todo The trust settings must be fully pluggable!!! They are currently hard-coded in separate methods...
 */
public class TrustModel extends BasicTrustModel implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "TrustModel";
	public static final String XML_ELEMENT_CONTAINER_NAME = "TrustModels";

	public static final int TRUST_NONE = 0;
	public static final int TRUST_LITTLE = 10;
	public static final int TRUST_DEFAULT = 20;
	public static final int TRUST_EXCLUSIVE = 30;

	private static final String XML_ELEM_PAY = "Payment";
	private static final String XML_ELEM_EXPIRED = "ExpiredCerts";
	private static final String XML_ATTR_CURRENT_TRUST_MODEL = "currentTrustModel";
	private static final String XML_ATTR_TRUST = "trust";
	private static final String XML_ATTR_NAME = "name";
	private static final String XML_ATTR_SHOW_WARNING = "showWarning";
	private static final String[] XML_ATTR_VALUE_TRUST = new String[]{"none", "little", "default", "exclusive"};

	private static final String MSG_CERTIFIED_CASCADES = TrustModel.class.getName() + "_certifiedCascades";
	private static final String MSG_CASCADES_WITH_COSTS = TrustModel.class.getName() + "_cascadesWithCosts";
	private static final String MSG_CASCADES_WITHOUT_COSTS =
		TrustModel.class.getName() + "_cascadesWithoutCosts";
	private static final String MSG_CASCADES_USER_DEFINED =
		TrustModel.class.getName() + "_cascadesUserDefined";

	private static Vector ms_defaultTrustModels;
	private static Vector ms_trustModels;
	private static TrustModel ms_currentTrustModel;

	private int m_trustPay = TRUST_DEFAULT;
	private int m_trustExpiredCerts = TRUST_DEFAULT;
	private int m_trustUserDefined = TRUST_DEFAULT;

	private String m_strName;
	private long m_id;


	static
	{
	    ms_defaultTrustModels = new Vector();

		// initialise basic trust models
		TrustModel model = new TrustModel();
		model.setName(JAPMessages.getString(MSG_CERTIFIED_CASCADES));
		model.m_id = 0;
		model.setTrustExpiredCerts(TRUST_LITTLE);
		model.setTrustPay(TRUST_DEFAULT);
		ms_defaultTrustModels.addElement(model);

		model = new TrustModel();
		model.setName(JAPMessages.getString(MSG_CASCADES_WITH_COSTS));
		model.m_id = 1;
		model.setTrustPay(TRUST_EXCLUSIVE);
		ms_defaultTrustModels.addElement(model);

		model = new TrustModel();
		model.setName(JAPMessages.getString(MSG_CASCADES_WITHOUT_COSTS));
		model.m_id = 2;
		model.setTrustPay(TRUST_NONE);
		ms_defaultTrustModels.addElement(model);

		model = new TrustModel()
		{
			public boolean isAdded()
			{
				Enumeration enties = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
				while (enties.hasMoreElements())
				{
					if (((MixCascade)enties.nextElement()).isUserDefined())
					{
						return true;
					}
				}
				return false;
			}
		};
		model.setName(JAPMessages.getString(MSG_CASCADES_USER_DEFINED));
		model.m_id = 3;
		model.m_trustUserDefined = TRUST_EXCLUSIVE;
		ms_defaultTrustModels.addElement(model);


		ms_trustModels = (Vector)ms_defaultTrustModels.clone();
		setCurrentTrustModel((TrustModel)ms_defaultTrustModels.elementAt(0));
	}

	public TrustModel()
	{
		m_id = 0;
		while (m_id >= 0 && m_id < ms_defaultTrustModels.size())
		{
			m_id = Math.abs(new Random().nextLong());
		}
		m_strName = "Default trust model";
	}

	public TrustModel(TrustModel a_trustModel)
	{
		this();
		if (a_trustModel == null)
		{
			throw new IllegalArgumentException("No argument given!");
		}
		setTrustExpiredCerts(a_trustModel.getTrustExpiredCerts());
		setTrustPay(getTrustPay());
	}

	public TrustModel(Element a_trustModelElement) throws XMLParseException
	{
		XMLUtil.assertNodeName(a_trustModelElement, XML_ELEMENT_NAME);

		//m_bShowWarning = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_SHOW_WARNING, m_bShowWarning);
		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_ID);
		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_NAME);

		m_id = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_ID, -1l);
		m_strName = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_NAME, null);


		m_trustPay = parseTrust(XMLUtil.parseAttribute(
			  XMLUtil.getFirstChildByName(a_trustModelElement, XML_ELEM_PAY),
			  XML_ATTR_TRUST, null), m_trustPay);
		m_trustExpiredCerts = parseTrust(XMLUtil.parseAttribute(
			  XMLUtil.getFirstChildByName(a_trustModelElement, XML_ELEM_EXPIRED),
			  XML_ATTR_TRUST, null), m_trustExpiredCerts);
	}


	public static abstract class AbstractTrustFilter implements IXMLEncodable
	{
		public static final int CATEGORY_DEFAULT = 0;

		private String m_strName;
		private int m_category;
		private int m_trust;
		private Hashtable m_allowedTrustSettings;

		private AbstractTrustFilter(String a_name, Hashtable a_allowedTrustSettings)
		{
			m_strName = a_name;
			m_category = CATEGORY_DEFAULT;
			m_allowedTrustSettings = (Hashtable)m_allowedTrustSettings.clone();
		}

		public final String getName()
		{
			return m_strName;
		}

		public final int getCategory()
		{
			return m_category;
		}

		public final int getTrust()
		{
			return m_trust;
		}

		public final void setTrust(int a_trust)
		{
			m_trust = a_trust;
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			// do nothing
		}

	}



	public boolean equals(Object a_trustModel)
	{
		if (a_trustModel == null || !(a_trustModel instanceof TrustModel))
		{
			return false;
		}
		return getId() == ((TrustModel)a_trustModel).getId();
	}

	public int hashCode()
	{
		return (int)getId();
	}

	public static boolean addTrustModel(TrustModel a_trustModel)
	{
		synchronized (ms_trustModels)
		{
			if (a_trustModel != null && !ms_trustModels.contains(a_trustModel))
			{
				ms_trustModels.addElement(a_trustModel);
				return true;
			}
		}
		return false;
	}

	public static TrustModel removeTrustModel(TrustModel a_trustModel)
	{
		if (a_trustModel.getId() < ms_defaultTrustModels.size())
		{
			// default models must not be removed
			return null;
		}
		synchronized (ms_trustModels)
		{
			if (a_trustModel != null && ms_trustModels.removeElement(a_trustModel))
			{
				return a_trustModel;
			}
		}
		return null;
	}

	public static Vector getTrustModels()
	{
		return (Vector)ms_trustModels.clone();
	}

	public static void setCurrentTrustModel(long a_id)
	{
		if (a_id < 0)
		{
			return;
		}

		synchronized (ms_trustModels)
		{
			for (int i = 0; i < ms_trustModels.size(); i++)
			{
				if (((TrustModel)ms_trustModels.elementAt(i)).getId() == a_id)
				{
					ms_currentTrustModel = (TrustModel)ms_trustModels.elementAt(i);
					break;
				}
			}
		}
	}

	public static void setCurrentTrustModel(TrustModel a_trustModel)
	{
		if (a_trustModel == null)
		{
			return;
		}
		synchronized (ms_trustModels)
		{
			if (!ms_trustModels.contains(a_trustModel))
			{
				ms_trustModels.addElement(a_trustModel);
			}
			ms_currentTrustModel = a_trustModel;
		}

	}

	public static TrustModel getCurrentTrustModel()
	{
		return ms_currentTrustModel;
	}

	public static void fromXmlElement(Element a_container)
	{
		if (a_container == null || !a_container.getNodeName().equals(XML_ELEMENT_CONTAINER_NAME))
		{
			return;
		}
		ms_trustModels.removeAllElements();
		ms_trustModels = (Vector)ms_defaultTrustModels.clone();
		NodeList elements = a_container.getElementsByTagName(XML_ELEMENT_NAME);
		for (int i = 0; i < elements.getLength(); i++)
		{
			try
			{
				addTrustModel(new TrustModel( (Element) elements.item(i)));
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not load trust model from XML!", a_e);
			}
		}
		setCurrentTrustModel(XMLUtil.parseAttribute(a_container, XML_ATTR_CURRENT_TRUST_MODEL, 0l));
	}

	public static Element toXmlElement(Document a_doc, String a_xmlContainerName)
	{
		if (a_doc == null || a_xmlContainerName == null)
		{
			return null;
		}

		Element container = a_doc.createElement(a_xmlContainerName);
		XMLUtil.setAttribute(container, XML_ATTR_CURRENT_TRUST_MODEL, getCurrentTrustModel().getId());
		synchronized (ms_trustModels)
		{
			for (int i = ms_defaultTrustModels.size(); i < ms_trustModels.size(); i++)
			{
				container.appendChild(((TrustModel)ms_trustModels.elementAt(i)).toXmlElement(a_doc));
			}
		}
		return container;
	}

	public void setName(String a_strName) throws IllegalArgumentException
	{
		if (a_strName == null || a_strName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Invalid name for trust model!");
		}
		m_strName = a_strName;
	}

	public String getName()
	{
		return m_strName;
	}

	public String toString()
	{
		return m_strName;
	}

	public long getId()
	{
		return m_id;
	}


	public Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}

		Element elemTrustModel = a_doc.createElement(XML_ELEMENT_NAME);
		Element elemTemp;

		//XMLUtil.setAttribute(elemTrustModel, XML_ATTR_SHOW_WARNING, m_bShowWarning);
		XMLUtil.setAttribute(elemTrustModel, XML_ATTR_ID, m_id);
		XMLUtil.setAttribute(elemTrustModel, XML_ATTR_NAME, m_strName);

		elemTemp = a_doc.createElement(XML_ELEM_PAY);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_TRUST, XML_ATTR_VALUE_TRUST[m_trustPay]);
		elemTrustModel.appendChild(elemTemp);

		elemTemp = a_doc.createElement(XML_ELEM_EXPIRED);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_TRUST, XML_ATTR_VALUE_TRUST[m_trustExpiredCerts]);
		elemTrustModel.appendChild(elemTemp);

		return elemTrustModel;
	}

	public void setTrustExpiredCerts(int a_trust)
	{
		synchronized (this)
		{
			if (m_trustExpiredCerts != a_trust)
			{
				setChanged();
				m_trustExpiredCerts = a_trust;
			}
			notifyObservers();
		}
	}

	public int getTrustExpiredCerts()
	{
		return m_trustExpiredCerts;
	}

	public void setTrustPay(int a_trust)
	{
		synchronized (this)
		{
			if (m_trustPay != a_trust)
			{
				m_trustPay = a_trust;
				setChanged();
			}
			notifyObservers();
		}
	}

	public int getTrustPay()
	{
		return m_trustPay;
	}

	public boolean isAdded()
	{
		return true;
	}

	public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
	{
		if (a_cascade != null && Database.getInstance(BlacklistedCascadeIDEntry.class).getEntryById(
			  a_cascade.getMixIDsAsString()) != null)
		{
			throw (new TrustException("Cascade is in blacklist!"));
		}

		if (m_trustUserDefined == TRUST_EXCLUSIVE)
		{
			if (a_cascade != null && a_cascade.isUserDefined())
			{
				if (a_cascade.getCertificate() == null && (a_cascade.getNumberOfMixes() == 0 ||
					a_cascade.getNumberOfMixes() == 1 &&
					(a_cascade.getMixInfo(0) == null || a_cascade.getMixInfo(0).getCertificate() == null)))
				{
					// not yet connected; do not make further tests
					return;
				}
			}
			else
			{
				throw new TrustException("Only user-defined services allowed!");
			}
		}

		super.checkTrust(a_cascade);



		// test if all mixes have valid certificates.
		MixInfo info;
		for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
		{
			info = a_cascade.getMixInfo(i);
			if (info == null || !info.isVerified())
			{
				throw new SignatureException("Mix " + (i + 1) + " has no valid signature!");
			}
		}

		if (a_cascade.isPayment())
		{
			if (m_trustPay == TRUST_NONE)
			{
				throw new TrustException("Payment is not allowed!");
			}
		}
		else if (m_trustPay == TRUST_EXCLUSIVE)
		{
			throw new TrustException("Only payment services allowed!");
		}

		if (a_cascade.getCertPath() != null && !a_cascade.getCertPath().checkValidity(new Date()))
		{
			if (m_trustExpiredCerts == TRUST_NONE)
			{
				throw new TrustException("Expired certificates are not trusted!");
			}
		}
	}

	private int parseTrust(String a_trustValue, int a_default)
	{
		if (a_trustValue == null)
		{
			return a_default;
		}

		for (int i = 0 ; i < XML_ATTR_VALUE_TRUST.length; i++)
		{
			if (XML_ATTR_VALUE_TRUST[i].equals(a_trustValue))
			{
				return i;
			}
		}

		return a_default;
	}
}

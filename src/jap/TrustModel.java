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

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import anon.client.BasicTrustModel;
import anon.client.ITrustModel.TrustException;
import anon.infoservice.MixCascade;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import anon.infoservice.MixInfo;


/**
 * This is the general trust model for JAP.
 *
 * @author Rolf Wendolsky
 */
public class TrustModel extends BasicTrustModel implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "TrustModel";

	public static final int TRUST_NONE = 0;
	public static final int TRUST_LITTLE = 1;
	public static final int TRUST_HIGH = 2;
	public static final int TRUST_EXCLUSIVE = 3;

	public static final int GENERAL_TRUST_PARANOID = 3;
	public static final int GENERAL_TRUST_SUSPICIOUS = 2;
	public static final int GENERAL_TRUST_HIGH = 1;
	public static final int GENERAL_TRUST_ALL = 0;


	public static final int DEFAULT_TRUST_PAY = TRUST_HIGH;
	public static final int DEFAULT_TRUST_EXPIRED_CERTS = TRUST_LITTLE;
	public static final int DEFAULT_TRUST = GENERAL_TRUST_HIGH;

	private static final String XML_ELEM_PAY = "Payment";
	private static final String XML_ELEM_EXPIRED = "ExpiredCerts";

	private static final String XML_ATTR_TRUST = "trust";
	private static final String[] XML_ATTR_VALUE_TRUST = new String[]{"none", "little", "high", "exclusive"};
	private static final String[] XML_ATTR_VALUE_GENERAL_TRUST =
		new String[]{"paranoid", "suspicious", "high", "all"};

	private int m_trustPay = DEFAULT_TRUST_PAY;
	private int m_trustExpiredCerts = DEFAULT_TRUST_EXPIRED_CERTS;

	private int m_generalTrust = DEFAULT_TRUST;

	public void parse(Element a_trustModelElement)
	{
		if (a_trustModelElement == null)
		{
			return;
		}
		m_generalTrust = parseGeneralTrust(
			  XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_TRUST, null), m_generalTrust);
		m_trustPay = parseTrust(XMLUtil.parseAttribute(
			  XMLUtil.getFirstChildByName(a_trustModelElement, XML_ELEM_PAY),
			  XML_ATTR_TRUST, null), m_trustPay);
		m_trustExpiredCerts = parseTrust(XMLUtil.parseAttribute(
			  XMLUtil.getFirstChildByName(a_trustModelElement, XML_ELEM_EXPIRED),
			  XML_ATTR_TRUST, null), m_trustExpiredCerts);
	}

	public Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}

		Element elemTrustModel = a_doc.createElement(XML_ELEMENT_NAME);
		Element elemTemp;

		XMLUtil.setAttribute(elemTrustModel, XML_ATTR_TRUST, XML_ATTR_VALUE_GENERAL_TRUST[m_generalTrust]);

		elemTemp = a_doc.createElement(XML_ELEM_PAY);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_TRUST, XML_ATTR_VALUE_TRUST[m_trustPay]);
		elemTrustModel.appendChild(elemTemp);

		elemTemp = a_doc.createElement(XML_ELEM_EXPIRED);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_TRUST, XML_ATTR_VALUE_TRUST[m_trustExpiredCerts]);
		elemTrustModel.appendChild(elemTemp);

		return elemTrustModel;
	}

	public void setGeneralTrust(int a_trust)
	{
		synchronized (this)
		{
			if (m_generalTrust != a_trust)
			{
				setChanged();
				m_generalTrust = a_trust;
			}
			notifyObservers();
		}
	}

	public int getGeneralTrust()
	{
		return m_generalTrust;
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

	public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
	{
		super.checkTrust(a_cascade);

		if (m_generalTrust == GENERAL_TRUST_ALL)
		{
			return;
		}

		if (a_cascade.isUserDefined())
		{
			// do not make further tests
			return;
		}

		if (m_generalTrust == GENERAL_TRUST_PARANOID)
		{
			// test if all mixes have valid certificates; do more later...
			MixInfo info;
			for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
			{
				info = a_cascade.getMixInfo(i);
				if (info == null || !info.isVerified())
				{
					throw new SignatureException("Mix " + (i + 1) + " has no valid signature!");
				}
			}
		}

		if (a_cascade.isPayment())
		{
			if (m_trustPay != TRUST_HIGH)
			{
				if (m_trustPay < m_generalTrust)
				{
					throw new TrustException("Payment is not trusted!");
				}
			}
		}
		else if (m_trustPay == TRUST_EXCLUSIVE)
		{
			throw new TrustException("Only payment services allowed!");
		}

		if (a_cascade.getCertPath() != null && !a_cascade.getCertPath().checkValidity(new Date()))
		{
			if (m_trustExpiredCerts < m_generalTrust)
			{
				throw new TrustException("Expired certificates are not trusted!");
			}
		}
	}

	private int parseGeneralTrust(String a_trustValue, int a_default)
	{
		if (a_trustValue == null)
		{
			return a_default;
		}

		for (int i = 0; i < XML_ATTR_VALUE_GENERAL_TRUST.length; i++)
		{
			if (XML_ATTR_VALUE_GENERAL_TRUST[i].equals(a_trustValue))
			{
				return i;
			}
		}

		return a_default;
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

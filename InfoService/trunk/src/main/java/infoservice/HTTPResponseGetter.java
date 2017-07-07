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

import java.util.Enumeration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.AbstractDistributableDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.HttpResponseStructure;
import anon.infoservice.IBoostrapable;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

abstract class HTTPResponseGetter
{
	private static final long CACHE_SERIALS_MS = 10000;
	private static final long CACHE_MS = 10000;

	private HttpResponseStructure m_cachedSerialsResponse;
	private HttpResponseStructure m_cachedSerialsCompressedResponse;
	private final Object SYNC_CACHE_SERIALS = new Object();
	private long m_lastSerialsUpdate = 0;

	private HttpResponseStructure m_cachedResponse;
	private HttpResponseStructure m_cachedCompressedResponse;
	private final Object SYNC_CACHE = new Object();
	private long m_lastUpdate = 0;
	private boolean m_bWebInfo;

	public HTTPResponseGetter()
		{
		}

	public HTTPResponseGetter(boolean a_bWebInfo)
		{
			m_bWebInfo = a_bWebInfo;
		}

	public abstract Class<? extends AbstractDatabaseEntry> getDatabaseClass();

	protected HttpResponseStructure fetchResponse(int a_supportedEncodings)
		{
			HttpResponseStructure httpResponse;
			Document doc;
			Element containerNode;

			synchronized (SYNC_CACHE)
				{
					if (m_lastUpdate < (System.currentTimeMillis() - CACHE_MS))
						{
							m_lastUpdate = System.currentTimeMillis();

							if (m_bWebInfo)
								{
									doc = Database.getInstance(getDatabaseClass()).getWebInfos();
								}
							else
								{
									doc = XMLUtil.createDocument();
									containerNode = doc.createElement(XMLUtil.getXmlElementContainerName(getDatabaseClass()));
									XMLUtil.setAttribute(containerNode, "id", Configuration.getInstance().getID());
									XMLUtil.setAttribute(containerNode, AbstractDatabaseEntry.XML_ATTR_LAST_UPDATE, m_lastUpdate);

									/* append the nodes of all entries we know */
									Enumeration knownentries = Database.getInstance(getDatabaseClass())
											.getEntrySnapshotAsEnumeration();
									IXMLEncodable currentCascade;
									Element node;
									while (knownentries.hasMoreElements())
										{
											/* import the entry XML structure in this document */
											currentCascade = (IXMLEncodable) (knownentries.nextElement());
											if (currentCascade instanceof IBoostrapable
													&& ((IBoostrapable) currentCascade).isBootstrap())
												{
													// do not forward this entry, as it is for internal use only
													continue;
												}
											node = currentCascade.toXmlElement(doc);
											containerNode.appendChild(node);
										}
									SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
											containerNode);
									doc.appendChild(containerNode);
								}

							/* send the XML document to the client */
							//if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
								{
									try
										{
											m_cachedCompressedResponse = new HttpResponseStructure(
													HttpResponseStructure.HTTP_TYPE_TEXT_XML, HttpResponseStructure.HTTP_ENCODING_ZLIB,
													ZLibTools.compress(XMLUtil.toByteArray(doc)));
											//ZLibTools.compress(XMLSignature.toCanonical(doc)));
										}
									catch (/*XMLParse*/Exception ex)
										{
											m_cachedCompressedResponse = new HttpResponseStructure(
													HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
										}
								}
							//else
								{
									m_cachedResponse = new HttpResponseStructure(doc);
								}
						}
				}

			if ((a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
				{
					httpResponse = m_cachedCompressedResponse;
				}
			else
				{
					httpResponse = m_cachedResponse;
				}
			return httpResponse;
		}

	protected HttpResponseStructure fetchSerialsResponse(int a_supportedEncodings)
		{
			HttpResponseStructure httpResponse;
			Document doc;
			Element node;

			synchronized (SYNC_CACHE_SERIALS)
				{
					if (m_lastSerialsUpdate < (System.currentTimeMillis() - CACHE_SERIALS_MS))
						{
							doc = XMLUtil.createDocument();
							node = new AbstractDistributableDatabaseEntry.Serials(getDatabaseClass()).toXmlElement(doc);
							SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, node);
							doc.appendChild(node);

							//if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
								{
									try
										{
											m_cachedSerialsCompressedResponse = new HttpResponseStructure(
													HttpResponseStructure.HTTP_TYPE_TEXT_XML, HttpResponseStructure.HTTP_ENCODING_ZLIB,
													//ZLibTools.compress(XMLUtil.toByteArray(doc)));
													ZLibTools.compress(XMLSignature.toCanonical(doc)));
										}
									catch (XMLParseException ex)
										{
											m_cachedCompressedResponse = new HttpResponseStructure(
													HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
										}
								}
							//else
								{
									m_cachedSerialsResponse = new HttpResponseStructure(doc);
								}
							m_lastSerialsUpdate = System.currentTimeMillis();
						}
				}
			/* send the XML document to the client */
			if ((a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
				{
					httpResponse = m_cachedSerialsCompressedResponse;
				}
			else
				{
					httpResponse = m_cachedSerialsResponse;
				}
			return httpResponse;
		}

	/**
	 * Sends the complete list of all known db entries to the client.
	 *
	 * @param a_supportedEncodings
	 *          defines the encoding supported by the client (deflate,
	 *          gzip,...)
	 * @param a_bSerialsOnly
	 *          only return a list with db entry serial numbers so that the
	 *          caller may decide which db entries have changed since the
	 *          last request
	 * @return The HTTP response for the client.
	 */
	public HttpResponseStructure fetchResponse(int a_supportedEncodings, boolean a_bSerialsOnly)
		{
			HttpResponseStructure httpResponse;
			try
				{
					if (a_bSerialsOnly)
						{
							httpResponse = fetchSerialsResponse(a_supportedEncodings);
						}
					else
						{
							httpResponse = fetchResponse(a_supportedEncodings);
						}
				}
			catch (Exception e)
				{
					/* should never occur */
					httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
					LogHolder.log(LogLevel.ERR, LogType.NET, e);
				}
			return httpResponse;
		}
}
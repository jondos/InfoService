/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.proxy;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Provides functionality for parsing and storing the headers of a
 * HTTP-Connection. Invoked by the ProxyCallbackHandler framework.
 * Inheriting from this class and implementing the abstract handler functions
 * allows examining and modifying of rthe correspondende HTTP messages before it is
 * transmitted further.
 * @author Simon Pecher
 */
public abstract class HTTPProxyCallback implements ProxyCallback
{
	final static int MESSAGE_TYPE_REQUEST = 0;
	final static int MESSAGE_TYPE_RESPONSE = 1;
	
	final static String CRLF = "\r\n";
	final static String HTTP_HEADER_END = CRLF+CRLF; //end of http message headers
	final static String HTTP_HEADER_DELIM = ": ";
	final static String HTTP_START_LINE_KEY = "start-line";
	final static String HTTP_VERSION_PREFIX = "HTTP/";
	final static String[] HTTP_REQUEST_METHODS = {"OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT"};
	
	public final String HTTP_CONTENT_LENGTH = "Content-Length";
	
	public final String HTTP_HOST = "Host";
	public final String HTTP_USER_AGENT = "User-Agent";
	public final String HTTP_ACCEPT = "Accept";
	public final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
	public final String HTTP_ACCEPT_ENCODING =  "Accept-Encoding";
	public final String HTTP_ACCEPT_CHARSET = "Accept-Charset";
	public final String HTTP_KEEP_ALIVE = "Keep-Alive";
	public final String HTTP_PROXY_CONNECTION = "Proxy-Connection";
	public final String HTTP_REFERER = "Referer";
	public final String HTTP_CACHE_CONTROL = "Cache-Control";
	public final String HTTP_PRAGMA = "Pragma";
	public final String HTTP_IE_UA_CPU = "UA-CPU";	
	
	/** Container for the Headers of a whole HTTP Connection
	 * including Request and Response. 
	 */
	private Hashtable m_connectionHTTPHeaders = null;
	/** request messages whose parsing hasn't finished yet */
	private Hashtable m_unfinishedRequests = null;
	/** response messages whose parsing hasn't finished yet */
	private Hashtable m_unfinishedResponses = null;
	
	private static final IHTTPHelper UPSTREAM_HELPER = new IHTTPHelper()
	{
		public byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header)
		{
			a_callback.handleRequest(a_header);
			return a_header.dumpRequestHeader();
		}
	};
	private static final IHTTPHelper DOWNSTREAM_HELPER = new IHTTPHelper()
	{
		public byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header)
		{
			a_callback.handleResponse(a_header);
			return a_header.dumpResponseHeader();
		}
	};
	
	public HTTPProxyCallback()
	{
		m_connectionHTTPHeaders = new Hashtable();
		m_unfinishedRequests = new Hashtable();
		m_unfinishedResponses = new Hashtable();
	}
	
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		return handleStreamChunk(anonRequest, chunk, len, MESSAGE_TYPE_REQUEST, UPSTREAM_HELPER);
		//return chunk;
	}
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		return handleStreamChunk(anonRequest, chunk, len, MESSAGE_TYPE_RESPONSE, DOWNSTREAM_HELPER);
		//return chunk;
	}

	private byte[] handleStreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len, 
			int a_messageType, IHTTPHelper a_helper)
	{
		
		/* Does only work with a valid AnonProxy reference */
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		Hashtable unfinishedMessages = 
			(a_messageType == MESSAGE_TYPE_REQUEST) ? 
				m_unfinishedRequests : m_unfinishedResponses;
		
		/* check if header parsing already started but hasn't finished yet for this AnonRequest */
		String unfinishedHeaderPart = (String) unfinishedMessages.get(anonRequest);
		String chunkData = new String(chunk, 0, len);
		chunkData = ((unfinishedHeaderPart != null) ? unfinishedHeaderPart : "") + chunkData;
		
		int content = (int) getLengthOfPayloadBytes(chunkData);
	
		if( hasAlignedHTTPStartLine(chunkData, a_messageType) )
		{
			boolean finished = extractHeaderParts(anonRequest, chunkData, a_messageType);
			if(!finished)
			{
				/* if header parsing hasn't finished yet:
				 * chunk can be delivered with delay by returning null for now and
				 * the rest later on.
				 */
				return null;
			}
			HTTPConnectionHeader connHeader;
			synchronized (m_connectionHTTPHeaders)
			{
				connHeader = (HTTPConnectionHeader) m_connectionHTTPHeaders.get(anonRequest);
			}
						
			if(connHeader != null)
			{
				String request_line = connHeader.getRequestLine();
		
				boolean performMods = (request_line == null) ? false : !request_line.startsWith("CONNECT");
				if(performMods)
				{
					byte[] newHeaders = a_helper.dumpHeader(this, connHeader);
					byte[] newChunk = new byte[newHeaders.length+content];
					System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
					System.arraycopy(chunk, len-content, newChunk, newHeaders.length, content);
					return newChunk;
				}				
			}
		}
		return chunk;		
	}
	
	
	private interface IHTTPHelper
	{
		byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header);
	}
	
	public abstract void handleRequest(HTTPConnectionHeader connHeader);
	
	public abstract void handleResponse(HTTPConnectionHeader connHeader);
	
	/* 
	 * 	extract headers from data chunk
	 * returns false if header could not be extracted
	 */
	private boolean extractHeaderParts(AnonProxyRequest anonRequest, String chunkData, int messageType)
	{
		// assumes, that the chunk is aligned.
		//Works in almost every case.
		
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		HTTPConnectionHeader connHeader = null;
		
		synchronized(m_connectionHTTPHeaders)
		{
			connHeader = (HTTPConnectionHeader) 
				m_connectionHTTPHeaders.get(anonRequest);
			
			if( (connHeader != null) )
			{
				/* old http messages already delivered by this AnonProxyRequest-Thread can be removed */ 
				if((messageType == MESSAGE_TYPE_REQUEST) && connHeader.isRequestFinished())
				{
					connHeader.clearRequest();
				}
				else if ((messageType == MESSAGE_TYPE_RESPONSE) && connHeader.isResponseFinished())
				{
					connHeader.clearResponse();
				}
			}
			
			if ( connHeader == null )
			{
				connHeader = new HTTPConnectionHeader();
				m_connectionHTTPHeaders.put(anonRequest, connHeader);
			}
		}
		
		if(hasAlignedHTTPStartLine(chunkData, messageType))
		{
			Hashtable unfinishedMessages = 
				(messageType == MESSAGE_TYPE_REQUEST) ? 
					m_unfinishedRequests : m_unfinishedResponses;
			int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);

			if((off_headers_end != -1))
			{
				//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
				parseHTTPHeader(chunkData.substring(0, off_headers_end), connHeader, messageType);
				if(messageType == MESSAGE_TYPE_REQUEST)
				{
					connHeader.setRequestFinished(true);
				}
				else if (messageType == MESSAGE_TYPE_RESPONSE)
				{
					connHeader.setResponseFinished(true);
				}
				unfinishedMessages.remove(anonRequest);
				return true;
			}
			else 
			{
				unfinishedMessages.put(anonRequest, chunkData);
				return false;
			}
		}
		return false;
	}
	
	private boolean hasAlignedHTTPStartLine(String chunkData, int messageType)
	{
		return (messageType == MESSAGE_TYPE_REQUEST) ? isRequest(chunkData) : isResponse(chunkData);
	}
	
	private boolean isRequest(String chunkData)
	{
		for (int i = 0; i < HTTP_REQUEST_METHODS.length; i++) {
			if( chunkData.startsWith(HTTP_REQUEST_METHODS[i]) )
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isResponse(String chunkData)
	{
		return chunkData.startsWith(HTTP_VERSION_PREFIX);
	}
	
	protected long getLengthOfPayloadBytes(String chunkData)
	{	
		int off_firstline= chunkData.indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1) )
		{
			if(off_headers_end == -1)
			{
				return 0l;
			}
			return (long) (chunkData.length() - (off_headers_end+HTTP_HEADER_END.length())); 
		}
		return (long) chunkData.length();
	}
	
	private static void parseHTTPHeader(String headerData, HTTPConnectionHeader connHeader, int headerType)
	{
		StringTokenizer lineTokenizer = new StringTokenizer(headerData,CRLF);
		if(lineTokenizer.countTokens() == 0)
		{
			return;
		}
		String header = null;
		String key = null;
		String value = null;
		if(headerType == MESSAGE_TYPE_REQUEST)
		{
			connHeader.setRequestHeader(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
		}
		else if (headerType == MESSAGE_TYPE_RESPONSE)
		{
			connHeader.setResponseHeader(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
		}
		while(lineTokenizer.hasMoreTokens())
		{
			header = lineTokenizer.nextToken();
			int delim = header.indexOf(HTTP_HEADER_DELIM);
			if(delim != -1)
			{
				key = header.substring(0, delim).trim();
				if(delim+1 < header.length())
				{
					value = header.substring(delim+1).trim();
				}
				if( (key != null) && (value != null) )
				{
					if(headerType == MESSAGE_TYPE_REQUEST)
					{
						connHeader.setRequestHeader(key, value);
					}
					else if (headerType == MESSAGE_TYPE_RESPONSE)
					{
						connHeader.setResponseHeader(key, value);
					}
				}
			}
		}
	}
	
	protected final class HTTPConnectionHeader
	{
		private Hashtable reqHeaders = new Hashtable();
		private Hashtable resHeaders = new Hashtable();
		
		private Vector reqHeaderOrder = new Vector();
		private Vector resHeaderOrder = new Vector();
		
		private boolean requestFinished = false;
		private boolean responseFinished = false;
		
		public synchronized boolean isResponseFinished() 
		{
			return responseFinished;
		}

		public synchronized void setResponseFinished(boolean responseFinished) 
		{
			this.responseFinished = responseFinished;
		}

		public synchronized boolean isRequestFinished() 
		{
			return requestFinished;
		}

		public synchronized void setRequestFinished(boolean finished) 
		{
			this.requestFinished = finished;
		}

		/*
		 * methods for checking or modifying HTTP message headers
		 */
		protected synchronized void setRequestHeader(String header, String value)
		{
			setHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		protected synchronized void setResponseHeader(String header, String value)
		{
			setHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		protected synchronized void replaceRequestHeader(String header, String value)
		{
			replaceHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		protected synchronized void replaceResponseHeader(String header, String value)
		{
			replaceHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		protected synchronized String getRequestLine()
		{
			return getStartLine(reqHeaders);
		}
		
		protected synchronized String getResponseLine()
		{
			return getStartLine(resHeaders);
		}
		
		protected synchronized String[] getRequestHeader(String header)
		{
			return getHeader(reqHeaders, header);
		}
		
		protected synchronized String[] getResponseHeader(String header)
		{
			return getHeader(resHeaders, header);
		}
		
		protected synchronized String[] removeRequestHeader(String header)
		{
			return removeHeader(reqHeaders, reqHeaderOrder, header);
		}
		
		protected synchronized String[] removeResponseHeader(String header)
		{
			return removeHeader(resHeaders, resHeaderOrder, header);
		}
		
		protected synchronized void clearRequest()
		{
			clearHeader(reqHeaders, reqHeaderOrder);
		}
		
		protected synchronized void clearResponse()
		{
			clearHeader(resHeaders, resHeaderOrder);
		}
		
		/* private util-fucntion area. All of these functions are not thread safe and are only to be accessed  
		 * synchronized by the actual ConnectionHeader object
		 */
		private void setHeader(Hashtable headerMap, Vector headerOrder, String header, String value)
		{
			Vector valueContainer = (Vector) headerMap.get(header.toLowerCase());
			if(valueContainer == null)
			{
				/* it's possible that a header was removed but is still in the order list. 
				 * because when a header is removed, it is not deleted from there.
				 * this is convenient for replacing headers but can cause side effects.
				 * (Removing and the setting a header will set the header in the place where 
				 * it first was). 
				 */
				boolean addToOrder = true;
				for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements();)
				{
					String aktheader = (String) enumeration.nextElement();
					if(aktheader.equalsIgnoreCase(header))
					{
						addToOrder = false;
					}
				}
				if(addToOrder)
				{
					headerOrder.addElement(header);
				}
				valueContainer = new Vector();
			}
			valueContainer.addElement(value);
			headerMap.put(header.toLowerCase(), valueContainer);
		}
		private void replaceHeader(Hashtable headerMap, Vector headerOrder, String header, String value)
		{
			removeHeader(headerMap, headerOrder, header);
			setHeader(headerMap, headerOrder, header, value);
		}
		
		private String[] getHeader(Hashtable headerMap, String header)
		{
			Vector valueContainer = (Vector) headerMap.get(header.toLowerCase());
			return valuesToArray(valueContainer);
		}
		
		private String[] removeHeader(Hashtable headerMap, Vector headerOrder, String header)
		{
			/*for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements();)
			{
				String aktheader = (String) enumeration.nextElement();
				if(aktheader.equalsIgnoreCase(header))
				{
					headerOrder.remove(aktheader);
				}
			}*/
			/* header is not removed from the order list: beware of side-effects */
			Vector valueContainer = (Vector) headerMap.remove(header.toLowerCase());
			return valuesToArray(valueContainer);
		}
		
		private void clearHeader(Hashtable headerMap, Vector headerOrder)
		{
			headerMap.clear();
			headerOrder.removeAllElements();
		}
		
		private String getStartLine(Hashtable headerMap)
		{
			Vector valueContainer = (Vector) headerMap.get(HTTP_START_LINE_KEY.toLowerCase());
			String[] startlineRet = valuesToArray(valueContainer);
			if (startlineRet == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Invalid request because it contains no startline");
				new Exception("DFg").printStackTrace();
				return null;
			}
			if(startlineRet.length > 1)
			{
				String errOutput = "";
				for (int i = 0; i < startlineRet.length; i++) 
				{
					errOutput+= startlineRet[i]+"\n";
				}
				LogHolder.log(LogLevel.ERR, LogType.NET, 
						"This HTTP message seems to be invalid, because it has multiple start lines:\n"
					+errOutput);
			}
			return startlineRet[0];
		}
		
		private String[] valuesToArray(Vector valueContainer)
		{
			if(valueContainer == null)
			{
				return null;
			}
			int valueCount = valueContainer.size();
			if(valueCount == 0)
			{
				return null;
			}
			String[] values = new String[valueCount];
			Enumeration enumeration = valueContainer.elements();
			for(int i = 0; enumeration.hasMoreElements(); i++)
			{
				values[i] = (String) enumeration.nextElement();
			}
			return values;
		}
		
		private byte[] dumpRequestHeader()
		{
			return dumpHeader(reqHeaders, reqHeaderOrder);
		}
		
		private byte[] dumpResponseHeader()
		{
			return dumpHeader(resHeaders, resHeaderOrder);
		}
		
		private byte[] dumpHeader(Hashtable headerMap, Vector headerOrder)
		{
			String allHeaders = "";
			String header = null;

			for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements(); )
			{
				header = (String) enumeration.nextElement();
				if(header.equalsIgnoreCase(HTTP_START_LINE_KEY))
				{
					if(!allHeaders.equals(""))
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, "HTTP startline set after Message-Header. " +
								"This is a Bug. please report this.");
						throw new  IllegalStateException("HTTP startline set after Message-Header. " +
								"This is a Bug. please report this.");
					}
					allHeaders += getStartLine(headerMap)+CRLF;
				}
				else
				{
					String[] values = getHeader(headerMap, header);
					if(values != null)
					{
						for (int i = 0; i < values.length; i++) 
						{
							allHeaders += header+": "+values[i]+CRLF;
						}
					}
				}
			}
			allHeaders += CRLF;
			LogHolder.log(LogLevel.INFO, LogType.NET, Thread.currentThread().getName()+": header dump:\n"+allHeaders);
			return allHeaders.getBytes();
		}
		
	}
}

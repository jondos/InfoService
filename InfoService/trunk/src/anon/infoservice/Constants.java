/*
 Copyright (c) 2000 - 2003, The JAP-Team
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

import java.util.Locale;

final public class Constants
{
  public static final String DEFAULT_RESSOURCE_FILENAME = "InfoService.properties";

  public static final String CRLF = "\r\n";
  public static final String TYPE = "Content-type: ";
  public static final String LENGTH = "Content-length: ";
  public static final String LOCATION = "Location: ";
  public static final String DATE = "Date: ";
  public static final String EXPIRES = "Expires: ";
  public static final String CACHE_CONTROL = "Cache-Control: ";
  public static final String PRAGMA = "Pragma: ";

  public static final String OCTET_STREAM = "application/octet-stream";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_HTML = "text/html";
  public static final String TEXT_XML = "text/xml";
  public static final String APPLICATION_JNLP = "application/x-java-jnlp-file";
  public static final String OK = "HTTP/1.1 200 OK";
  public static final String MOVED = "HTTP/1.1 301 Moved Permanently";
  public static final String ERROR = "HTTP/1.1 400 Bad Request";
  public static final String NOTFOUND = "HTTP/1.1 404 Not Found";
  public static final String HTML_NOTFOUND = "<HTML><TITLE>404 File Not Found</TITLE><H1>404 File Not Found</H1><P>File not found on this server.</P></HTML>";
  public static final String HTML_INVALID_REQUEST = "<HTML><TITLE>400 Bad Request</TITLE><H1>400 Bad Request</H1><P>Your request has been rejected by the server.</P></HTML>";
  public static final String HTML_HASMOVED = "<HTML><TITLE>301 Moved Permanently</TITLE><H1>301 Moved Permanently</H1><P>The document has moved <A HREF=\"%\">here</A>.</P></HTML>";
  public static final String CERTSPATH = "certificates/";
  public static final String CERT_JAPINFOSERVICEMESSAGES = "japinfoservicemessages.cer";;
  public static final int MAX_REQUEST_HEADER_SIZE = 10000;
  public static final int REQUEST_METHOD_UNKNOWN = -1;
  public static final int REQUEST_METHOD_GET = 1;
  public static final int REQUEST_METHOD_POST = 2;
  public static final int REQUEST_METHOD_HEAD = 3;

  public static final int MAX_NR_OF_CONCURRENT_CONNECTIONS = 50;

  /* don't remove the (long)-casts because the standard is only an int and so it wouldn't work
   * with bigger values
   */

  /**
   * The standard timeout for infoservice database entries in a JAP client. Is only used
   * if no expire time is received from the infoservice.
   */
  public static final long TIMEOUT_INFOSERVICE_JAP = 30 * 24 * 3600 * (long) (1000); // 30 days
  /**
   * The standard timeout for infoservice database entries in an infoservice.
   */
  public static final long TIMEOUT_INFOSERVICE = 11 * 60 * (long) (1000); // 11 minutes
  public static final long TIMEOUT_MIX = 11 * 60 * (long) (1000); // 11 minutes
  public static final long TIMEOUT_MIXCASCADE = 11 * 60 * (long) (1000); // 11 minutes
  public static final long TIMEOUT_STATUS = 100 * (long) (1000); // 100 seconds

  /**
   * The timeout for all entries in the database of JAP forwarders. If we don't get a new  update
   * message from the forwarder within that time, it is removed from the database. The default is
   * 11 minutes, so there is no problem, if the forwarder updates the entry every 10 minutes.
   */
  public static final long TIMEOUT_JAP_FORWARDERS = 11 * 60 * (long) 1000;

  /**
   * This is the timeout in seconds for verifying a JAP forwarding server (contacting the server
   * and getting the acknowledgement, that it is a JAP forwarder). If we can't get the
   * verification within that time, the JAP forwarding server is declared as invalid.
   */
  public static final int FORWARDING_SERVER_VERIFY_TIMEOUT = 20;

  /**
   * This is the general timeout for the Infoservice socket communication (milli seconds).
   */
   public static final int	COMMUNICATION_TIMEOUT=30000; //30 seconds

  public static final long ANNOUNCE_PERIOD = 10 * 60 * (long) (1000); // 10 minutes

  public static final long UPDATE_INFORMATION_ANNOUNCE_PERIOD = 10 * 60 * (long) (1000); // 10 minutes

  /**
   * We use this for display some values in the local format.
   */
  public static final Locale LOCAL_FORMAT = Locale.GERMAN;

  /**
   * This is the version number of the infoservice software.
   */
  public static final String INFOSERVICE_VERSION = "IS.06.037"; //never change the layout of this line!


}

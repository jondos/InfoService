/*
 Copyright (c) 2000, The JAP-Team
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

import java.awt.Font;
import java.awt.Insets;

public final class JAPConstants
{
	public static final String aktVersion = "00.02.061"; //Never change the layout of this line!
	private static final String CVS_GENERATED_RELEASE_DATE = "$Date$";

	//Warning: This is a little bit tricky,
	//because CVS will expand the $Date$
	//to the date of the last commmit of this file

	public final static boolean m_bReleasedVersion = false; //Set to true if this is a stable (release) Version
	private static final String RELEASE_DATE = "2003/08/27 14:00:0"; // Set only to a Value, if m_bReleaseVersion=true

	public static final String strReleaseDate; //The Release date of this version

	//display in some information dialog and in
	//the update dialog
	static
	{ //This will set the strRealeaseDate to the correct Value
		//This is ether the CVS_GENERATED_RELEAS_DATE or the RELEASE_DATE, if m_bReleasedVersion==true;
		if (m_bReleasedVersion)
		{
			strReleaseDate = RELEASE_DATE;
		}
		else
		{
			strReleaseDate = CVS_GENERATED_RELEASE_DATE.substring(7, 26);
		}
	}

	//static final String buildDate=".."
	//static final String buildType="..."
	//needed for update.JAPUpdate
	static final int defaultPortNumber = 4001;
	static final int defaultSOCKSPortNumber=1080;
	static final String defaultAnonName = "Dresden-Desden";
	static final String defaultAnonID = "141.76.1.120%3A6544";
	static final String defaultAnonHost = "mix.inf.tu-dresden.de";
	static final String defaultAnonIP = "141.76.1.120"; //only used for fallback,

	//if DNS could not get IP for
	// defaultAnonHost
	static final int defaultAnonPortNumber = 6544;
	/**
	 * The name of the default infoservice.
	 */
	public static final String defaultInfoServiceName = "JAP-Team InfoService";
	public static final String defaultInfoServiceID = "infoservice.inf.tu-dresden.de%3A80";
	public static final String defaultInfoServiceHostName = "infoservice.inf.tu-dresden.de";
	public static final int defaultInfoServicePortNumber = 6543;

	/**
	 * This defines, whether automatic infoservice request are disabled as default.
	 */
	public static final boolean DEFAULT_INFOSERVICE_DISABLED = false;

	/**
	 * This defines, whether there is an automatic change of infoservice after failure as default.
	 */
	public static final boolean DEFAULT_INFOSERVICE_CHANGES = true;

	/**
	 * This defines the timeout for infoservice communication (connections to the update server
	 * have also this timeout because of the same HTTPConnectionFactory).
	 */
	public static final int DEFAULT_INFOSERVICE_TIMEOUT = 10;

	public static final int FIREWALL_TYPE_HTTP = 1;
	public static final int FIREWALL_TYPE_SOCKS = 2;
	static final int defaultFirewallType = FIREWALL_TYPE_HTTP;

	static final int SMALL_FONT_SIZE = 9;
	static final int SMALL_FONT_STYLE = Font.PLAIN;
	static final Insets SMALL_BUTTON_MARGIN = new Insets(1, 1, 1, 1);

	static final String JAPLocalFilename = "JAP.jar";
	static final int MAXHELPLANGUAGES = 6;
	public static final String TITLE = "JAP";
	public static final String TITLEOFICONIFIEDVIEW = "JAP";
	static final String AUTHOR = "(c) 2000 The JAP-Team";
	static final String IMGPATHHICOLOR = "images/";
	static final String IMGPATHLOWCOLOR = "images/lowcolor/";
	static final String XMLCONFFN = "jap.conf";
	static final String MESSAGESFN = "JAPMessages";
	static final String BUSYFN = "busy.gif";
	static final String SPLASHFN = "splash.gif";
	static final String ABOUTFN = "info.gif";
	public static final String DOWNLOADFN = "install.gif";
	static final String IICON16FN = "icon16.gif";
	static final String ICONFN = "icon.gif";

	//static final String   JAPTXTFN                     = "japtxt.gif";
	static final String JAPEYEFN = "japeye.gif";
	static final String JAPICONFN = "japi.gif";

	//static final String   CONFIGICONFN                 = "icoc.gif";
	static final String ICONIFYICONFN = "iconify.gif";
	static final String ENLARGEYICONFN = "enlarge.gif";
	static final String METERICONFN = "icom.gif";
	public static final String IMAGE_ARROW = "arrow46.gif";
	public static final String IMAGE_BLANK = "blank.gif";
	public static final String IMAGE_STEPFINISHED = "haken.gif";
	public static final String IMAGE_ARROW_DOWN = "arrowDown.gif";
	public static final String IMAGE_ARROW_UP = "arrowUp.gif";
	public static final String CERTENABLEDICON = "cenabled.gif";
	public static final String CERTDISABLEDICON = "cdisabled.gif";

	static final String[] METERFNARRAY =
		{
		"meterD.gif", // anonymity deactivated
		"meterNnew.gif", // no measure available
		"meter1.gif",
		"meter2.gif",
		"meter3.gif",
		"meter4.gif",
		"meter5.gif",
		"meter6.gif"
	};
	public static final String PIHOST = "anon.inf.tu-dresden.de";
	public static final int PIPORT = 2342;
	public static final boolean PI_SSLON = false; // auf true setzten wenn die pay funktionalität fertig gebaut ist.

	public final static String CERTSPATH = "certificates/";
	public final static String TRUSTEDROOTCERT = "japroot.cer";
	public final static String CERT_JAPCODESIGNING = "japcodesigning.cer";
	public final static String CERT_JAPINFOSERVICEMESSAGES = "japinfoservicemessages.cer";

	/** Tor related defaults **/
	public final static boolean TOR_IS_ENABLED=false;
	public final static String TOR_DIR_SERVER_ADR="moria.seul.org";
	public final static int TOR_DIR_SERVER_PORT=9031;
}

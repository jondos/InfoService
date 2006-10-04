/*
 Copyright (c) 2000-2006, The JAP-Team
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

import anon.tor.Circuit;
import anon.tor.Tor;
import anon.mixminion.Mixminion;

public final class JAPConstants
{
	public static final String aktVersion = "00.05.458"; //Never change the layout of this line!
	private static final String CVS_GENERATED_RELEASE_DATE = "$Date$";

	//Warning: This is a little bit tricky,
	//because CVS will expand the $Date$
	//to the date of the last commmit of this file

	public final static boolean m_bReleasedVersion = false; //Set to true if this is a stable (release) Version
	private static final String RELEASE_DATE = "2005/08/14 15:22:00"; // Set only to a Value, if m_bReleaseVersion=true

	public static final String CURRENT_CONFIG_VERSION = "0.24";

	public static final String strReleaseDate; //The Release date of this version

	//display in some information dialog and in
	//the update dialog
	static
	{ //This will set the strRealeaseDate to the correct Value
		//This is ether the CVS_GENERATED_RELEASE_DATE or the RELEASE_DATE, if m_bReleasedVersion==true;
		if (m_bReleasedVersion)
		{
			strReleaseDate = RELEASE_DATE;
		}
		else
		{
			strReleaseDate = CVS_GENERATED_RELEASE_DATE.substring(7, 26);
		}
	}

	static final int DEFAULT_PORT_NUMBER = 4001;
	static final boolean DEFAULT_LISTENER_IS_LOCAL = true;
	static final String DEFAULT_ANON_NAME = "Dresden-Dresden";
	static final String[] DEFAULT_ANON_MIX_IDs = new String[]{"141.76.45.36%3A6544", "141.76.45.36%3A6544"};
	static final String[] DEFAULT_ANON_HOSTS = {"mix.inf.tu-dresden.de", "141.76.45.33"};

	static final int DEFAULT_ANON_PORT_NUMBERS[] =
		{
		22, 80, 443, 6544};
	/**
	 * The name of the default infoservice.
	 */
	public static final String DEFAULT_INFOSERVICE_NAMES[] =
		new String[]{"JAP-Team InfoService", "IS Uni Regensburg", "IS HostEurope"};
	public static final String DEFAULT_INFOSERVICE_HOSTNAMES[] =
		new String[]{"infoservice.inf.tu-dresden.de", "132.199.134.2", "87.230.9.148"};
	public static final int DEFAULT_INFOSERVICE_PORT_NUMBERS[][] =
		{{80, 6543}, {80, 6543}, {80}};

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
	public static final int DEFAULT_INFOSERVICE_TIMEOUT = 30;

	public static final boolean DEFAULT_ALLOW_INFOSERVICE_NON_ANONYMOUS_CONNECTION = true;
	public static final boolean DEFAULT_ALLOW_PAYMENT_NON_ANONYMOUS_CONNECTION = true;
	public static final boolean DEFAULT_ALLOW_UPDATE_NON_ANONYMOUS_CONNECTION = true;

	public static final boolean REMIND_OPTIONAL_UPDATE = true;
	public static final boolean REMIND_JAVA_UPDATE = true;

	static final int SMALL_FONT_SIZE = 9;
	static final int SMALL_FONT_STYLE = Font.PLAIN;

	public static final int VIEW_NORMAL = 1;
	public static final int VIEW_SIMPLIFIED = 2;
	static final int DEFAULT_VIEW = VIEW_NORMAL;

	static final boolean DEFAULT_SAVE_MAIN_WINDOW_POSITION = true;
	static final boolean DEFAULT_SAVE_MINI_WINDOW_POSITION = false;
	static final boolean DEFAULT_SAVE_CONFIG_WINDOW_POSITION = false;
	static final boolean DEFAULT_SAVE_HELP_WINDOW_POSITION = true;
	static final boolean DEFAULT_SAVE_HELP_WINDOW_SIZE = true;
	static final boolean DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP = false;
	static final boolean DEFAULT_MINIMIZE_ON_STARTUP = false;
	static final boolean DEFAULT_WARN_ON_CLOSE = true;

	static final String JAPLocalFilename = "JAP.jar";
	public static final String TITLE = "JAP";
	public static final String TITLEOFICONIFIEDVIEW = "JAP";
	static final String AUTHOR = "(c) 2000 The JAP-Team";
	public static final String XMLCONFFN = "jap.conf";
	public static final String MESSAGESFN = "JAPMessages";
	public static final String BUSYFN = "busy.gif";
	static final String ABOUTFN = "info.gif";
	public static final String DOWNLOADFN = "install.gif";
	static final String IICON16FN = "icon16.gif";
	static final String ICONFN = "icon.gif";


	//static final String   CONFIGICONFN                 = "icoc.gif";
	static final String ICONIFYICONFN = "iconify.gif";
	static final String ENLARGEYICONFN = "enlarge.gif";
	static final String METERICONFN = "icom.gif";
	public static final String IMAGE_ARROW = "arrow46.gif";
	public static final String IMAGE_BLANK = "blank.gif";
	public static final String IMAGE_STEPFINISHED = "haken.gif";
	public static final String IMAGE_ARROW_DOWN = "arrowDown.gif";
	public static final String IMAGE_ARROW_UP = "arrowUp.gif";
	public static final String IMAGE_SERVER = "server.gif";
	public static final String IMAGE_SERVER_BLAU = "server_blau.gif";
	public static final String IMAGE_SERVER_ROT = "server_rot.gif";
	public static final String IMAGE_RELOAD = "reload.gif";
	public static final String IMAGE_RELOAD_DISABLED = "reloaddisabled_anim.gif";
	public static final String IMAGE_RELOAD_ROLLOVER = "reloadrollover.gif";
	public static final String IMAGE_WARNING = "warning.gif";
	public static final String IMAGE_INFORMATION = "information.gif";
	public static final String IMAGE_ERROR = "error.gif";
	public static final String IMAGE_CASCADE_MANUELL = "servermanuell.gif";
	public static final String IMAGE_CASCADE_NOT_CERTIFIED = "servermanuell.gif";
	public static final String IMAGE_CASCADE_PAYMENT = "serverwithpayment.gif";
	public static final String IMAGE_CASCADE_INTERNET = "serverfrominternet.gif";
	public static final String IMAGE_INFOSERVICE_MANUELL = "infoservicemanuell.gif";
	public static final String IMAGE_INFOSERVICE_INTERNET = "infoservicefrominternet.gif";
	public static final String IMAGE_INFOSERVICE_BIGLOGO = "infoservicebiglogo.gif";

	public static final String IMAGE_SAVE = "saveicon.gif";
	public static final String IMAGE_EXIT = "exiticon.gif";
	public static final String IMAGE_DELETE = "deleteicon.gif";
	public static final String IMAGE_COPY = "copyicon.gif";
	public static final String IMAGE_COPY_CONFIG = "copyintoicon.gif";

	public static final String CERTENABLEDICON = "cenabled.gif";
	public static final String CERTDISABLEDICON = "cdisabled.gif";

	public static final String IMAGE_COINS_FULL = "coins-full.gif";
	public static final String IMAGE_COINS_QUITEFULL = "coins-quitefull.gif";
	public static final String IMAGE_COINS_MEDIUM = "coins-medium.gif";
	public static final String IMAGE_COINS_LOW = "coins-low.gif";
	public static final String IMAGE_COINS_EMPTY = "coins-empty.gif";
	public static final String IMAGE_COIN_COINSTACK = "coinstack.gif";

// Bastian Voigt: Icons for the account meter
	public static final String[] ACCOUNTICONFNARRAY =
		{
		"accountDisabled.gif", "accountOk.gif", "accountBroken.gif"
	};

	// Bastian Voigt: Browser executable names for the browserstart hack
	public static final String[] BROWSERLIST =
		{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox", "firebird", "opera"
	};

	public final static String PI_ID = "PIjapteam";
	public final static String PI_NAME = "JAP Team PI";
	public final static String PI_HOST = "anon.inf.tu-dresden.de";
	public final static int PI_PORT = 2342;
	public final static String PI_CERT = "bi.cer";
	public final static String CERTSPATH = "certificates/";
	public final static String INFOSERVICE_CERTSPATH = "acceptedInfoServiceCAs/";
	public final static String MIX_CERTSPATH = "acceptedMixCAs/";
	public final static String CERT_JAPCODESIGNING = "japcodesigning.cer";
	public final static String CERT_JAPINFOSERVICEMESSAGES = "japupdatemessages.cer";
	public static final boolean DEFAULT_CERT_CHECK_ENABLED = true;

	public final static int TOR_MAX_CONNECTIONS_PER_ROUTE = Circuit.MAX_STREAMS_OVER_CIRCUIT;
	public final static int TOR_MAX_ROUTE_LEN = Tor.MAX_ROUTE_LEN;
	public final static int TOR_MIN_ROUTE_LEN = Tor.MIN_ROUTE_LEN;
	public final static int MIXMINION_MAX_ROUTE_LEN = Mixminion.MAX_ROUTE_LEN;
	public final static int MIXMINION_MIN_ROUTE_LEN = Mixminion.MIN_ROUTE_LEN;
	public final static boolean DEFAULT_TOR_PRECREATE_ROUTES = false;
	public final static int DEFAULT_TOR_MIN_ROUTE_LEN = Tor.MIN_ROUTE_LEN;
	public final static int DEFAULT_TOR_MAX_ROUTE_LEN = Tor.MIN_ROUTE_LEN + 1;
	public final static int DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE = Circuit.MAX_STREAMS_OVER_CIRCUIT;
	public final static int DEFAULT_MIXMINION_ROUTE_LEN = Mixminion.MIN_ROUTE_LEN;
	public final static String DEFAULT_MIXMINION_EMAIL = "";
	/**
	 * The minimum bandwidth per user needed for forwarding. This affects the maximum number
	 * of users, which can be forwarded with a specified bandwidth. The default is 2 KByte/sec
	 * for each user.
	 */
	public static final int ROUTING_BANDWIDTH_PER_USER = 4000;

	/**
	 * This is the mailaddress of the InfoService mailgateway.
	 */
	public static final String MAIL_SYSTEM_ADDRESS = "japmailsystem@infoservice.inf.tu-dresden.de";

	//japconfig
	public final static String CONFIG_VERSION = "version";
	public final static String CONFIG_PORT_NUMBER = "portNumber";
	public final static String CONFIG_LISTENER_IS_LOCAL = "listenerIsLocal";
	public final static String CONFIG_NEVER_REMIND_ACTIVE_CONTENT = "neverRemindActiveContent";
	public final static String CONFIG_NEVER_EXPLAIN_FORWARD = "neverExplainForward";
	public final static String CONFIG_NEVER_ASK_PAYMENT = "neverAskPayment";
	public final static String CONFIG_DO_NOT_ABUSE_REMINDER = "doNotAbuseReminder";
	public final static String CONFIG_NEVER_REMIND_GOODBYE = "neverRemindGoodBye";
	public final static String CONFIG_INFOSERVICE_DISABLED = "infoServiceDisabled";
	public final static String CONFIG_INFOSERVICE_TIMEOUT = "infoserviceTimeout";
	public final static String CONFIG_PROXY_HOST_NAME = "proxyHostName";
	public final static String CONFIG_PROXY_PORT_NUMBER = "proxyPortNumber";
	public final static String CONFIG_PROXY_TYPE = "proxyType";
	public final static String CONFIG_PROXY_AUTH_USER_ID = "proxyAuthUserID";
	public final static String CONFIG_PROXY_AUTHORIZATION = "proxyAuthorization";
	public final static String CONFIG_PROXY_MODE = "proxyMode";
	public final static String CONFIG_DUMMY_TRAFFIC_INTERVALL = "DummyTrafficInterval";
	public final static String CONFIG_AUTO_CONNECT = "autoconnect";
	public final static String CONFIG_AUTO_RECONNECT = "autoReconnect";
	public final static String CONFIG_MINIMIZED_STARTUP = "minimizedStartup";
	public final static String CONFIG_LOCALE = "Locale";
	public final static String CONFIG_LOOK_AND_FEEL = "LookAndFeel";
	public final static String CONFIG_UNKNOWN = "unknown";
	public final static String CONFIG_GUI = "GUI";
	public final static String CONFIG_LOG_DETAIL = "Detail";
	public final static String CONFIG_MAIN_WINDOW = "MainWindow";
	public final static String CONFIG_LOCATION = "Location";
	public final static String CONFIG_X = "x";
	public final static String CONFIG_Y = "y";
	public final static String CONFIG_DX = "dx";
	public final static String CONFIG_DY = "dy";
	//public final static String CONFIG_SIZE = "Size";
	public final static String CONFIG_MOVE_TO_SYSTRAY = "MoveToSystray";
	public final static String CONFIG_DEFAULT_VIEW = "DefaultView";
	public final static String CONFIG_NORMAL = "Normal";
	public final static String CONFIG_SIMPLIFIED = "Simplified";
	public final static String CONFIG_DEBUG = "Debug";
	public final static String CONFIG_LEVEL = "Level";
	public final static String CONFIG_TYPE = "Type";
	public final static String CONFIG_OUTPUT = "Output";
	public final static String CONFIG_CONSOLE = "Console";
	public final static String CONFIG_FILE = "File";
	public final static String CONFIG_TOR = "Tor";
	public final static String CONFIG_Mixminion = "Mixminion";
	public final static String CONFIG_MAX_CONNECTIONS_PER_ROUTE = "MaxConnectionsPerRoute";
	public final static String CONFIG_TOR_PRECREATE_ANON_ROUTES = "PreCreateAnonRoutes";
	public final static String CONFIG_ROUTE_LEN = "RouteLen";
	public final static String CONFIG_MIXMINION_REPLY_MAIL = "MixminionREPLYMail";
	public final static String CONFIG_MIN = "min";
	public final static String CONFIG_MAX = "max";
	public final static String CONFIG_PAYMENT = "Payment";
	public final static String CONFIG_PAYMENT_INSTANCES = "PaymentInstances";
	public final static String CONFIG_ENCRYPTED_DATA = "EncryptedData";
	public final static String CONFIG_PAY_ACCOUNTS_FILE = "PayAccountsFile";
	public final static String CONFIG_JAP_FORWARDING_SETTINGS = "JapForwardingSettings";

	/** Supported non-generic payment names. Comma-separated list. e.g. "CreditCard,DirectDebit"*/
	public final static String PAYMENT_NONGENERIC="CreditCard";
}

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

public class JAPConstants {
	static final String aktVersion = "00.01.046"; //Never change the layout of this line!
	//static final String buildDate=".."
	//static final String buildType="..."
        //needed for update.JAPUpdate
        public static String aktVersion2 = aktVersion;
        public final static boolean m_bReleasedVersion = false;
	static final int      defaultPortNumber            = 4001;
	static final String   defaultanonHost              = "mix.inf.tu-dresden.de";
	static final int      defaultanonPortNumber        = 6544;
	static final String   defaultinfoServiceHostName   = "infoservice.inf.tu-dresden.de";
	static final int      defaultinfoServicePortNumber = 6543;
	static final String   urlJAPNewVersionDownload     = "http://anon.inf.tu-dresden.de:80/~sk13/anon/jap/JAP.jar";
	static final String   JAPLocalFilename             = "JAP.jar";
	static final int      MAXHELPLANGUAGES             = 6;
	static final String   TITLE                        = "JAP";
	static final String   TITLEOFICONIFIEDVIEW         = "JAP";
	static final String   AUTHOR                       = "(c) 2000 The JAP-Team";
	static final String   IMGPATHHICOLOR               = "images/";
	static final String   IMGPATHLOWCOLOR              = "images/lowcolor/";
	static final String   XMLCONFFN                    = "jap.conf";
	static final String   MESSAGESFN                   = "JAPMessages";
	static final String   BUSYFN                       = "busy.gif";
	static final String   SPLASHFN                     = "splash.gif";
	static final String   ABOUTFN                      = "info.gif";
	static final String   DOWNLOADFN                   = "install.gif";
	static final String   IICON16FN                    = "icon16.gif";
	static final String   ICONFN                       = "icon.gif";
	static final String   JAPTXTFN                     = "japtxt.gif";
	static final String   JAPEYEFN                     = "japeye.gif";
	static final String   JAPICONFN                    = "japi.gif";
	static final String   CONFIGICONFN                 = "icoc.gif";
	static final String   ICONIFYICONFN                = "iconify.gif";
	static final String   ENLARGEYICONFN               = "enlarge.gif";
	static final String   METERICONFN                  = "icom.gif";
	static final String[] METERFNARRAY                 ={
															"meterD.gif",    // anonymity deactivated
															"meterNnew.gif", // no measure available
															"meter1.gif",
															"meter2.gif",
															"meter3.gif",
															"meter4.gif",
															"meter5.gif",
															"meter6.gif"
														};
}

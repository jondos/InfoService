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
package pay.data;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

public final class Literals
{

	public static final int SMALL_FONT_SIZE = 9;
	public static final int SMALL_FONT_STYLE = Font.PLAIN;
	public static final Insets SMALL_BUTTON_MARGIN = new Insets(0, 2, 0, 2);
	public static final Insets SMALL_TEXTFIELD_MARGIN = new Insets(2, 2, 2, 2);
	public static final int NORM_FONT_SIZE = 12;
	public static final Font NORM_FONT = new Font("Arial", Font.BOLD, 12);
	public static final Font NORM_FONT_BOLD = new Font("Arial", Font.BOLD, 12);
	public static final Color BACKGROUND = new Color(250, 250, 240);
	public static final Color BUTTON_BACKGROUND = Color.lightGray;
	public static final String TITLE = "PAY";
	public static final String AUTHOR = "(c) 2003 JAP-Team";
	public static final String MESSAGES = "PayText";
	public static final String BUSYFN = "busy.gif";
	public static final String SPLASHFN = "splash.gif";
	public static final String ABOUTFN = "info.gif";

	public static final String CHARGEPATH = "/pay/";
	public static final String PROTOKOLL = "http";
	public static final String CHARGEHOST = "anon.inf.tu-dresden.de";
	public static final String[] CHARGEPARAMS =
		{
		"transfernum", "lang"};
	public static final String CHARGEFILE = "index.php";

	public static final String[] BROWSERLIST =
	{
		"konqueror", "iexplore", "explorer", "mozilla", "firefox", "mozilla-firefox", "firebird", "opera"
	};

	public static final boolean SAVE_PW = false;
	public static final String RES_PATH = "pay/res/";
	public static final String ACCOUNT_FILE = "Account.dat";

	public static final String USER_FILE = "user.dat";

}

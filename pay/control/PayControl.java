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
package pay.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import jap.JAPConstants;
import jap.JAPModel;
import pay.Pay;
import pay.data.Literals;
import pay.PayAccountsFile;

/**
 * Diese Klasse stellt stellt zwei init Methoden bereits initPay() sollte dabei immer aufgerufen werden bevor mit der gesammten
 * Pay Api gearbeitet wird. hier werden die wichtigsten Klassen intitialisiert.
 * Au\uFFFDerdem findet heir ein Teil des Benutzerinteraktion statt. Der gr\uFFFD\uFFFDte Teil ist aber innerhalb von den Klassen im pay.view
 * Package und in PayAccountsControl. Es k\uFFFDnnte komplett hierher verschoben werden wenn ein klarere MVC Struktur erreicht werden sollte.
 *
 * @todo remove this class
 */

public class PayControl
{

	public static void initGui()
	{
		UIManager.put("Button.font", Literals.NORM_FONT);
		UIManager.put("Button.margin", Literals.SMALL_BUTTON_MARGIN);
		UIManager.put("Label.font", Literals.NORM_FONT);
		UIManager.put("Label.margin", Literals.SMALL_BUTTON_MARGIN);
		UIManager.put("TextField.font", Literals.NORM_FONT);
		UIManager.put("TextField.margin", Literals.SMALL_TEXTFIELD_MARGIN);
		UIManager.put("Panel.margin", Literals.SMALL_BUTTON_MARGIN);
		UIManager.put("ScrollPane.margin", Literals.SMALL_BUTTON_MARGIN);
	}

}

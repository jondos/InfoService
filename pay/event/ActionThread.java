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
package pay.event;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import jap.JAPWaitSplash;
import pay.util.Log;

public abstract class ActionThread
	implements Runnable, ActionListener
{
	protected ActionEvent event;
	protected Container container;
	protected String titel;
	public ActionThread(Container parent, String titel)
	{
		container = parent;
		this.titel = titel;
	}

	public ActionThread(Container parent)
	{
		this(parent, parent.toString());
	}

	public void actionPerformed(ActionEvent event)
	{
		Log.log(this, "Action Thread action performed ANFANG", Log.TEST);
		this.event = event;
		new Thread(this).start();
		Log.log(this, "Action Thread action performed ENDE", Log.TEST);

	}

	public void run()
	{
		Log.log(this, "Action Thread run() ANFANG", Log.TEST);
		//EasyProgressWindow pf = new EasyProgressWindow(container,titel);

		JAPWaitSplash splash = JAPWaitSplash.start("Work is in progress", titel);
		action(event);
		splash.abort();
		//pf = null;
		Log.log(this, "Action Thread run() ENDE", Log.TEST);
		//container.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public abstract void action(ActionEvent event);

}

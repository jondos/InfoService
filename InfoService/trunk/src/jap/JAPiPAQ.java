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
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.FontUIResource;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Insets;
import java.util.Enumeration;
import JAPModel;
import JAPDebug;
import JAPController;
import JAPConstants;
import JAPView;
import JAPMessages;
final public class JAPiPAQ {

 /* private class iPAQTheme extends DefaultMetalTheme
    {
      public FontUIResource getControlTextFont()
        {
          return new FontUIResource("Dialog",FontUIResource.PLAIN,9);
        }

      public FontUIResource getUserTextFont()
        {
          return new FontUIResource("Dialog",FontUIResource.PLAIN,9);
        }
      public FontUIResource getSystemTextFont()
        {
          return new FontUIResource("Dialog",FontUIResource.PLAIN,9);
        }
    }
*/
  public JAPiPAQ()
    {
    }

public void startJAP(String strJapConfFile) {
		JAPModel.create().setSmallDisplay(true);
    // Init Messages....
		JAPMessages.init();
		// Test (part 2) for right JVM....
		// Create the controller object
		JAPController controller = JAPController.create();
		// Create debugger object
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.WARNING);
		// load settings from config file
		controller.loadConfigFile(strJapConfFile);
		// Output some information about the system
		// Create the view object
		JAPView view = new JAPView(JAPConstants.TITLE);
		// Create the main frame
		view.create();
		// Switch Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		// Add observer
		controller.addJAPObserver(view);
		// Register the views where they are needed
		controller.registerView(view);
		// pre-initalize anon service
		anon.server.AnonServiceImpl.init();
    // initially start services
		controller.initialRun();
	}

  public JPanel getMainPanel()
    {
      return JAPController.getView().getMainPanel();
    }
	public static void main(String[] argv) {
    JAPiPAQ japOniPAQ = new JAPiPAQ();
		JAPModel.create().setSmallDisplay(true);
    japOniPAQ.startJAP(null);
    //Test
    JFrame frame=new JFrame("Test");
    frame.setContentPane(japOniPAQ.getMainPanel());
    frame.setSize(240,320);
    frame.show();
	}

}

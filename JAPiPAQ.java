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
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.FontUIResource;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import java.awt.Insets;
import java.util.Enumeration;
public class JAPiPAQ extends JAP {

  private class iPAQTheme extends DefaultMetalTheme
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

	JAPiPAQ(String[] argv) {
		super(argv);
		MetalLookAndFeel.setCurrentTheme(new iPAQTheme());
	  System.out.println(UIManager.getInsets("Button.margin"));
    UIManager.put("Button.margin",new Insets(1,1,1,1));
    //Enumeration enum=def.elements();
    //while(enum.hasMoreElements())
      //System.out.println(enum.nextElement());
  }


	public static void main(String[] argv) {
    JAPiPAQ japOniPAQ = new JAPiPAQ(argv);
		JAPModel.create().setSmallDisplay(true);
    japOniPAQ.startJAP();
	}

}

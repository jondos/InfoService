package infoservice.mailsystem.central.commands;

import infoservice.mailsystem.central.MailContext;
import infoservice.mailsystem.central.MailSystem;

import org.w3c.dom.Element;

import anon.infoservice.InfoServiceHolder;
import junit.framework.TestCase;
import junitx.framework.extension.XtendedPrivateTestCase;

public class GetForwarderCommandTest extends XtendedPrivateTestCase
	{
		public GetForwarderCommandTest(String a_name)
		{
			super(a_name);
		}

		public void testGetForwarder() throws Exception
			{
				/** 
				 * Move to setup...
				 */
				MailContext.createInstance("infoservice/mailsystem/central/commands/"+MailSystem.DEFAULT_CONFIG_FILE);
				
				Element forwarderEntry = InfoServiceHolder.getInstance().getForwarder();
				assertTrue(forwarderEntry!=null);
			}
	}

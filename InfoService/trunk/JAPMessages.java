import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.Frame;

public final class JAPMessages
	{
		private static final String MESSAGESFN   = "JAPMessages";
		private static ResourceBundle msg=null;
		
		
		private JAPMessages()
			{
			}
		
		public static void init()
			{
				// Load Texts for Messages and Windows
				try
						{
							msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() );
						}
					catch(Exception e1)
						{
							try
								{
									msg=ResourceBundle.getBundle(MESSAGESFN);
								}
							catch(Exception e)
								{
									JAPAWTMsgBox.MsgBox(new Frame(),
																			"File not found: "+MESSAGESFN+".properties\nYour package of JAP may be corrupted.\nTry again to download or install the package.",
																			"Error");
									System.exit(-1);
								}
						}
			}
		
		public static String getString(String key) 
			{
				try
					{
						return msg.getString(key);
					}
				catch(Exception e)
					{
						return key;
					}
			}
	}

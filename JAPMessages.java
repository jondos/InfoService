import java.util.ResourceBundle;
import java.util.PropertyResourceBundle;
import java.util.Locale;
import java.awt.Frame;

public final class JAPMessages
	{
		private static ResourceBundle msg=null;


		private JAPMessages()
			{
			}

		/* Initalize with the System default Locale...*/
		public static void init()
			{
				// Load Texts for Messages and Windows
				init(Locale.getDefault());
			}

		/* Init with the specified Locale**/
		public static void init(Locale locale)
			{
				// Load Texts for Messages and Windows
				try
						{
							msg = PropertyResourceBundle.getBundle(JAPConstants.MESSAGESFN, locale);
						}
					catch(Exception e1)
						{
							try
								{
									msg=PropertyResourceBundle.getBundle(JAPConstants.MESSAGESFN);
								}
							catch(Exception e)
								{
									JAPAWTMsgBox.MsgBox(new Frame(),
																			"File not found: "+JAPConstants.MESSAGESFN+".properties\nYour package of JAP may be corrupted.\nTry again to download or install the package.",
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

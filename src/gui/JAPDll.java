package gui;

final public class JAPDll
	{
		private static boolean m_sbHasOnTraffic=true;
		static
			{
				try
					{
						String osName=System.getProperty("os.name","").toLowerCase();
						if(osName.indexOf("win")>-1)
							System.loadLibrary("japdll");
					}
				catch(Throwable t)
					{
					}
			}

		static public boolean setWindowOnTop(String caption, boolean onTop)
			{
				try
					{
						setWindowOnTop_dll(caption,onTop);
						return true;
					}
				catch(Throwable t)
					{
					}
				return false;
			}

		static public boolean hideWindowInTaskbar(String caption)
			{
				try
					{
						return hideWindowInTaskbar_dll(caption);
					}
				catch(Throwable t)
					{
						return false;
					}
			}

		static public boolean onTraffic()
			{
				if(m_sbHasOnTraffic)
					try
						{
							onTraffic_dll();
							return true;
						}
					catch(Throwable t)
						{
							m_sbHasOnTraffic=false;
							return false;
						}
				return false;
			}

		static public String getDllVersion()
			{
				try
					{
						return getDllVersion_dll();
					}
				catch(Throwable t)
					{
					}
				return null;
			}

		native static private void setWindowOnTop_dll(String caption, boolean onTop);

		native static private boolean hideWindowInTaskbar_dll(String caption);

		native static private void onTraffic_dll();

		native static private String getDllVersion_dll();
}

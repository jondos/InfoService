package gui;

final public class JAPDll
	{
    static boolean m_bHaveOnTraffic=true;
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
						hideWindowInTaskbar_dll(caption);
						return true;
					}
				catch(Throwable t)
					{
						return false;
					}
			}

		static public boolean onTraffic()
			{
				try
					{
            if(!m_bHaveOnTraffic)
              return false;
            onTraffic_dll();
						return true;
					}
				catch(Throwable t)
					{
            m_bHaveOnTraffic=false;
					}
				return false;
      }

		native static private void setWindowOnTop_dll(String caption, boolean onTop);

		native static private void hideWindowInTaskbar_dll(String caption);

    native static private void onTraffic_dll();

}

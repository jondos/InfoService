package gui;

final public class JAPDll
	{
		static
			{
				try
					{
						System.loadLibrary("japdll");
					}
				catch(Throwable t)
					{
						t.printStackTrace();
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
						t.printStackTrace();
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
						t.printStackTrace();
						return false;
					}
			}


		native static private void setWindowOnTop_dll(String caption, boolean onTop);

		native static private void hideWindowInTaskbar_dll(String caption);

}

package jap;

class JAPConfAnonGeneral extends AbstractJAPConfModule
{
	protected JAPConfAnonGeneral(IJAPConfSavePoint savePoint)
	{
		super(null);
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("ngAnonGeneralPanelTitle");
	}

	public void recreateRootPanel()
	{
	}
}

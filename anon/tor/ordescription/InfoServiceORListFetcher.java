package anon.tor.ordescription;
import anon.infoservice.*;
final public class InfoServiceORListFetcher implements ORListFetcher
{
	public InfoServiceORListFetcher()
	{
	}

	public String getORList()
	{
		return InfoServiceHolder.getInstance().getTorNodesList();
	}
}

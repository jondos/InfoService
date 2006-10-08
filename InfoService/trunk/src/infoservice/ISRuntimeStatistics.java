package infoservice;

/**
 * This class collects some statistic information about the IS runtime.*/
final class ISRuntimeStatistics
{
	//How many TCP/IP Connections did we receive?
	static long ms_lTCPIPConnections=0;

	///How many get /mixcascadestatus command did we process?
	static long ms_lNrOfGetMixCascadeStatusRequests=0;

	static String getAsHTML()
	{
		StringBuffer sb=new StringBuffer();
		sb.append("TCP/IP Connections received: ");
		sb.append(ms_lTCPIPConnections);
		sb.append("<br>");
		sb.append("GET Requests for Cascade Status: ");
		sb.append(ms_lNrOfGetMixCascadeStatusRequests);
		sb.append("<br>");
		return sb.toString();
	}

}

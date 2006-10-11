package infoservice;

import anon.util.MyStringBuilder;

/**
 * This class collects some statistic information about the IS runtime.*/
final class ISRuntimeStatistics
{
	//How many TCP/IP Connections did we receive?
	static volatile long ms_lTCPIPConnections=0;

	///How many get /mixcascadestatus command did we process?
	static volatile long ms_lNrOfGetMixCascadeStatusRequests=0;

	static String getAsHTML()
	{
		MyStringBuilder sb=new MyStringBuilder(512);
		sb.append("TCP/IP Connections received: ");
		sb.append(ms_lTCPIPConnections);
		sb.append("<br>");
		sb.append("GET Requests for Cascade Status: ");
		sb.append(ms_lNrOfGetMixCascadeStatusRequests);
		sb.append("<br>");
		return sb.toString();
	}

}

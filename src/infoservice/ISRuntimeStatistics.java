package infoservice;

import anon.util.MyStringBuilder;
import java.text.NumberFormat;

/**
 * This class collects some statistic information about the IS runtime.*/
final class ISRuntimeStatistics
{
	//How many TCP/IP Connections did we receive?
	static volatile long ms_lTCPIPConnections=0;

	///How many get /mixcascadestatus command did we process?
	static volatile long ms_lNrOfGetMixCascadeStatusRequests=0;

	///How many get /tornodes command did we process?
	static volatile long ms_lNrOfGetTorNodesRequests=0;

	///How many get min jap version command did we process?
	static volatile long ms_lNrOfGetMinJapVersion=0;

	static NumberFormat ms_NumberFormat=NumberFormat.getInstance();

	static String getAsHTML()
	{
		MyStringBuilder sb=new MyStringBuilder(512);
		sb.append("<table>");
		sb.append("<tr><td>TCP/IP Connections received: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lTCPIPConnections));
		sb.append("</td></tr><tr><td>GET Requests for Cascade Status: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMixCascadeStatusRequests));
		sb.append("</td></tr><tr><td>GET Requests for Tor Nodes: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetTorNodesRequests));
		sb.append("</td></tr><tr><td>GET Requests for Min JAP Version: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMinJapVersion));
		sb.append("</td></tr><tr><td>Total Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().totalMemory()));
		sb.append("</td></tr><tr><td>Free Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().freeMemory()));
		sb.append("</td></tr></table>");
		return sb.toString();
	}

}

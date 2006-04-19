/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.infoservice;

import java.text.NumberFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.util.XMLUtil;

/**
 * Holds the information of a mixcascade status.
 */
public class StatusInfo extends AbstractDatabaseEntry implements IDistributable {

  /**
   * This is the ID of the mixcascade to which this status belongs.
   */
  private String m_mixCascadeId;

  /**
   * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this status
   * message.
   */
  private long m_lastUpdate;

  /**
   * Stores the number of active users in the corresponding mixcascade.
   */
  private int m_nrOfActiveUsers;

  /**
   * Stores the current risk for using this mix cascade. This is a value between 0 an 100 and it
   * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
   * client.
   */
  private int m_currentRisk;

  /**
   * Stores the current traffic situation for the mixcascade.
   */
  private int m_trafficSituation;

  /**
   * This is the number of packets, which are mixed through the cascade since their startup.
   */
  private long m_mixedPackets;

  /**
   * This is the calculated anonymity level (from number of active users, current traffic
   * and cascade length). It is a value between 0 and 5.
   */
  private int m_anonLevel;

  /**
   * Stores the XML description which we forward to other infoservices (the same as we have
   * received). This XML description is also used by recent versions of the JAP client
   * (>= 00.02.016) when fetching the status info. We are using a string representation here
   * because it is much faster if we don't need to process the XML tree everytime a client
   * requests the current status.
   */
  private String m_statusXmlData;

  /**
   * Returns a new StatusInfo with dummy values (everything is set to -1). The LastUpdate time is
   * set to the current system time. This function is used every time, we can't get the StatusInfo
   * from the infoservice of when a new MixCascade is constructed. This method is only used within
   * the context of the JAP client.
   *
   * @param a_mixCascadeId The ID of the MixCascade the StatusInfo belongs to.
   *
   * @return The new dummy StatusInfo.
   */
  public static StatusInfo createDummyStatusInfo(String a_mixCascadeId) {
    return (new StatusInfo(a_mixCascadeId, -1, -1, -1, -1, -1));
  }

  /**
   * Returns the name of the XML element corresponding to this class ("MixCascadeStatus").
   *
   * @return The name of the XML element corresponding to this class.
   */
  public static String getXmlElementName() {
    return "MixCascadeStatus";
  }


  /**
   * Creates a new StatusInfo from XML description (MixCascadeStatus node). There is no anonymity
   * level calculated for the new status entry -> getAnonLevel() will return -1. This constructor
   * should only be called within the context of the infoservice.
   *
   * @param a_statusNode The MixCascadeStatus node from an XML document.
   */
  public StatusInfo(Element a_statusNode) throws Exception {
    this(a_statusNode, -1);
  }

  /**
   * Creates a new StatusInfo from XML description (MixCascadeStatus node).
   *
   * @param a_statusNode The MixCascadeStatus node from an XML document.
   * @param a_mixCascadeLength The number of mixes in the mixcascade. We need this for
   *                           calculating the anonymity level. If this value is smaller than 0,
   *                           no anonymity level is calculated and getAnonLevel() will return
   *                           -1.
   */
  public StatusInfo(Element a_statusNode, int a_mixCascadeLength) throws Exception {
    /* use always the timeout for the infoservice context, because the JAP client currently does
     * not have a database of status entries -> no timeout for the JAP client necessary
     */
    super(System.currentTimeMillis() + Constants.TIMEOUT_STATUS);
    /* get all the attributes of MixCascadeStatus */
    m_mixCascadeId = a_statusNode.getAttribute("id");
    /* get the values */
    m_currentRisk = Integer.parseInt(a_statusNode.getAttribute("currentRisk"));
    m_mixedPackets = Long.parseLong(a_statusNode.getAttribute("mixedPackets"));
    m_nrOfActiveUsers = Integer.parseInt(a_statusNode.getAttribute("nrOfActiveUsers"));
    m_trafficSituation = Integer.parseInt(a_statusNode.getAttribute("trafficSituation"));
    m_lastUpdate = Long.parseLong(a_statusNode.getAttribute("LastUpdate"));
    /* calculate then anonymity level */
    m_anonLevel = -1;
    if ( (a_mixCascadeLength >= 0) && (getNrOfActiveUsers() >= 0) && (getTrafficSituation() >= 0)) {
      double userFactor = Math.min( ( (double) getNrOfActiveUsers()) / 500.0, 1.0);
      double trafficFactor = Math.min( ( (double) getTrafficSituation()) / 100.0, 1.0);
      double mixFactor = 1.0 - Math.pow(0.5, a_mixCascadeLength);
      /* get the integer part of the product -> 0 <= anonLevel <= 5 because mixFactor is always < 1.0 */
      m_anonLevel = (int) (userFactor * trafficFactor * mixFactor * 6.0);
    }
    m_statusXmlData = XMLUtil.toString(a_statusNode);
  }


  /**
   * Constructs a StatusInfo out of the single values. The creation time (last update) is set to
   * the current system time.
   *
   * @param a_mixCascadeId The ID of the mixcascade this StatusInfo belongs to.
   * @param a_nrOfActiveUsers The number of active users in the cascade.
   * @param a_currentRisk The risk calculated by the cascade (between 0 and 100).
   * @param a_trafficSituation The amount of traffic in the cascade.
   * @param a_mixedPackets The number of packets the cascade has mixed since startup.
   * @param a_anonLevel The anonymity level calculated by the JAP client (between 0 and 5).
   */
  private StatusInfo(String a_mixCascadeId, int a_nrOfActiveUsers, int a_currentRisk, int a_trafficSituation, long a_mixedPackets, int a_anonLevel) {
    /* use always the timeout for the infoservice context, because the JAP client currently does
     * not have a database of status entries -> no timeout for the JAP client necessary
     */
    super(System.currentTimeMillis() + Constants.TIMEOUT_STATUS);
    m_mixCascadeId = a_mixCascadeId;
    m_lastUpdate = System.currentTimeMillis();
    m_nrOfActiveUsers = a_nrOfActiveUsers;
    m_currentRisk = a_currentRisk;
    m_trafficSituation = a_trafficSituation;
    m_mixedPackets = a_mixedPackets;
    m_anonLevel = a_anonLevel;
    m_statusXmlData = XMLUtil.toString(generateXmlRepresentation());
  }


  /**
   * Returns the mixcascade ID of this status.
   *
   * @return The mixcascade ID of this status.
   */
  public String getId() {
    return m_mixCascadeId;
  }

  /**
   * Returns the time (see System.currentTimeMillis()), when the mixcascade has sent this
   * StatusInfo to an InfoService.
   *
   * @return The send time of this StatusInfo from the mixcascade.
   *
   */
  public long getLastUpdate() {
    return m_lastUpdate;
  }

  /**
   * Returns the time when this StatusInfo was created by the origin mixcascade (or by the JAP
   * client if it is a dummy entry).
   *
   * @return A version number which is used to determine the more recent status entry, if two
   *         entries are compared (higher version number -> more recent entry).
   */
  public long getVersionNumber() {
    return getLastUpdate();
  }

  /**
   * Returns the number of active users in the corresponding mixcascade.
   *
   * @return The number of active users in the corresponding mixcascade.
   */
  public int getNrOfActiveUsers() {
    return m_nrOfActiveUsers;
  }

  /**
   * Returns the current risk for using this mix cascade. This is a value between 0 an 100 and it
   * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
   * client.
   *
   * @return The current risk for the mixcascade.
   */
  public int getCurrentRisk() {
    return m_currentRisk;
  }

  /**
   * Returns the current traffic situation for the mixcascade.
   *
   * @return The current traffic situation for the mixcascade.
   */
  public int getTrafficSituation() {
    return m_trafficSituation;
  }

  /**
   * Returns the number of packets, which are mixed through the cascade since their startup.
   *
   * @return The number of mixed packets.
   */
  public long getMixedPackets() {
    return m_mixedPackets;
  }

  /**
   * Returns the calculated anonymity level (from number of active users, current traffic
   * and cascade length). It is a value between 0 and 5.
   *
   * @return The current anonymity level.
   */
  public int getAnonLevel() {
    return m_anonLevel;
  }

  /**
   * This returns the filename (InfoService command), where this status entry is posted at other
   * infoservices. It's always '/feedback'. This method is used within the context of the
   * infoservice when this status entry is forwarded to other infoservices.
   *
   * @return The filename where the information about this StatusInfo is posted at other
   *         InfoServices when this entry is forwarded.
   */
  public String getPostFile() {
    return "/feedback";
  }

  /**
   * This returns the data, which are posted to other InfoServices. It's the whole XML structure
   * of this status entry.
   *
   * @return The data, which are posted to other InfoServices when this entry is forwarded.
   */
  public byte[] getPostData() {
    return m_statusXmlData.getBytes();
  }

  /**
   * Returns the XML structure of this status entry as we received it.
   *
   * @return The original XML data of this status entry.
   */
  public String getStatusXmlData() {
    return m_statusXmlData;
  }

  /**
   * Returns a HTML table line with the data of this StatusDBEntry. This method is called within
   * the context of the infoservice by InfoServiceCommands.humanGetStatus().
   *
   * @return A HTML table line with the data of this status entry.
   */
  public String getHtmlTableLine() {
    String htmlTableLine = "<TR><TD CLASS=\"name\">";
    MixCascade ownMixCascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(getId());
    if (ownMixCascade != null) {
      htmlTableLine = htmlTableLine + ownMixCascade.getName();
    }
    /* generate a String, which describes the traffic situation */
    String trafficString = " (n/a)";
    if (getTrafficSituation() >= 0) {
      trafficString = " (low)";
    }
    if (getTrafficSituation() > 30) {
      trafficString = " (medium)";
    }
    if (getTrafficSituation() > 60) {
      trafficString = " (high)";
    }
    htmlTableLine = htmlTableLine + "</TD><TD CLASS=\"name\">" + getId() +
      "</TD><TD CLASS=\"status\" ALIGN=\"right\">" + Integer.toString(getNrOfActiveUsers()) +
      "</TD><TD CLASS=\"status\" ALIGN=\"right\">" + Integer.toString(getCurrentRisk()) +
      "</TD><TD CLASS=\"status\" ALIGN=\"center\">" + Integer.toString(getTrafficSituation()) +
      trafficString +
      "</TD><TD CLASS=\"status\" ALIGN=\"right\">" +
      NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(getMixedPackets()) +
      "</TD><TD CLASS=\"status\">" + new Date(getLastUpdate()) +
      "</TD></TR>";
    return htmlTableLine;
  }

  /**
   * This is a compatibility method for the creation of the CurrentStatus in the MixCascade
   * XML structure for old JAP clients.
   * @todo remove this method, only for compatibility with JAP client < 00.02.016
   *
   * @return The CurrentStatus node for this status entry.
   */
  public Node generateMixCascadeCurrentStatus() {
    Document doc = XMLUtil.createDocument();
    /* create the CurrentStatus element */
    Element currentStatusNode = doc.createElement("CurrentStatus");
    /* create the attributes of the CurrentStatus node */
    currentStatusNode.setAttribute("CurrentRisk", Integer.toString(getCurrentRisk()));
    currentStatusNode.setAttribute("TrafficSituation", Integer.toString(getTrafficSituation()));
    currentStatusNode.setAttribute("ActiveUsers", Integer.toString(getNrOfActiveUsers()));
    currentStatusNode.setAttribute("MixedPackets", Long.toString(getMixedPackets()));
    currentStatusNode.setAttribute("LastUpdate", Long.toString(getLastUpdate()));
    return currentStatusNode;
  }


  /**
   * Generates an XML representation for this StatusInfo entry.
   *
   * @return The generated XML representation for this StatusInfo.
   */
  private Element generateXmlRepresentation() {
    Document doc = XMLUtil.createDocument();
    /* create the MixCascadeStatus element */
    Element mixCascadeStatusNode = doc.createElement("MixCascadeStatus");
    /* create the attributes of the MixCascadeStatus node */
    mixCascadeStatusNode.setAttribute("id", getId());
    mixCascadeStatusNode.setAttribute("currentRisk", Integer.toString(getCurrentRisk()));
    mixCascadeStatusNode.setAttribute("mixedPackets", Long.toString(getMixedPackets()));
    mixCascadeStatusNode.setAttribute("nrOfActiveUsers", Integer.toString(getNrOfActiveUsers()));
    mixCascadeStatusNode.setAttribute("trafficSituation", Integer.toString(getTrafficSituation()));
    mixCascadeStatusNode.setAttribute("LastUpdate", Long.toString(getLastUpdate()));
    return mixCascadeStatusNode;
  }
}

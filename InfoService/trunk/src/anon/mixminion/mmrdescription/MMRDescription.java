/*
 Copyright (c) 2004, The JAP-Team
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
package anon.mixminion.mmrdescription;

import java.io.LineNumberReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
//import java.util.Iterator;
import java.util.TimeZone;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.ByteArrayUtil;

import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class MMRDescription
{
//	FIXME add/delete the needed/unneeded variables for our algorithms
	private String m_address;
	private String m_name;
	private int m_port;
	private String m_strSoftware;
	private MyRSAPublicKey m_IdentityKey;
	private MyRSAPublicKey m_PacketKey;
	private byte[] m_digest;
	private byte[] m_keydigest;
	private boolean m_isExitNode;

	private Date m_datePublished;
	private Date m_validafter;
	private Date m_validuntil;
	private final static DateFormat ms_DateFormatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final static DateFormat ms_DateFormatDateOnly = new SimpleDateFormat("yyyy-MM-dd");
	static
	{
		ms_DateFormatFull.setTimeZone(TimeZone.getTimeZone("GMT"));
		ms_DateFormatDateOnly.setTimeZone(TimeZone.getTimeZone("GMT"));
	}



	/**
	 * Constructor
	 * @param address
	 * address of the mixminion router
	 * @param name
	 * name for the mixminion router
	 * @param port
	 * port
	 * @param strSoftware
	 * version of the onion router software
	 */
	public MMRDescription(String address, String name, int port, String strSoftware, byte[] digest,
			byte[] keydigest, boolean exit, Date published, Date validafter, Date validuntil)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		this.m_strSoftware = strSoftware;
		this.m_digest = digest;
		this.m_keydigest = keydigest;
		this.m_isExitNode = exit;
		this.m_datePublished = published;
		this.m_validafter = validafter;
		this.m_validuntil = validuntil;
	}

	/**
	 * sets the IdentityKey for this MMR
	 * @param IdentityKey
	 * IdentityKey
	 * @return
	 * true if the key is a rsa key
	 */
	public boolean setIdentityKey(byte[] identitykey)
	{
		m_IdentityKey = MyRSAPublicKey.getInstance(identitykey);
		return m_IdentityKey != null;
	}

	/**
	 * gets the IdentityKey
	 * @return
	 * IdentityKey
	 */
	public MyRSAPublicKey getIdentityKey()
	{
		return this.m_IdentityKey;
	}

	/**
	 * sets the Packet key
	 * @param packetkey
	 * packetKey
	 * @return
	 * true if the packetKey is a rsa key
	 */
	public boolean setPacketKey(byte[] packetKey)
	{
		m_PacketKey = MyRSAPublicKey.getInstance(packetKey);
		return m_PacketKey != null;
	}

	/**
	 * gets the signing key
	 * @return
	 * signing key
	 */
	public MyRSAPublicKey getPacketKey()
	{
		return this.m_PacketKey;
	}


	/**
	 * gets the digest
	 * @return
	 * digest
	 */
	public byte[] getDigest()
	{
		return this.m_digest;
	}

	/**
	 * gets the keydigest
	 * @return
	 * digest
	 */
	public byte[] getKeyDigest()
	{
		return this.m_keydigest;
	}

	/**
	 * sets this server as exit node or not
	 * @param bm_isExitNode
	 *
	 */
	public void setExitNode(boolean bm_isExitNode)
	{
		m_isExitNode = bm_isExitNode;
	}

	/**
	 * returns if this server is an exit node
	 * @return
	 */
	public boolean isExitNode()
	{
		return m_isExitNode;
	}

	/**
	 * gets the address of the MMR
	 * @return
	 * address
	 */
	public String getAddress()
	{
		return this.m_address;
	}
	/**
	 * gets the name of the MMR
	 * @return
	 * name
	 */
	public String getName()
	{
		return this.m_name;
	}


	/**
	 * gets the port
	 * @return
	 * port
	 */
	public int getPort()
	{
		return m_port;
	}


	/**
	 * gets the software version of this MMR
	 * @return
	 * software version
	 */
	public String getSoftware()
	{
		return m_strSoftware;
	}

	/**
	 * gets the Routing Informations of this MMR
	 * @return routingInformation Vector
	 * Vector with two byte[], first is the Routing Type, Second the Routing Information
	 */
	public Vector getRoutingInformation()
	{
		byte[] a = m_address.getBytes();
		byte[] p = ByteArrayUtil.inttobyte(m_port,2);
		byte[] ri = new byte[a.length+p.length+m_digest.length];
		byte[] rt = ByteArrayUtil.inttobyte(3,2);
		ri = ByteArrayUtil.conc(p,m_digest,a);
		Vector routingInformation = new Vector();
		routingInformation.addElement(rt);
		routingInformation.addElement(ri);
		return routingInformation;
	}

	/**
	 *
	 * @param email vector with strings max 8
	 * @return vector with routingtype, routinginformation
	 */
	public Vector getExitInformation(Vector email) {

		Vector exitInformation = getRoutingInformation();
		//if no e-mail adress is specified return a vector with a drop and log error
		if (email.capacity()<1) {
			exitInformation.addElement( ByteArrayUtil.inttobyte(0,2));
	    	LogHolder.log(LogLevel.ERR, LogType.MISC,
			  "[Building ExitInformation]: no Recipients; Packet will be dropped! ");
			return exitInformation;
		}
		else {
			String s = "00"; //for the space between two email-adresses
			int count = 0;
			exitInformation.insertElementAt(ByteArrayUtil.inttobyte(4,2),0);
	/*		Iterator it = email.iterator();
			while (it.hasNext()) {
				String mail = (String) it.next();
				exitInformation.add(1,ByteArrayUtil.conc((byte[])exitInformation.get(1),s.getBytes(), mail.getBytes()));
				count++;
				if (count>8) {
			    	LogHolder.log(LogLevel.ERR, LogType.MISC,
					  "[Building ExitInformation]: more than 8 Recipients; 9+ will not receive ");
					break;
				}
			}*/
		}

		return exitInformation;
	}

	/**
	 * test if two OR's are identical
	 * returns also true, if the routers are in the same family
	 * @param or
	 * OR
	 * @return
	 */
	public boolean isSimilar(Object mixminionrouter)
	{
		if (mixminionrouter != null)
		{
			if(mixminionrouter instanceof MMRDescription)
			{
				MMRDescription or = (MMRDescription)mixminionrouter;
				if(m_address.equals(or.getAddress()) &&
				m_name.equals(or.getName()) &&
				(m_port == or.getPort()))
				{
					return true;
				}
			}
		}
		return false;
	}


	/**Tries to parse an router specification according to the desing document.
	 * @param reader
	 * reader
	 */
	public static MMRDescription parse(LineNumberReader reader)
	{
		try
		{
			//TODO only store the things we need in variables, otherwise make a readline to jump over them


			//skip [Server]
			//Descriptor-Version
			String descrver = reader.readLine().substring(20);
		//Nickname
			String nickname = reader.readLine().substring(10);
		//Identity
			byte[] identity = Base64.decode(reader.readLine().substring(10));
		//Digest
			byte[] digest = Base64.decode(reader.readLine().substring(8));
		//Signature
			byte[] signature = Base64.decode(reader.readLine().substring(11));
		//published
			Date published = ms_DateFormatFull.parse(reader.readLine().substring(11));
		//Valid after
			Date validafter = ms_DateFormatDateOnly.parse(reader.readLine().substring(13));
		//Valid until
			Date validuntil = ms_DateFormatDateOnly.parse(reader.readLine().substring(13));
		//Packet Key
			byte[] packetkey = Base64.decode(reader.readLine().substring(12));
			//Packet Versions
			String packetversions = reader.readLine().substring(17);
		//Software
			String software = reader.readLine().substring(10);
			//Secure-Configuration
			String secureconfiguration = reader.readLine().substring(22);
			//Contact
			String contact = reader.readLine().substring(9);
			//Why Insecure
			String whyinsecure = reader.readLine().substring(14);
			//Comments
			String comments = reader.readLine().substring(10);
			reader.readLine(); //skip [incoming/MMTP]
			//Incoming Version
			String incomversion = reader.readLine().substring(9);
			//IP
			String ip = reader.readLine().substring(4);
		//Hostname
			String hostname = reader.readLine().substring(10);
		//Port
			String port = reader.readLine().substring(6);
			//FIXME some routers define no port, they will be dropped
			if (port.startsWith("gest")) return null;
		//Key-Digest
			byte[] keydigest = Base64.decode(reader.readLine().substring(12));
			//Incoming Protocols
			String incomprotocols = reader.readLine().substring(11);
			reader.readLine(); //Skip [Outgoing/MMTP]
			//Outgoing Version
			String outversion = reader.readLine().substring(9);
			//Outgoing Protocols
			String outprotocols = reader.readLine().substring(11);

		//exitnode,mbox and/or fragmented delivery
			String temp="";
			boolean exitNode=false;
			boolean mbox = false;
			boolean fragmented = false;
			for (;;) {
				temp=reader.readLine();
				if (temp.startsWith("[Testing]")) break;
				if (temp.startsWith("[Delivery/SMTP]")) exitNode=true;
				if (temp.startsWith("[Delivery/MBOX]")) mbox = true;
				if (temp.startsWith("[Delivery/Fragmented")) fragmented = true;
			}

			//build the new MMRDescription
			MMRDescription mmrd = new MMRDescription(hostname, nickname, Integer.parseInt(port),
								software, digest, keydigest, exitNode, published,validafter, validuntil);

			if (!mmrd.setIdentityKey(identity) || !mmrd.setPacketKey(packetkey))
				{
				 return null;
				}

			return mmrd;




		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}
	//FIXME only for testing purpose
	public String toString()
	{
		return "MMRRouter: " + this.m_name + " on " + this.m_address + ":" + this.m_port + " Software : "+this.m_strSoftware+" Exitnode:" +this.m_isExitNode;
	}

}

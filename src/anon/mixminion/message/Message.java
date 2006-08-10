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

package anon.mixminion.message;


import java.io.IOException;
import java.util.Vector;

//import javax.swing.JFrame;
//import javax.swing.JTextArea;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.mixminion.FirstMMRConnection;
import anon.mixminion.Mixminion;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.mixminion.mmrdescription.PlainMMRListFetcher;


/**
 * @author Jens Kempe, Stefan Rönisch
 */
public class Message
{
	private String m_payload = null; // "raw"-Payload from the mail
	private String[] m_recipient = null; // all recipients; if there is a replyblock in use: dont care
	private int m_hops = 0;
	private String m_address; // E-Mail address of the sender; needed to build a Replyblock(this one is appended to the payload to allow a reply)
	private boolean m_withreplyblock = false; // true if the the message should contain a Replyblock
	private String m_decoded = null;


	// Constants (from original Python-Implementation)
	int MAX_FRAGMENTS_PER_CHUNK = 16;
	double EXP_FACTOR = 1.3333333333333333;
	// keyringpassword
	String keyringpassword = "mussichnoch"; //FIXME

/**
 * usage:
 * for normal message: specify payload, recipient, hops,rb=null,myadress=dontcare,repliable=false
 * for repliable message specify above but: myadress=e-mail for replies, repliable=true
 * for message to an replyblock specify above possibilitys but: rb = Replyblock, recipient=dontcare
 * @param payload
 * @param recipient
 * @param hops
 * @param rb
 * @param myadress
 * @param repliable
 */
	public Message(String payload, String[] recipient, int hops, String myadress, boolean repliable)
	{
		this.m_payload = payload;
		this.m_recipient = recipient;
		this.m_hops = hops;
		this.m_address = myadress; //=Exit-Address in the ReplyBlock, which allows a recipient to reply
		this.m_withreplyblock = repliable; //Message should be repliable
//		// FIXME hops gerade machen
//		m_hops = ( (m_hops + 1) / 2) * 2;
	}

	public boolean send()
	{
		return encodeMessage();
	}

	boolean encodeMessage()
//  1. Determine the type of the message: plaintext forward, or reply.

				{
//		0. 	Check whether the user only wants to decode a payload, yes: decode it no: 0.1
//		0.1 Look if the Sender has specified a ReplyBlock to send to
//		0.2 Do we want to make the message repliable? yes: add a replyblock to the payload
//      0.3 add ascii armor to the payload
//		1. Compress the message
//		2. Choose whether SingleBlock or Fragmented Message Imlementation and build the payload(s)
//		3. Choose whether in Reply or normal Forward Message
//		4. Build the packets.
//      5. Deliver each packet


		//0.look if the user only wants a decoded representation
		Decoder decoder = new Decoder(m_payload, keyringpassword);
		String decoded = null;
		try
		{
			decoded = decoder.decode();
		} catch (IOException e2)
			{
			System.out.println("Decodier-Exception...");
			}

		if (decoded != null)
		{
			//TODO for the Moment we open a new window with the decoded content
	/** Removed becaus no GUI in lib coding directive...
	JFrame jf = new JFrame( "Decodiert:" );
			jf.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE   );
		    jf.setSize( 700, 1000 );

		    JTextArea jt = new JTextArea();
		    jt.append(decoded);
		    jt.setVisible(true);
		    jf.getContentPane().add(jt);
		    jf.setVisible( true );
		    //----
*/			m_decoded=decoded;
			return false;
		}

		//There is a message to send....
		//Needed Variables
		byte[][] message_parts = null; // Array with finalized payload parts, each 28kb
		boolean returnValue = true; // all parts sended correct?

		MessageImplementation payload_imp; //BridgeVariable, if single or multipart implemetation
		ReplyImplementation message_imp; //BridgeVariable, if repliable or not


		//0.1 possibly parse replyblock
		String m_payload_temp = m_payload;
		ReplyBlock rb = new ReplyBlock(null,null,null,10000); //if the payload contains a replyblock this one is initialised and used as header 2, otherwise the specified recipients are used to build header2
		try
		{
			//TODO do this stuff in the e-mail class
			rb = rb.parseReplyBlock(m_payload_temp, null);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		//remove the replyblock from the payload, he is not needed anymore
		if (rb != null)
	{
			try
			{
				m_payload = rb.removeRepyBlock(m_payload);
			} catch (IOException e1)
		{
				e1.printStackTrace();
			}
		}

		//prepare mmrlist
		//TODO Logausgabe????
		MMRList mmrlist = new MMRList(new InfoServiceMMRListFetcher()); //try to get it from the infoservice
		if (!mmrlist.updateList()) //if this fails, try to get it directly from the server
		{
			mmrlist = new MMRList(new PlainMMRListFetcher());
			if (!mmrlist.updateList())  //if nothing works return false
			{
				return false;
			}
		}

		//0.2 Do we want to send a ReplyBlock with the Message to allow a Reply?
		//if so, we have to build a replyblock an add it to the payload...

		if (m_withreplyblock) {
			//build Repyblock
			//TODO BETA We use the same Replyblock for every Message part
			Vector path_to_me = mmrlist.getByRandomWithExit(m_hops);
			byte[] user_secret = new Keyring(keyringpassword).getNewSecret();
			ReplyBlock reply_to_me = new ReplyBlock(m_address, path_to_me, user_secret);
			reply_to_me.buildBlock();
			m_payload = m_payload + reply_to_me.getReplyBlockasString();
	}

		//0.3 bring the payload in the right format
		//TODO Test if this is necessary, because the server adds this too
		m_payload = armorText(m_payload);

		//1. Compress  "raw" payload.
		byte[] compressed_payload = MixMinionCryptoUtil.compressData(m_payload.getBytes());
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "[Message] Compressed Size = " + compressed_payload.length);

		//2. choose which concrete Implementation for the payload to use
		//Test if the payload fix to one 28k block or if it must be fragmented
		if (compressed_payload.length + SingleBlockMessage.SINGLETON_HEADER_LEN <= 28 * 1024)

		{
			payload_imp = new SingleBlockMessage(compressed_payload);
		}

		else
		{
			payload_imp = new FragmentedMessage(m_recipient, m_payload.getBytes());

		}

		//build payload
		message_parts = payload_imp.buildPayload();

		//constraint
		if (message_parts.length == 0)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
					  "[Message] Compression failure--> 0 packets ");
			return false;
		}

		//3. Choose wether to send with in Reply to an ReplyBlock or without, note that the replyblock in the constructor
		//contains the address of the recipient
		//for every element in message_parts build a 32k message
		if (rb != null)
		{
			//FIXME test whether the rb is within his time to live
			if (!rb.timetoliveIsOK()) return false;
			//
			else message_imp = new ReplyMessage(message_parts, m_hops, rb, mmrlist);
		}

		else {
			message_imp = new NoReplyMessage(message_parts, m_hops, m_recipient, mmrlist);
		}


		//4. build the packets
		Vector packets = message_imp.buildMessage();
		//5. send each packet
		Vector firstservers = message_imp.getStartServers();

		for(int i = 0; i < packets.size(); i++)
		{
			returnValue = returnValue && sendToMixMinionServer((byte[])packets.elementAt(i), (MMRDescription) firstservers.elementAt(i));
		}
		return returnValue;
	}

	/**
	 * Send a message to the specified MMR
	 * @param message
	 * @param description
	 * @return true if sended successfully
	 */
	private boolean sendToMixMinionServer(byte[] message, MMRDescription description)
	{
		boolean returnValue = false;

		try
		{
			Mixminion mixminion = Mixminion.getInstance();
			FirstMMRConnection fMMRcon = new FirstMMRConnection(description, mixminion);

			System.out.println("   connecting...");
			fMMRcon.connect();

			System.out.println("   sending...");
			returnValue = fMMRcon.sendMessage(message);
			System.out.println("   Value of SendingMethod = " + returnValue);

			System.out.println("   close connection");
			fMMRcon.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnValue;
	}

	/**
	 * Adds a ASCII Armor to the message
	 * @param message
	 * @return armoredText
	 */

	private String armorText(String message){
        //FIXME Absender rausfiltern
		m_payload = m_payload.substring(m_payload.indexOf("Subject:"), m_payload.length());
//		//- davorsetzen
//		LineNumberReader reader = new LineNumberReader(new StringReader(m_payload));
//		String filtered ="";
//
//		while (1==1) {
//			try {
//				String aktLine = reader.readLine();
//				if (aktLine == null) {
//					break;
//				}
//				filtered += "\n" + "- " + aktLine ;
//			} catch (IOException e) {
//				break;
//			}
//		}

		String result = new String();
		    result = "-----BEGIN TYPE III ANONYMOUS MESSAGE-----\n";
		    result = result + "Message-type: plaintext\n\n" +"Nachricht: \n";
		    result = result + message + "\n";
		    result = result + "-----END TYPE III ANONYMOUS MESSAGE-----\n";
		    return result;
	}

	/**
	 * rechte die Ganzzahl aus, welche groesser als a/b ist
	 * @param a
	 * @param b
	 * @return
	 */
	private int ceilDiv(double a, double b)
	{
		return (int) Math.ceil(a / b);
	}

	public String getDecoded() {
		return m_decoded;
	}
}

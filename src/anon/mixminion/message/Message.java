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

import java.io.FileOutputStream;
import java.util.Vector;

import anon.mixminion.FirstMMRConnection;
import anon.mixminion.Mixminion;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.util.ByteArrayUtil;

import anon.mixminion.fec.FECCode;
import anon.mixminion.fec.FECCodeFactory;

/**
 * @author Jens Kempe
 */
public class Message
{
	private byte[] m_payload = null;
	private Vector m_recipient = null;
	private int m_hops = 0;

	/** Konstanten aus der E2E and Spec **/
	static final int FRAGMENT_HEADER_LEN = 47,
	SINGLETON_HEADER_LEN = 22,
	KEY_LEN = 16,
	OVERHEAD = 0; // weil Plaintext forward Message

	/** The Constructor of a Message **/
	public Message(byte[] payload, Vector recipient, int hops)
	{
		System.out.println("   Constructor");
		this.m_payload = payload;
		this.m_recipient = recipient;
		this.m_hops = hops;
	}

	public boolean send()
	{
		return encodeMessage();
	}

	boolean encodeMessage()
	{
//        1. Determine the type of the message: encrypted forward, plaintext forward, or reply.
//        2. Compress and possibly fragment the message into a set of payloads.  (The size of
//           the payloads will depend on the type of the message.)
//        3. Annotate each payload with a payload header.  (The payload header includes size,
//           integrity, and fragmentation information.)
//        4. According to the type of the message, encode each payload into a final 28KB paylaod
//           and (possibly) 20-octet decoding handle.
//        5. For each payload, select a list of servers to form a path through the network.
//        6. Using the decoding handle, payload contents, and route for each payload, generate
//           a 32KB type III packet.
//        7. Deliver each packet

		/*2*/
		byte[] compressPayload = MixMinionCryptoUtil.compressData(m_payload);
		System.out.println("   Compressed Size = " + compressPayload.length);

		try
		{
			new FileOutputStream("C:/temp/compress.txt").write(compressPayload);
		}
		catch (Exception e)
		{}

		String[] frags = null;
		byte[] help = null;
		// "possibly" falls mehr als 28K dann fragmentieren //
//        if (compressPayload.length > 28*1024) frags = divideIntoFragments(compressPayload, 28*1024, 4/3);
//        else frags = new String[] {new String(compressPayload)};
//        System.out.println("   l=" + frags.length);

		//Erstmal nur f�r Nachrichten kleiner 28KB
		if (compressPayload.length + SINGLETON_HEADER_LEN <= 28 * 1024)
		{
			// Let PADDING_LEN = 28KB - LEN(M_C) - SINGLETON_HEADER_LEN - OVERHEAD
			// Let PADDING = Rand(PADDING_LEN)
			// return Flag 0 | Int(15,LEN(M_C)) | Hash(M_C | PADDING) | M_C | PADDING
			int PADDING_LEN = 28 * 1024 - compressPayload.length - SINGLETON_HEADER_LEN;
			byte[] PADDING = MixMinionCryptoUtil.randomArray(PADDING_LEN);
			long len = compressPayload.length;
			byte[] first = ByteArrayUtil.inttobyte(len, 2);
			byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(compressPayload, PADDING));
			byte[] all = ByteArrayUtil.conc(first, hash, compressPayload, PADDING);
			frags = new String[]
				{
				new String(all)};
			help = all;
			try
			{
				new FileOutputStream("C:/temp/frag1_1.txt").write(all);
			}
			catch (Exception e)
			{}
			try
			{
				new FileOutputStream("C:/temp/frag1_2.txt").write(frags[0].getBytes());
			}
			catch (Exception e)
			{}

			byte[] hh = "PAYLOAD ENCRYPT".getBytes();
			String hhh = new String(hh);
			try
			{
				new FileOutputStream("C:/temp/t1_1.txt").write(hh);
			}
			catch (Exception e)
			{}
			try
			{
				new FileOutputStream("C:/temp/t1_2.txt").write(hhh.getBytes());
			}
			catch (Exception e)
			{}

		}
		else
		{
//            Let FRAGMENTS = DIVIDE(M_C, 28KB-OVERHEAD-FRAGMENT_HEADER_LEN)
//            Let ID = Rand(TAG_LEN)
//            Let SZ = Int(32,Len(M_C))
//            For every FRAGMENT_i in FRAGMENTS:
//               Let PAYLOAD_i = a fragment payload containing:
//                  Flag 1 | Int(23,i) | Hash(ID | SZ | FRAGMENT_i ) | ID
//                     | SZ | FRAGMENT_i
//            return every PAYLOAD_i.
			frags = divideIntoFragments(compressPayload, 28 * 1024 - FRAGMENT_HEADER_LEN, 4 / 3);
			byte[] id = MixMinionCryptoUtil.randomArray(20);
			byte[] sz = ByteArrayUtil.inttobyte(compressPayload.length, 4);
			String[] payloads = new String[frags.length];
			for (int i = 0; i < frags.length; i++)
			{
				long flag = 8388608; // =2^23
				flag = flag + i;
				byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(id, sz, frags[i].getBytes()));
				payloads[i] = new String(ByteArrayUtil.conc(ByteArrayUtil.inttobyte(flag, 3), hash, id, sz,
					frags[i].getBytes()));
			}
			frags = payloads;
		}

		/*3-6 fuer jedes Payload_Fragment einen Header machen -> M machen -> versenden*/
		boolean returnValue = true;
		if (frags.length == 0)
		{
			returnValue = false;
		}
		for (int i_frag = 0; i_frag < frags.length; i_frag++)
		{
			System.out.println("   frag_i=" + i_frag);

			// Header machen
			Header header1 = new Header(m_hops, m_recipient);
			Header header2 = new Header(m_hops, m_recipient);
			byte[] H1 = header1.getAsByteArray();
			byte[] H2 = header2.getAsByteArray();
			byte[] P = help; // frags[i_frag].getBytes(); FIXME FIXME
			// M is the MixMinionPacket Type III
			byte[] M = null;

			/** Phase 1 - H2 is not a reply block **/
			// for i = N .. 1
			//   P = SPRP_Encrypt(SK2_i, "PAYLOAD ENCRYPT", P)
			// end
			for (int i = header2.getSecrets().size() - 1; i >= 0; i--)
			{
				System.out.println("   s_keys=" + header2.getSecrets().size());
				byte[] SK2_i = (byte[]) header2.getSecrets().elementAt(i);
				byte[] K = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK2_i, "PAYLOAD ENCRYPT".getBytes()));
				P = MixMinionCryptoUtil.SPRP_Encrypt(K, P);
				try
				{
					new FileOutputStream("C:/temp/encrypt1_" + i + ".txt").write(P);
				}
				catch (Exception e)
				{}
			}

			/** Phase 2: SPRP verschluesseln **/
			// H2 = SPRP_Encrypt(SHA1(P), "HIDE HEADER", H2)
			// P = SPRP_Encrypt(SHA1(H2), "HIDE PAYLOAD", P)
			// for i = N .. 1
			//   H2 = SPRP_Encrypt(SK1_i, "HEADER ENCRYPT",H2)
			//   P = SPRP_Encrypt(SK1_i, "PAYLOAD ENCRYPT",P)
			// end
			// M = H1 | H2 | P

			H2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(P,
				"HIDE HEADER".getBytes())), H2);
			P = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(H2,
				"HIDE PAYLOAD".getBytes())), P);
			for (int i = header1.getSecrets().size() - 1; i >= 0; i--)
			{
				System.out.println("   s_keys=" + header1.getSecrets().size());
				byte[] SK1_i = (byte[]) header1.getSecrets().elementAt(i);
				H2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
					"HEADER ENCRYPT".getBytes())), H2);
				P = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
					"PAYLOAD ENCRYPT".getBytes())), P);
				try
				{
					new FileOutputStream("C:/temp/encrypt2_" + i + ".txt").write(P);
				}
				catch (Exception e)
				{}
			}
			M = ByteArrayUtil.conc(H1, H2, P);
			try
			{
				new FileOutputStream("C:/temp/M_toSend.txt").write(M);
			}
			catch (Exception e)
			{}

			System.out.println("   M=" + M.length);

			boolean AntwortAusDerSendingMethode = false;
			// an 1. Server schicken
			try
			{
//                MMRList mmrl = new MMRList(new PlainMMRListFetcher());
//                System.out.println("MMR-Liste aktualisiert: " + mmrl.updateList());

//Jens: an den 1.Server aus Header und ncht an RandomServer schicken
//                MMRDescription mmdescr = mmrl.getByRandom();
				MMRDescription mmdescr = (MMRDescription) header1.getRoute();

				Mixminion mixminion = Mixminion.getInstance();
				FirstMMRConnection fMMRcon = new FirstMMRConnection(mmdescr, mixminion);
				System.out.println(mmdescr.toString());
				// connect to first miminion-router

				/** nur zum testen **/
//                MMRDescription mm = new MMRDescription("127.0.0.1", "rinos", 48099, "",
//                                                        Base64.decode("syFg+ty3hWUgY9NBurUpOotdfhg="),
//                                                        Base64.decode("MIIBCgKCAQEA584fjC480O/T9PO1AQMw82ULbA89EBCqhCsbXD+jhQHT+XxtVazXRYA+za3Ex1NvPRrQBhYH+FLNHrYvHNo2LD7AT/pKXqeAeMRc18YAuC4A54SctM4jcOkLIHHn57xe1AanAuu4EjodeDKOLCv1fJpcIijeJOM98vE3ejdnfvahaMwNGYdBxovhHAwU8CADdNzYCNbfrl+nm6fZwRXmTHdEmQ6mnTXNiOnaNb6nlSmolNkvUDPXxt2xXR2yEpGJefgJhTasKyhpbMNpeHFF260897qK6HfScd88MX+yQbXNxOP3NI48/hDrmvanSrLZOsh2tKoTIASjDCDGU6xBzQIDAQAB"),
//                                                        true, new Date(2005,12,12),  new Date(2005,12,12),  new Date(2005,12,12));
//                mm.setIdentityKey(Base64.decode("MIIBCgKCAQEA584fjC480O/T9PO1AQMw82ULbA89EBCqhCsbXD+jhQHT+XxtVazXRYA+za3Ex1NvPRrQBhYH+FLNHrYvHNo2LD7AT/pKXqeAeMRc18YAuC4A54SctM4jcOkLIHHn57xe1AanAuu4EjodeDKOLCv1fJpcIijeJOM98vE3ejdnfvahaMwNGYdBxovhHAwU8CADdNzYCNbfrl+nm6fZwRXmTHdEmQ6mnTXNiOnaNb6nlSmolNkvUDPXxt2xXR2yEpGJefgJhTasKyhpbMNpeHFF260897qK6HfScd88MX+yQbXNxOP3NI48/hDrmvanSrLZOsh2tKoTIASjDCDGU6xBzQIDAQAB"));
//                fMMRcon = new FirstMMRConnection(mm, mixminion);
				/** ende tesen**/


				//
				System.out.println("   connecting...");
				fMMRcon.connect();

				//send a junk-packet
				System.out.println("   sending...");
				AntwortAusDerSendingMethode = fMMRcon.sendMessage(M);
				System.out.println("   Sende Message: " + AntwortAusDerSendingMethode);

				//close connection
				System.out.println("   Trenne Verbindung.");
				fMMRcon.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				AntwortAusDerSendingMethode = false;
			}
			returnValue = returnValue && AntwortAusDerSendingMethode;
		}
		return returnValue;
	}

	/////////////////////////////////// zusaetzliche Methoden ///////////////////////////////////


	/**
	 * Diese Methode fragmentiert eine Nachricht (nach der E2E.txt)
	 * @param M, the message to send
	 * @param PS, payload sized (fixed)
	 * @param EXF, expansion factor [Everyone must use the same EXF. The value of EXF is 4/3.]
	 * @return the fragments of the Message
	 */
	String[] divideIntoFragments(byte[] M, int PS, double EXF)
	{
		System.out.println("   beim Teilen");

		double tmp;

		// Let M_SIZE = CEIL(LEN(M) / PS)
		double M_SIZE = Math.ceil(M.length / (double) PS);

		// Let K = Min(16, 2**CEIL(Log2(M_SIZE)))
		tmp = Math.log(M_SIZE) / Math.log(2);
		tmp = Math.ceil(tmp);
		tmp = Math.pow(2, tmp);
		int K = (int) Math.min(16, tmp);

		// Let NUM_CHUNKS = CEIL(M_SIZE / K)
		int NUM_CHUNKS = (int) Math.ceil(M_SIZE / (double) K);

		System.out.println("   beim Teilen 2");

		// Let M = M | PRNG(Rand(KEY_LEN), Len(M) - NUM_CHUNKS*PS*K)
		byte[] random = MixMinionCryptoUtil.randomArray(KEY_LEN);
		int len = M.length - NUM_CHUNKS * PS * K;
		System.out.println(len);
		byte[] prng = MixMinionCryptoUtil.createPRNG(random, len);
		M = ByteArrayUtil.conc(M, prng);

		System.out.println("   beim Teilen 3");

		// For i from 1 to NUM_CHUNKS:
		//    Let CHUNK_i = M[(i-1)*PS*K : i*PS*K]
		// End
		String[] CHUNK = new String[NUM_CHUNKS];
		for (int i = 1; i <= NUM_CHUNKS; i++)
		{
			byte[] b = ByteArrayUtil.copy(M, (int) ( (i - 1) * PS * K), (int) (i * PS * K));
			CHUNK[i + 1] = new String(b);
		}

		System.out.println("   beim Teilen 4");

		// Let N = Ceil(EXF*K)
		int N = (int) Math.ceil(EXF * K);

		// For i from 0 to NUM_CHUNKS-1:
		//    For j from 0 to N-1:
		//      FRAGMENTS[i*N+j] = FRAGMENT(CHUNK_i, K, N, j)
		//    End loop
		// End loop

		System.out.println("   N,num" + N + " " + NUM_CHUNKS);

		String[] FRAGMENTS = new String[NUM_CHUNKS * N];
		for (int i = 0; i <= NUM_CHUNKS - 1; i++)
		{
			for (int j = 0; j <= N - 1; j++)
			{
				FRAGMENTS[i * N + j] = FRAGMENT(CHUNK[i], K, N, j, PS);
			}
		}

		return FRAGMENTS;
	}

	/**
	 * soll den von der Nachricht m das i'te von N Paketen zur�ckgeben
	 */
	String FRAGMENT(String M, int K, int N, int I, int PS)
	{
		//k = number of source packets to encode
		//n = number of packets to encode to
		//packetsize = 1024;
		int packetsize = PS;

		System.out.println("   " + M);

//        Random rand = new Random();

		byte[] source = M.getBytes(); //new byte[K*packetsize]; //this is our source file

		//NOTE:
		//The source needs to split into k*packetsize sections
		//So if your file is not of the write size you need to split it into
		//groups.  The final group may be less than k*packetsize, in which case
		//you must pad it until you read k*packetsize.  And send the length of the
		//file so that you know where to cut it once decoded.

//        rand.nextBytes(source);     //this is just so we have something to encode

		byte[] repair = new byte[N * packetsize]; //this will hold the encoded file

		//These buffers allow us to put our data in them
		//they reference a packet length of the file (or at least will once
		//we fill them)
		byte[][] sourceBuffer = new byte[K][];
		byte[][] repairBuffer = new byte[N][];
		int[] srcOffs = new int[sourceBuffer.length];
		int[] repairOffs = new int[repairBuffer.length];
		for (int i = 0; i < sourceBuffer.length; i++)
		{
			sourceBuffer[i] = source;
			srcOffs[i] = i * packetsize;
		}

		for (int i = 0; i < repairBuffer.length; i++)
		{
			repairBuffer[i] = repair;
			repairOffs[i] = i * packetsize;
		}

		//When sending the data you must identify what it's index was.
		//Will be shown and explained later
		int[] repairIndex = new int[N];

		for (int i = 0; i < repairIndex.length; i++)
		{
			repairIndex[i] = i;
		}

		//create our fec code
		FECCode fec = FECCodeFactory.getDefault().createFECCode(K, N);

		//encode the data
		fec.encode(sourceBuffer, srcOffs, repairBuffer, repairOffs, repairIndex, packetsize);
		//encoded data is now contained in the repairBuffer/repair byte array

		return new String(repairBuffer[I], repairOffs[I], packetsize);
	}
}

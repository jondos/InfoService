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

import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.mixminion.FirstMMRConnection;
import anon.mixminion.Mixminion;
import anon.mixminion.fec.FECCode;
import anon.mixminion.fec.FECCodeFactory;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.util.ByteArrayUtil;

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
	OVERHEAD = 0; // ist 0 weil Plaintext forward Message

	// Konstanten aus den PhyonCode
	int MAX_FRAGMENTS_PER_CHUNK = 16;
	double EXP_FACTOR = 1.3333333333333333;

	/** The Constructor of a Message **/
	public Message(byte[] payload, Vector recipient, int hops)
	{
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
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "[Message] Compressed Size = " + compressPayload.length);

		// Feld fuer die Fragmente
		byte[][] frags = null;

		// "possibly" falls mehr als 28K dann fragmentieren //
//        if (compressPayload.length > 28*1024) frags = divideIntoFragments(compressPayload, 28*1024, 4/3);
//        else frags = new String[] {new String(compressPayload)};
//        System.out.println("   l=" + frags.length);

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
			frags = new byte[][]
				{
				all};
		}
		else
		{
//			When generating plaintext forward
//			fragmented messages, the message generator uses a routing type of
//			"FRAGMENT" (0x0103), an empty routing info, and prepends the
//			following fields to the message body before compressing and
//			fragmenting it:
//
//			         RS Routing size    2 octets
//			         RT Routing type    2 octets
//			         RI Routing info    (variable length; RS=Len(RI))
			ExitInformation tri = MMRDescription.getExitInformation(m_recipient);
			byte[] prepayload = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(tri.m_Content.length, 2),
				ByteArrayUtil.inttobyte(ExitInformation.TYPE_SMTP, 2), tri.m_Content);
			m_payload = ByteArrayUtil.conc(prepayload, m_payload);
			compressPayload = MixMinionCryptoUtil.compressData(m_payload);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[Message] Fragmented, new Compressed Size = " + compressPayload.length);
			if (compressPayload.length + SINGLETON_HEADER_LEN <= 28 * 1024)
			{
				throw new RuntimeException("Fragmented Header nach Neukomprimierung mit Single-Laenge");
			}

//            Let FRAGMENTS = DIVIDE(M_C, 28KB-OVERHEAD-FRAGMENT_HEADER_LEN)
//            Let ID = Rand(TAG_LEN)
//            Let SZ = Int(32,Len(M_C))
//            For every FRAGMENT_i in FRAGMENTS:
//               Let PAYLOAD_i = a fragment payload containing:
//                  Flag 1 | Int(23,i) | Hash(ID | SZ | FRAGMENT_i ) | ID
//                     | SZ | FRAGMENT_i
//            return every PAYLOAD_i.
			frags = divideIntoFragments(compressPayload);
			byte[] id = MixMinionCryptoUtil.randomArray(20);
			byte[] sz = ByteArrayUtil.inttobyte(compressPayload.length, 4);
			byte[][] payloads = new byte[frags.length][28 * 1024];
			for (int i = 0; i < frags.length; i++)
			{
				long flag = 8388608; // =2^23
				flag = flag + i;
				byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(id, sz, frags[i]));
				payloads[i] = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(flag, 3), hash, id, sz, frags[i]);
			}
			frags = payloads;
		}

		/*3-6 fuer jedes Payload_Fragment einen Header machen -> M machen -> versenden*/
		boolean returnValue = true;
		if (frags.length == 0)
		{
			returnValue = false;
		}
		// Pfad fuer die Header erzeugen
		// hops gerade machen
		m_hops = ( (m_hops + 1) / 2) * 2;
		//Pfad anlegen
		MMRList mmrlist = new MMRList(new InfoServiceMMRListFetcher());
		mmrlist.updateList();
		Vector path = new Vector();
		boolean isfragmented = false;
		//fuer Singleton
		if (frags.length == 1)
		{
			Vector tv = mmrlist.getByRandomWithExit(m_hops);
			path.addElement(tv);
		}
		// fuer jedes frag einen mit gleichem schluss-server
		else
		{
			path = mmrlist.getByRandomWithFrag(m_hops, frags.length);
			isfragmented = true;
		}

		for (int i_frag = 0; i_frag < frags.length; i_frag++)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[Message] make Header to Fragment_" + i_frag);

			// Header machen
			Vector wholepath = (Vector) path.elementAt(i_frag);
			Vector path1 = MixMinionCryptoUtil.subVector(wholepath, 0, m_hops / 2);
			Vector path2 = MixMinionCryptoUtil.subVector(wholepath, m_hops / 2, m_hops / 2);

			//zwei mal secrets bauen
			Vector secrets1 = new Vector();
			Vector secrets2 = new Vector();
			for (int i = 0; i < (m_hops / 2); i++)
			{
				secrets1.addElement(MixMinionCryptoUtil.randomArray(16));
				secrets2.addElement(MixMinionCryptoUtil.randomArray(16));
			}
			//ExitInfos und Crossoverpoint definieren
			ExitInformation exit2 = new ExitInformation();
			if (isfragmented)
			{
				exit2.m_Type = ExitInformation.TYPE_FRAGMENTED;
				exit2.m_Content = new byte[0];
			}
			else
			{
				exit2 = ( (MMRDescription) path2.elementAt(0)).getExitInformation(m_recipient);
			}

			ExitInformation exit1 = new ExitInformation();
			exit1.m_Type = RoutingInformation.TYPE_SWAP_FORWARD_TO_HOST;
			exit1.m_Content = ( (ForwardInformation) ( (MMRDescription) path2.elementAt(0)).
							   getRoutingInformation()).m_Content;
			//Header erzeugen
			Header header1 = new Header(path1, secrets1, (ExitInformation) exit1);
			Header header2 = new Header(path2, secrets2, exit2);
			byte[] H1 = header1.getAsByteArray();
			byte[] H2 = header2.getAsByteArray();
			byte[] P = frags[i_frag];
			// M is the MixMinionPacket Type III
			byte[] M = null;

			/** Phase 1 - H2 is not a reply block **/
			// for i = N .. 1
			//   P = SPRP_Encrypt(SK2_i, "PAYLOAD ENCRYPT", P)
			// end
			for (int i = secrets2.size() - 1; i >= 0; i--)
			{
				byte[] SK2_i = (byte[]) secrets2.elementAt(i);
				byte[] K = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK2_i, "PAYLOAD ENCRYPT".getBytes()));
				P = MixMinionCryptoUtil.SPRP_Encrypt(K, P);
			}

			/** Phase 2: SPRP verschluesseln **/
			// H2 = SPRP_Encrypt(SHA1(P), "HIDE HEADER", H2)
			// P = SPRP_Encrypt(SHA1(H2), "HIDE PAYLOAD", P)
			// for i = N .. 1
			//   H2 = SPRP_Encrypt(SK1_i, "HEADER ENCRYPT",H2)
			//   P = SPRP_Encrypt(SK1_i, "PAYLOAD ENCRYPT",P)
			// end
			// M = H1 | H2 | P

			H2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(
				MixMinionCryptoUtil.hash(P), "HIDE HEADER".getBytes())), H2);
			P = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(
				MixMinionCryptoUtil.hash(H2), "HIDE PAYLOAD".getBytes())), P);
			for (int i = secrets1.size() - 1; i >= 0; i--)
			{
				byte[] SK1_i = (byte[]) secrets1.elementAt(i);
				H2 = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
					"HEADER ENCRYPT".getBytes())), H2);
				P = MixMinionCryptoUtil.SPRP_Encrypt(MixMinionCryptoUtil.hash(ByteArrayUtil.conc(SK1_i,
					"PAYLOAD ENCRYPT".getBytes())), P);
			}
			M = ByteArrayUtil.conc(H1, H2, P);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[Message] the Messagesize = " + M.length + " Bytes");

			// Message an 1. MixMinonServer schicken
			returnValue = returnValue && sendToMixMinionServer(M, (MMRDescription) path1.elementAt(0));
		}
		return returnValue;
	}

	/////////////////////////////////// zusaetzliche Methoden ///////////////////////////////////


	/**
	 * Diese Methode fragmentiert eine Nachricht (nach der E2E.txt)
	 * @param M, the message to send
	 * @return the fragments of the Message
	 */
	byte[][] divideIntoFragments(byte[] M)
	{
		int PS = 28 * 1024 - FRAGMENT_HEADER_LEN - OVERHEAD;
		double EXF = 4.0 / 3.0;

		System.out.println("   divideIntoFragments");
		System.out.println("      M   = " + M.length);
		System.out.println("      PS  = " + PS);
		System.out.println("      EXF = " + EXF);

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

		// Let M = M | PRNG(Rand(KEY_LEN), Len(M) - NUM_CHUNKS*PS*K)
		byte[] random = MixMinionCryptoUtil.randomArray(KEY_LEN);
		int len = M.length - NUM_CHUNKS * PS * K;
		System.out.println(len);

		// begin  FIXME this wasn't write in the E2E, but it seems to be right
		len = Math.abs(len);
		// end    FIXME

		byte[] prng = MixMinionCryptoUtil.createPRNG(random, len);
		M = ByteArrayUtil.conc(M, prng);

		// For i from 1 to NUM_CHUNKS:
		//    Let CHUNK_i = M[(i-1)*PS*K : i*PS*K]
		// End
		byte[][] CHUNK = new byte[NUM_CHUNKS][PS];
		for (int i = 1; i <= NUM_CHUNKS; i++)
		{
			byte[] b = ByteArrayUtil.copy(M, (int) ( (i - 1) * PS * K), (int) ( /*i **/PS * K));
			CHUNK[i - 1] = b;
		}

		// Let N = Ceil(EXF*K)
		int N = (int) Math.ceil(EXF * K);

		// For i from 0 to NUM_CHUNKS-1:
		//    For j from 0 to N-1:
		//      FRAGMENTS[i*N+j] = FRAGMENT(CHUNK_i, K, N, j)
		//    End loop
		// End loop

		System.out.println("   N,num " + N + " " + NUM_CHUNKS);

		byte[][] FRAGMENTS = new byte[NUM_CHUNKS * N][28 * 1024];
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
	 * soll den von der Nachricht m das i'te von N Paketen zurueckgeben
	 */
	byte[] FRAGMENT(byte[] M, int K, int N, int I, int PS)
	{
		//k = number of source packets to encode
		//n = number of packets to encode to
		//packetsize = 1024;
		int packetsize = PS;

//		System.out.println("   " + M);

//        Random rand = new Random();

		byte[] source = M; //new byte[K*packetsize]; //this is our source file

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

		return ByteArrayUtil.copy(repairBuffer[I], repairOffs[I], packetsize);
//		return new String(repairBuffer[I], repairOffs[I], packetsize);
	}

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
	 * for testing
	 * @param args
	 */
	public static void main(String[] args)
	{
		Message mmm = new Message(null, null, 0);

		byte[] M_original = MixMinionCryptoUtil.randomArray(80000);
		byte[] M1 = (byte[]) M_original.clone();
		byte[] M2 = (byte[]) M_original.clone();

		byte[][] f1 = mmm.divideIntoFragments(M1);
		byte[][] f2 = mmm._divide_NACH_PYTHON_CODE(M2);

		System.out.println(f1);
		System.out.println(f2);

		boolean eq = true;
		for (int i = 0; i < f1.length; i++)
		{
			boolean e = ByteArrayUtil.equal(f1[i], f2[i]);
			System.out.println(e);
			eq = eq && e;
		}
		System.out.println(eq);

	}

	/**
	 * Diese Methode ist aus den PhytonCode uebernommen und anschliessend
	 * in Java umgeschrieben worden
	 */
	byte[][] _divide_NACH_PYTHON_CODE(byte[] M)
	{
		int length = M.length;
		int fragCapacity = 28 * 1024 - FRAGMENT_HEADER_LEN - OVERHEAD;
		// minimum number of payloads to hold msg, without fragmentation
		// or padding.
		int minFragments = ceilDiv(length, fragCapacity);
		if (! (minFragments >= 2))
		{
			throw new RuntimeException("The minimum of Fragments must be 2.");
		}
		// Number of data fragments per chunk.
		int k = 2;
		while (k < minFragments && k < MAX_FRAGMENTS_PER_CHUNK)
		{
			k *= 2;
		}
		// Number of chunks.
		int nChunks = ceilDiv(minFragments, k);
		// Number of total fragments per chunk.
		int n = (int) Math.ceil(EXP_FACTOR * k);
		// Data in  a single chunk
		int chunkSize = fragCapacity * k;
		// Length of data to fill chunks
		int paddedLen = nChunks * fragCapacity * k;
		// Length of padding needed to fill all chunks with data.
		int paddingLen = paddedLen - length;

		/** jetzt kommt der Fragment-Algo aus dem Python-Code **/
		//def getFragments(self, s, paddingPRNG=None):
		//    """Given a string of length self.length, whiten it, pad it,
		//       and fragmment it.  Return a list of the fragments, in order.
		//       (Note -- after building the fragment packets, be sure to shuffle
		//       them into a random order.)"""

		byte[] random = MixMinionCryptoUtil.randomArray(KEY_LEN);
		byte[] s = ByteArrayUtil.conc(M, MixMinionCryptoUtil.createPRNG(random, paddingLen));
		if (! (s.length == paddedLen))
		{
			throw new RuntimeException("The Common-Fragment has a wrong size.");
		}

		Vector chunks = new Vector();
		for (int i = 0; i < nChunks; i++)
		{
			byte[] b = ByteArrayUtil.copy(s, i * chunkSize, chunkSize);
			chunks.addElement(b);
		}

		Vector fragments = new Vector();
		for (int i = 0; i < nChunks; i++)
		{
			Vector blocks = new Vector();
			for (int j = 0; j < k; j++)
			{
				byte[] b = ByteArrayUtil.copy( (byte[]) chunks.elementAt(i), j * fragCapacity, fragCapacity);
				blocks.addElement(b);
			}
			for (int j = 0; j < n; j++)
			{
				byte[] b = FRAGMENT(s, k, n, j, fragCapacity);
				fragments.addElement(b);
			}
		}

		byte[][] allFragments = new byte[fragments.size()][fragCapacity];
		for (int i = 0; i < fragments.size(); i++)
		{
			allFragments[i] = (byte[]) fragments.elementAt(i);
		}
		return allFragments;
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

}

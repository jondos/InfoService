package anon.tor.cells;

import org.bouncycastle.crypto.digests.SHA1Digest;
import anon.tor.crypto.CTRBlockCipher;
import anon.tor.util.helper;

/**
 * @author stefan
 *
 */
public class RelayCell extends Cell
{

	public final static byte RELAY_BEGIN = 1;
	public final static byte RELAY_DATA = 2;
	public final static byte RELAY_END = 3;
	public final static byte RELAY_CONNECTED = 4;
	public final static byte RELAY_SENDME = 5;
	public final static byte RELAY_EXTEND = 6;
	public final static byte RELAY_EXTENDED = 7;
	public final static byte RELAY_TRUNCATE = 8;
	public final static byte RELAY_TRUNCATED = 9;
	public final static byte RELAY_DROP = 10;

	private byte m_relayCommand;
	private int m_streamID;
	private boolean m_digestGenerated;

	/**
	 * Constructor for a relay cell
	 */
	public RelayCell()
	{
		super(3);
	}

	/**
	 * Constructor for a relay cell
	 *
	 * @param circID
	 * circID
	 */
	public RelayCell(int circID)
	{
		super(3, circID);
		this.m_digestGenerated = false;
	}

	/**
	 * Constructor for a relay cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public RelayCell(int circID, byte[] payload)
	{
		super(3, circID, payload);
		this.m_relayCommand = payload[0];
		this.m_streamID = ( (payload[3] & 0xFF) << 8) | (payload[4] & 0xFF);
		this.m_digestGenerated = false;
	}

	public RelayCell(int circID, byte relaycommand, int streamid, byte[] data)
	{
		super(3, circID, createPayload(relaycommand, streamid, data));
		this.m_relayCommand = relaycommand;
		this.m_streamID = streamid;
		this.m_digestGenerated = false;
	}

	public byte getRelayCommand()
	{
		return this.m_relayCommand;
	}

	public int getStreamID()
	{
		return this.m_streamID;
	}

	public void generateDigest(SHA1Digest digest)
	{
		if (!this.m_digestGenerated)
		{
			digest.update(this.m_payload, 0, this.m_payload.length);
			SHA1Digest sha = new SHA1Digest(digest);
			byte[] d = new byte[sha.getDigestSize()];
			sha.doFinal(d, 0);
			for (int i = 0; i < 4; i++)
			{
				this.m_payload[i + 5] = d[i];
			}
			this.m_digestGenerated = true;
		}
	}

	public void checkDigest(SHA1Digest digest) throws Exception
	{
		digest.update(this.m_payload, 0, 5);
		digest.update(new byte[4], 0, 4);
		digest.update(this.m_payload, 9, this.m_payload.length - 9);
		SHA1Digest sha = new SHA1Digest(digest);
		byte[] d = new byte[sha.getDigestSize()];
		sha.doFinal(d, 0);
		for (int i = 0; i < 4; i++)
		{
			if (this.m_payload[i + 5] != d[i])
			{
				throw new Exception("Wrong Digest detected");
			}
		}
		this.m_digestGenerated = true;
	}

	public void doCryptography(CTRBlockCipher engine)
	{
		byte[] data = new byte[this.m_payload.length];
		engine.processBlock(this.m_payload, 0, data, 0,509);
		this.m_payload = helper.copybytes(data, 0, 509);
		this.m_relayCommand = this.m_payload[0];
		this.m_streamID = ( (this.m_payload[3] & 0xFF) << 8) | (this.m_payload[4] & 0xFF);
	}

	private static byte[] createPayload(byte relaycommand, int streamid, byte[] data)
	{
		byte[] b = new byte[]
			{
			relaycommand, 0x00, 0x00, }; //relaycommand + 2bytes Recognized
		b = helper.conc(b, helper.inttobyte(streamid, 2)); //streamid
		b = helper.conc(b, new byte[4]); //4bytes digest
		if(data==null)
		{
			data=new byte[498];
		}
		if (data.length < 499)
		{
			b = helper.conc(b, helper.inttobyte(data.length, 2)); //length
			b = helper.conc(b, data); //data
		}
		else //copy only the first 498 bytes into the payload and forget the rest
		{
			b = helper.conc(b, helper.inttobyte(498, 2)); //length
			b = helper.conc(b, helper.copybytes(data, 0, 498)); //data
		}
		return b;
	}

	public byte[] getCellData()
	{
		if (this.m_digestGenerated)
		{
			return super.getCellData();
		}
		else
		{
			return null;
		}
	}

}

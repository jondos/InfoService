package anon.tor.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implements the Segmented Integer Counter (SIC) mode on top of a simple
 * block cipher. This mode is also know as CTR mode.
 */
public class CTRBlockCipher
{
	private BlockCipher cipher = null;
	private int blockSize;
	private byte[] IV;
	private byte[] counter;
	private byte[] counterOut;
	private boolean encrypting;
	private int pos;

	/**
	 * Basic constructor.
	 *
	 * @param c the block cipher to be used.
	 */
	public CTRBlockCipher(BlockCipher c)
	{
		this.cipher = c;
		this.blockSize = cipher.getBlockSize();
		this.IV = new byte[blockSize];
		this.counter = new byte[blockSize];
		this.counterOut = new byte[blockSize];
		this.pos = 0;
	}

	/**
	 * return the underlying block cipher that we are wrapping.
	 *
	 * @return the underlying block cipher that we are wrapping.
	 */
	public BlockCipher getUnderlyingCipher()
	{
		return cipher;
	}

	public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException
	{
		this.encrypting = forEncryption;

		if (params instanceof ParametersWithIV)
		{
			ParametersWithIV ivParam = (ParametersWithIV) params;
			byte[] iv = ivParam.getIV();
			System.arraycopy(iv, 0, IV, 0, IV.length);

			reset();
			cipher.init(true, ivParam.getParameters());
		}
	}

	public String getAlgorithmName()
	{
		return cipher.getAlgorithmName() + "/CTR";
	}

	public int getBlockSize()
	{
		return cipher.getBlockSize();
	}

	public void processBlock(byte[] in, int inOff, byte[] out, int outOff, int len) throws DataLengthException,
		IllegalStateException
	{
		while (len > 0)
		{

			if (this.pos == 0)
			{
				cipher.processBlock(counter, 0, counterOut, 0);
			}

			while(pos<counterOut.length)
			{
				out[outOff] = (byte) (counterOut[pos] ^ in[inOff]);
				outOff++;
				inOff++;
				len--;
				pos++;
				if (len == 0)
				{
					return;
				}
			}
			this.pos = 0;
			//
			// XOR the counterOut with the plaintext producing the cipher text
			//

			int carry = 1;

			for (int i = counter.length - 1; i >= 0; i--)
			{
				int x = (counter[i] & 0xff) + carry;

				if (x > 0xff)
				{
					carry = 1;
				}
				else
				{
					carry = 0;
				}

				counter[i] = (byte) x;
			}
		}
	}

	public void reset()
	{
		System.arraycopy(IV, 0, counter, 0, counter.length);
		cipher.reset();
	}
}

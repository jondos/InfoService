package proxy;

interface IAnonProxy
{
	void incNumChannels();
	void decNumChannels();
	void transferredBytes(int bytes);
	public static final int E_SUCCESS = 0;
}

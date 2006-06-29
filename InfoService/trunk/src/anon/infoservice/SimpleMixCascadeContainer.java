package anon.infoservice;

/**
 * Takes and returns a single MixCascade.
 *
 * @author Rolf Wendolsky
 */
public class SimpleMixCascadeContainer extends AbstractMixCascadeContainer
{
	private MixCascade m_mixCascade;

	public SimpleMixCascadeContainer(MixCascade a_mixCascade)
	{
		m_mixCascade = a_mixCascade;
	}
	public MixCascade getNextMixCascade()
	{
		return m_mixCascade;
	}
	public MixCascade getCurrentMixCascade()
	{
		return m_mixCascade;
	}

	public boolean isCascadeAutoSwitched()
	{
		return false;
	}

	public boolean isReconnectedAutomatically()
	{
		return false;
	}


	public void keepCurrentCascade(boolean a_bKeepCurrentCascade)
	{
	}
}

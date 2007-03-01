package anon.tor.ordescription.test;

import junitx.framework.PrivateTestCase;

import anon.tor.ordescription.PlainORListFetcher;

public class PlainORListFetcherTest extends PrivateTestCase 
{
	// use following server for testing
	public final static String TEST_DIR_ADDR = "18.244.0.188";
	public final static int    TEST_DIR_PORT = 9032;
	
	private PlainORListFetcher m_lf;
	
	public PlainORListFetcherTest(String s)
	{
		super(s);
	}
	
	public void setUp()
	{
		m_lf = new PlainORListFetcher(TEST_DIR_ADDR,TEST_DIR_PORT);
	}
	
	public void tearDown()
	{
		
	}
	
	public void testStatus()
	{
		byte[] b = m_lf.getRouterStatus();
		assertTrue(b != null);
	}
	
	public void testDescriptor()
	{
		byte[] b = m_lf.getAllDescriptors();
		assertTrue(b != null);
	}
}

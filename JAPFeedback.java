import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

public final class JAPFeedback implements Runnable {
	
	private JAPModel model;
	private boolean runFlag = true;
	
	public JAPFeedback()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPFeedback:initializing...");
			model = JAPModel.getModel();
		}
		
	public void run()
		{
			runFlag = true;
			while(runFlag)
				{
					if (model.isAnonMode())
						{
							model.getInfoService().getFeedback();
						}
					try 
						{
							Thread.sleep(60000);
						}
					catch (Exception e)
						{}
				}
		}
	
	public void stopRequests() 
		{
			runFlag = false;
			this.notify();
		}

}

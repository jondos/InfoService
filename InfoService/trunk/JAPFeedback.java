import java.util.*;
import java.net.*;
import java.io.*;

import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import com.sun.xml.tree.XmlDocument;

public class JAPFeedback implements Runnable {
	
	public static final String DP = "%3A"; // Doppelpunkt 
	boolean runFlag = true;
	JAPModel model;
	
	public JAPFeedback() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPFeedback:initializing...");
		this.model = JAPModel.getModel();
	}
		
	public void run() {
		//int counter = 0;
		boolean error;
		runFlag = true;
		String path = "http://"+model.infoServiceHostName+":"+model.infoServicePortNumber+
		  			  "/feedback/"+model.anonHostName+DP+model.anonPortNumber;
		while(runFlag) {
			if (model.isAnonMode()) {
				String s = "";
				int nrOfActiveUsers = -1;
				int trafficSituation = -1;
				int currentRisk = -1;
				try {
					error = false;
					URL url=new URL(path);
					Socket socket = new Socket(url.getHost(),((url.getPort()==-1)?80:url.getPort()));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					out.write("GET "+url.getFile()+" HTTP/1.0\r\n\r\n");
					out.flush();
					DataInputStream in=new DataInputStream(socket.getInputStream());
					String line = readLine(in);
					if (line.indexOf("200") == -1) {
						error = true;
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPFeedback: Bad response from server: "+line);
					}
					// read remaining header lines
					while (line.length() != 0) {
						line = readLine(in);
					}
					
					if (error == false) {
						// XML stuff
						InputSource ins = new InputSource(in);
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ins);
						NamedNodeMap n=doc.getFirstChild().getAttributes();
						
						s                = n.getNamedItem("anonServer").getNodeValue();
						nrOfActiveUsers  = Integer.valueOf(n.getNamedItem("nrOfActiveUsers").getNodeValue()).intValue();
						trafficSituation = Integer.valueOf(n.getNamedItem("currentRisk").getNodeValue()).intValue();
						currentRisk      = Integer.valueOf(n.getNamedItem("trafficSituation").getNodeValue()).intValue();
					}
					// close streams and socket
					in.close();
					out.close();
					socket.close();
				}
				catch(Exception e) {
					error = true;
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPFeedback: "+e);
				}
				if (error == false) {
						model.nrOfActiveUsers  = nrOfActiveUsers;
						model.trafficSituation = trafficSituation;
						model.currentRisk      = currentRisk;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPFeedback: "+nrOfActiveUsers+"/"+trafficSituation+"/"+currentRisk);
				} else {
						model.nrOfActiveUsers  = -1;
						model.trafficSituation = -1;
						model.currentRisk      = -1;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPFeedback: -1/-1/-1");
				}
				// fire event
				model.notifyJAPObservers();
			}
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
			}
		}
	}
	
    private String readLine(DataInputStream inputStream) throws Exception {
		String returnString = "";
		try{
			int byteRead = inputStream.read();
			while (byteRead != 10 && byteRead != -1) {
			if (byteRead != 13) returnString += (char)byteRead;
			byteRead = inputStream.read();
			}
		} catch (Exception e) {
			throw e;
		}
		return returnString;
    }
	
    public void stopRequests() {
		runFlag = false;
	}

}

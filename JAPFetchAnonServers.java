import java.net.*;
import java.io.*;

public final class JAPFetchAnonServers {
	JAPModel model;

	public JAPFetchAnonServers() {
		this.model = JAPModel.getModel();
	}
	
	public void fetch() throws Exception
		{
			String path = "http://"+model.getInfoServiceHost()+":"+model.getInfoServicePort()+
		  			  "/servers";
			try
				{
					URL url=new URL(path);
					Socket socket = new Socket(url.getHost(),((url.getPort()==-1)?80:url.getPort()));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					out.write("GET "+url.getFile()+" HTTP/1.0\r\n\r\n");
					out.flush();
					DataInputStream in=new DataInputStream(socket.getInputStream());
					String line = readLine(in);
					if (line.indexOf("200") == -1) {
						throw new Exception("JAPFetchAnonServers: Bad response from server: "+line);
					}
					// read remaining header lines
					while (line.length() != 0) {
						line = readLine(in);
				}
			// XML stuff
/*						InputSource ins = new InputSource(in);
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ins);
			NamedNodeMap n=doc.getFirstChild().getAttributes();
			
			s                = n.getNamedItem("anonServer").getNodeValue();
			nrOfActiveUsers  = Integer.valueOf(n.getNamedItem("nrOfActiveUsers").getNodeValue()).intValue();
			trafficSituation = Integer.valueOf(n.getNamedItem("currentRisk").getNodeValue()).intValue();
			currentRisk      = Integer.valueOf(n.getNamedItem("trafficSituation").getNodeValue()).intValue();
*/
			// close streams and socket
			in.close();
			out.close();
			socket.close();
		}
		catch(Exception e) {
			throw e;
//			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPFetchAnonServers: "+e);
		}
		// fire event
		model.notifyJAPObservers();
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
	
}

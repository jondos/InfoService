public class AnonServerDBEntry {
	public String name;
	public String host;
	public int    port;
	
	public AnonServerDBEntry (String host, int port) {
		this.name = host + ":" + port;
		this.host = host;
		this.port = port;
	}
}

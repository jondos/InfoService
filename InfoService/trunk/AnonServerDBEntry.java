final public class AnonServerDBEntry 
	{
		private String host;
		private int    port;
	
		public AnonServerDBEntry (String host, int port)
			{
				host = host;
				port = port;
			}
		
		public String getName()
			{
				return host+":"+Integer.toString(port);
			}
		
		public int getPort()
			{
				return port;
			}
			
		public String getHost()
			{
				return host;
			}
	}

final public class AnonServerDBEntry 
	{
		private String host;
		private int    port;
	
		public AnonServerDBEntry (String h, int p)
			{
				host = h;
				port = p;
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

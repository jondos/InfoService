public final class AnonServerDBEntry 
	{
		private String host;
		private int    port;
		private String name;
	
		public AnonServerDBEntry (String n,String h, int p)
			{
				host = h;
				port = p;
				name=n;
			}
		
		public String getName()
			{
				return name;//host+":"+Integer.toString(port);
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

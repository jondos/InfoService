import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;

public class CAVersion
	{
		private static String aktVersion="00.00.007";
		public static int checkForNewVersion(JAPModel model)
			{
				try
					{
						byte[] buff=new byte[9];
						URL url=new URL(model.url_info_version);
						DataInputStream in=new DataInputStream(url.openStream());
						in.readFully(buff);
						in.close();
						String s=new String(buff);
						System.out.println("Version: "+s);
						if(s.compareTo(aktVersion)>0)
							return 1;
						return 0;
					}
				catch(Exception e)
					{
						return -1;
					}
			}
		public static int getNewVersion(JAPModel model)
			{
				try
					{
						URL url=new URL(model.url_jap_newversion);
						URLConnection urlconn=url.openConnection();
						System.out.println("Hier");
						int len=urlconn.getContentLength();
						if(len==-1)
							{
								System.out.println("unkown Size");
								return -1;
							}
						System.out.println("Size: "+Integer.toString(len));
						byte[] buff=new byte[len];
						DataInputStream in=new DataInputStream(urlconn.getInputStream());
						in.readFully(buff);
						in.close();
						FileOutputStream f=new FileOutputStream("JAP.jar");
						f.write(buff);
						f.flush();
						f.close();
		//				System.out.println(this
						return 0;
					}
				catch(Exception e)
					{
						return -1;
					}
				
			}
	}

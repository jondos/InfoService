import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import org.w3c.dom.Node;

public final class JAPUtil
	{
		public static int applyJarDiff(String oldJAR, String newJAR, String diffJAR)
			{
				try
					{
						ZipFile zold=null;	
						ZipInputStream zdiff=null;
						ZipOutputStream znew=null;
						ZipEntry ze=null;
			// geting old names
						zold=new ZipFile(oldJAR);
						Hashtable oldnames=new Hashtable();
						Enumeration e=zold.entries();
						while(e.hasMoreElements())
							{
								ze=(ZipEntry)e.nextElement();
								oldnames.put(ze.getName(),ze.getName());
							}
						zdiff=new ZipInputStream(new FileInputStream(diffJAR));
						znew=new ZipOutputStream(new FileOutputStream(newJAR));		
						znew.setLevel(9);
						byte[] b=new byte[5000];
						while((ze=zdiff.getNextEntry())!=null)
							{
								ZipEntry zeout=new ZipEntry(ze.getName());
								if(!ze.getName().equalsIgnoreCase("META-INF/INDEX.JD"))
									{
										System.out.println(ze.getName());
										oldnames.remove(ze.getName());
										int s=-1;
										zeout.setTime(ze.getTime());
										zeout.setComment(ze.getComment());
										zeout.setExtra(ze.getExtra());
										zeout.setMethod(ze.getMethod());
										if(ze.getSize()!=-1)
											zeout.setSize(ze.getSize());
										if(ze.getCrc()!=-1)
											zeout.setCrc(ze.getCrc());
										znew.putNextEntry(zeout);
										while((s=zdiff.read(b,0,5000))!=-1)
											{
												znew.write(b,0,s);
											}
										znew.closeEntry();
									}
								else
									{
										BufferedReader br=new BufferedReader(new InputStreamReader(zdiff));
										String s=null;
										while((s=br.readLine())!=null)
											{
												StringTokenizer st=new StringTokenizer(s);
												s=st.nextToken();
												if(s.equalsIgnoreCase("remove"))
													oldnames.remove(st.nextToken());
												else if(s.equalsIgnoreCase("move"))
													System.out.println("move "+st.nextToken());
												else
													System.out.println("unkown: "+s);										
											}
									}
								zdiff.closeEntry();
							}
						e=oldnames.elements();
						while(e.hasMoreElements())
							{
								String s=(String)e.nextElement();
								System.out.println(s);
								ze=zold.getEntry(s);
								ZipEntry zeout=new ZipEntry(ze.getName());
								zeout.setTime(ze.getTime());
								zeout.setComment(ze.getComment());
								zeout.setExtra(ze.getExtra());
								zeout.setMethod(ze.getMethod());
								if(ze.getSize()!=-1)
									zeout.setSize(ze.getSize());
								if(ze.getCrc()!=-1)
									zeout.setCrc(ze.getCrc());
								znew.putNextEntry(zeout);
								System.out.println("Getting in..");
								InputStream in=zold.getInputStream(ze);
								int l=-1;
								System.out.println("Reading..");
								try{
								while((l=in.read(b,0,5000))!=-1)
									{
										znew.write(b,0,l);
								}}
								catch(Exception er)
								{
									er.printStackTrace(System.out);
								}
								in.close();
								znew.closeEntry();
								
							}
						
						znew.finish();
						znew.flush();
						znew.close();
						zold.close();
						zdiff.close();
				}
			catch(Throwable e)
				{
					e.printStackTrace();
					return -1;
				}
			return 0;
		}
		
	public static boolean isPort(int port)
		{
			if((port<1)||(port>65536))
				return false;
			return true;
		}
	
	public static int parseNodeInt(Node n,int defaultValue)
		{
			int i=defaultValue;
			if(n!=null)
				try	
					{
						i=Integer.parseInt(n.getNodeValue());
					}
				catch(Exception e)
					{
					}
			return i;
		}

	public static boolean parseNodeBoolean(Node n,boolean defaultValue)
		{
			boolean b=defaultValue;
			if(n!=null)
				try	
					{
						String tmpStr=n.getNodeValue();
						if(tmpStr.equalsIgnoreCase("true"))
							b=true;
						else if(tmpStr.equalsIgnoreCase("false"))
							b=false;
					}
				catch(Exception e)
					{
					}
			return b;
		}

	public static String parseNodeString(Node n,String defaultValue)
		{
			String s=defaultValue;
			if(n!=null)
				try	
					{
						s=n.getNodeValue();
					}
				catch(Exception e)
					{
					}
			return s;
		}

}

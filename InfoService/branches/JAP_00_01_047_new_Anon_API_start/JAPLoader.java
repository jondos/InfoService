import java.io.File;

public final class JAPLoader
	{
		public static void main(String[] argv) 
			{
				File fnew=new File("jap.jar.new");
				if(fnew.exists())
					{//New Version found
						File fold=new File("JAP.jar");
						File fbackup=new File("jap.jar.old");
						if(!fold.exists())
							{
								fold=new File("jap.jar");
								if(!fold.exists())
									{
										System.out.println("Big Problem!");
										System.exit(-1);
									}
							}
						try
							{
								if(!fold.renameTo(fbackup))
									{
										System.out.println("Big Problem!");
										System.exit(-1);
									}
								if(!fnew.renameTo(fold))
									{
										System.out.println("Big Problem!");
										System.exit(-1);
									}									
							}
						catch(Exception e)
							{
								System.out.println("Big Problem!");
								System.exit(-1);
							}
						try
							{
								fbackup.delete();
							}
						catch(Exception e)
							{
								System.out.println("Could not delete Backup-File!");
							}
						fold=null;
						fbackup=null;
					}
				fnew=null;
				JAP jap = new JAP(argv);
				jap.startJAP();
			}
	}

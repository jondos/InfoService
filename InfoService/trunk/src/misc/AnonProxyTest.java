package misc;

import jap.JAPController;
import jap.JAPUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

import java.util.Enumeration;
import java.util.Observable;

import org.apache.log4j.Logger;

import anon.proxy.AnonProxy;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.update.AbstractMixCascadeUpdater;
import anon.infoservice.update.InfoServiceUpdater;
import logging.AbstractLog4jLog;
import logging.Log;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import logging.SystemErrLog;
import anon.client.AbstractAutoSwitchedMixCascadeContainer;
import anon.client.DummyTrafficControlChannel;
import anon.client.ITermsAndConditionsContainer;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.util.ClassUtil;
import anon.util.Configuration;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import anon.util.Updater.ObservableInfo;

public class AnonProxyTest
{
	private static InfoServiceUpdater ms_isUpdater;
	private static MixCascadeUpdater ms_cascadeUpdater;
	private static ServerSocket m_socketListener;
	private static AnonProxy m_jondonymProxy;
	
	
	public static synchronized void init(final Logger a_logger, Configuration a_configuration) throws Exception
	{		
		if (ms_isUpdater != null)
		{
			return;
		}
		
		Log templog;
		if (a_logger == null)
		{
			templog = new SystemErrLog();
		}
		else
		{
			templog = new AbstractLog4jLog()
			{
				  protected Logger getLogger()
					{
						return a_logger;
					}
			};
		}
		
   		LogHolder.setLogInstance(templog);
   		templog.setLogType(LogType.ALL);
   		templog.setLogLevel(LogLevel.WARNING);
   		
   		LogHolder.log(LogLevel.ALERT, LogType.MISC, "Initialising AnonProxyTest version 0.1");
   		
		ClassUtil.enableFindSubclasses(false); // This would otherwise start non-daemon AWT threads, blow up memory and prevent closing the app.
		XMLUtil.setStorageMode(XMLUtil.STORAGE_MODE_AGRESSIVE); // Store as few XML data as possible for memory optimization.
		SignatureVerifier.getInstance().setCheckSignatures(true);
		
		 
		addDefaultCertificates("acceptedInfoServiceCAs/", new String[] {"japinfoserviceroot.cer", "InfoService_CA.cer"}, JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE);
		addDefaultCertificates("acceptedMixCAs/", new String[] {"japmixroot.cer", "Operator_CA.cer", "Test_CA.cer.dev", "gpf_jondonym_ca.cer"}, JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX);
     
     
		 // simulate database distributor and suppress distributor warnings
		Database.registerDistributor(new IDistributor()
		{
			public void addJob(IDistributable a_distributable)
			{
			}
		});
    
		InfoServiceDBEntry[] defaultInfoService = JAPController.createDefaultInfoServices();
		for (int i = 0; i < defaultInfoService.length; i++)
		{
			Database.getInstance(InfoServiceDBEntry.class).update(defaultInfoService[i]);
		}
		InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService[0]);
		
		ObservableInfo a_observableInfo = new ObservableInfo(new Observable())
		{
			public Integer getUpdateChanged()
			{
				return new Integer(0);
			}
			public boolean isUpdateDisabled()
			{
				return false;
			}
		};
		
		ms_isUpdater = new InfoServiceUpdater(a_observableInfo);
		ms_cascadeUpdater = new MixCascadeUpdater(a_observableInfo);
	}
	
	public static void main(String[] args)
	{
		try
		{
			init(null, null);
			start();
			
			if (!isRunning())
			{
				return;
			}
			
			String entered = "";
			
			while (true)
			{
				if (entered != null)
				{
					System.out.println("Type 'exit' to quit.");
				}
				entered=null;
				try
				{
					entered = new BufferedReader(new InputStreamReader(System.in)).readLine();
				}
				catch(Throwable t)
				{
				}
				if (entered == null)
				{
					//Hm something is strange... do not simply continue but wait some time
					//BTW: That are situations when this could happen?
					// One is if JAP is run on VNC based X11 server
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
					}
					continue;
				}
				if (entered.equals("exit"))
				{
					stop();
					break;
				}
				System.out.println(m_jondonymProxy.getMixCascade().getName());
				  
	            System.out.println("Memory usage: " + 
	            		JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().totalMemory()) + ", Max VM memory: " +

						 JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().maxMemory()) + ", Free memory: " +
						 JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().freeMemory()));
			}			
			
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
		}
	}
	
	public static synchronized void stop()
	{
		if (m_jondonymProxy != null)
		{
			m_jondonymProxy.stop();
		}
		if (m_socketListener != null)
		{
			try 
			{
				m_socketListener.close();
			} 
			catch (IOException a_e) 
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, a_e);
			}
			m_socketListener = null;
		}
	}
	
	public static synchronized MixCascade getCurrentCascade()
	{
		if (m_jondonymProxy == null)
		{
			return null;
		}
		return m_jondonymProxy.getMixCascade();
	}
	
	public static synchronized boolean isRunning()
	{
		return m_socketListener != null;
	}
	
	public static synchronized boolean isConnected()
	{
		return isRunning() && m_jondonymProxy != null && m_jondonymProxy.isConnected();
	}
	
	public static synchronized void start()
	{
		if (m_socketListener != null)
		{
			return;
		}
		
		try
		{
			m_socketListener = new ServerSocket(4001);
			
	        ms_isUpdater.start(true);
	        ms_isUpdater.update();
	        ms_cascadeUpdater.start(true);
	        ms_cascadeUpdater.update();
	         
	        MixCascade cascade;
	        if (m_jondonymProxy == null || m_jondonymProxy.getMixCascade() == null)
	        {
		        cascade = (MixCascade)Database.getInstance(MixCascade.class).getRandomEntry();
	        }
	        else
	        {
	        	cascade = m_jondonymProxy.getMixCascade();
	        }
	        
	        if (cascade == null)
	        {
	        	// cascade = new MixCascade("mix.inf.tu-dresden.de", 6544);
	        	cascade = new MixCascade("none", 6544);
	        }
	        
	        m_jondonymProxy = new AnonProxy(m_socketListener, null,null);
	        m_jondonymProxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
	        m_jondonymProxy.start(new AutoSwitchedMixCascadeContainer(cascade));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
			stop();
		}
	}
	
	
	private static void addDefaultCertificates(String a_certspath, String[] a_singleCerts, int a_type)
	{
		JAPCertificate defaultRootCert = null;

		if (a_singleCerts != null)
		{
			for (int i = 0; i < a_singleCerts.length; i++)
			{
				if (a_singleCerts[i] != null)
					//	 &&(!JAPConstants.m_bReleasedVersion ||
					 //!a_singleCerts[i].endsWith(".dev")))
				{
					defaultRootCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
							"certificates/" + a_certspath + a_singleCerts[i]));
					if (defaultRootCert == null)
					{
						continue;
					}
					SignatureVerifier.getInstance().getVerificationCertificateStore().
						addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
				}
			}
		}
		String strBlockCert = null;
		/*if (JAPConstants.m_bReleasedVersion)
		{
			strBlockCert = ".dev";
		}*/
		Enumeration certificates =
			JAPCertificate.getInstance("certificates/" + a_certspath, true, strBlockCert).elements();
		while (certificates.hasMoreElements())
		{
			defaultRootCert = (JAPCertificate) certificates.nextElement();
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
		}
		/* no elements were found */
		if (defaultRootCert == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading certificates of type '" + a_type + "'.");
		}
	}
	
	
	private static class AutoSwitchedMixCascadeContainer extends AbstractAutoSwitchedMixCascadeContainer
	{
		public AutoSwitchedMixCascadeContainer(MixCascade a_cascade)
		{
			super(false, a_cascade);
		}


		public boolean isPaidServiceAllowed()
		{
			return false;
		}

		public boolean isServiceAutoSwitched()
		{
			return true;
		}
		
		public boolean isReconnectedAutomatically()
		{
			return true;
		}
		
		public ITermsAndConditionsContainer getTCContainer()
		{
			return null;
		}
	}	
	
	private static class MixCascadeUpdater extends AbstractMixCascadeUpdater
	{
		public MixCascadeUpdater(ObservableInfo a_observableInfo)
		{
			super(a_observableInfo);
		}

		protected AbstractDatabaseEntry getPreferredEntry()
		{
			return null; // set current cascade here!
		}

		protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
		{
			// do nothing
		}
	}

}

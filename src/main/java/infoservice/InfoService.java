/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Locale;
import java.util.Observer;
import java.util.Observable;

import infoservice.performance.PerformanceMeter;
import anon.infoservice.Constants;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.Database;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.ListenerInterface;
import anon.terms.template.TermsAndConditionsTemplate;
import anon.infoservice.update.AccountUpdater;
import anon.util.JAPMessages;
import anon.util.ThreadPool;
import anon.util.TimedOutputStream;
import anon.util.XMLParseException;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class InfoService implements Observer
	{
		/**
		 * This is the version number of the infoservice software.
		 */
		public static final String INFOSERVICE_VERSION = "IS.09.010"; //never change the layout of this line!

		protected JWSInternalCommands oicHandler;

		private static int m_connectionCounter;
		private static PerformanceMeter ms_perfMeter;
		private static AccountUpdater ms_accountUpdater;

		private String m_configFileName;

		protected ThreadPool m_ThreadPool;

		public static void main(String[] argv)
			{
				String fn = null;
				String strPasswd = null;
				if (argv.length >= 1)
					{
						fn = argv[0].trim();
					}
				if (fn != null)
					{
						if (fn.equalsIgnoreCase("--generatekey"))
							{
								int i = 1;
								String isName = null;
								String passwd = null;
								try
									{
										while (i < argv.length)
											{
												String arg = argv[i].trim();
												if (arg.equals("--name"))
													{
														i++;
														isName = argv[i].trim();
													}
												else if (arg.equals("--passwd"))
													{
														i++;
														passwd = argv[i].trim();
													}
												i++;
											}
									}
								catch (Throwable t)
									{
									}
								InfoService.generateKeyPair(isName, passwd);
								System.exit(0);
							}
						else if (fn.equalsIgnoreCase("--version"))
							{
								System.out.println("InfoService version: " + INFOSERVICE_VERSION);
								System.exit(0);
							}
						else if (fn.equalsIgnoreCase("--password"))
							{
								strPasswd = argv[1].trim();
								if (argv.length > 2)
									fn = argv[2].trim();
								else
									fn = null;
							}
					}

				// start the InfoService
				try
					{
						InfoService s1 = new InfoService(fn, strPasswd);

						// start info service server
						s1.startServer();

						//			 configure the performance meter
						if (Configuration.getInstance().isPerfEnabled())
							{
								LogHolder.log(LogLevel.NOTICE, LogType.NET, "Starting Performance Meter...");

								ms_accountUpdater = new AccountUpdater(false);
								ms_accountUpdater.start(false);
								ms_perfMeter = new PerformanceMeter(ms_accountUpdater);
								Thread perfMeterThread = new Thread(InfoService.ms_perfMeter);
								perfMeterThread.start();
							}
						else
							{
								InfoService.ms_perfMeter = null;
							}

						/*SignalHandler handler = new SignalHandler();
						handler.addObserver(s1);
						handler.addSignal("HUP");
						handler.addSignal("TERM");
						*/

						JAPMessages.setLocale(Locale.ENGLISH);

						System.out.println("InfoService is running!");

						Thread tacLoader = new Thread()
							{
								public void run()
									{
										while (true)
											{
												try
													{
														Thread.sleep(1000 * 60 * 60 * 5);
													}
												catch (InterruptedException ex)
													{
														break;
													}
												try
													{
														loadTemplatesFromDirectory(Configuration.getInstance().getTermsAndConditionsDir());
													}
												catch (SignatureException e)
													{
														LogHolder.log(LogLevel.EMERG, LogType.CRYPTO, e);
													}
											}
									}
							};
						loadTemplatesFromDirectory(Configuration.getInstance().getTermsAndConditionsDir());

						tacLoader.start();
					}
				catch (Exception e)
					{
						System.out.println("Cannot start InfoService...");
						e.printStackTrace();
						System.exit(1);
					}
			}

		public void update(Observable a_ob, Object a_args)
			{
				if (a_args == null || a_args.toString() == null)
					{
						return;
					}

				String signal = a_args.toString();

				if (signal.equals("SIGHUP"))
					{
						System.out.println("Reloading configuration...");
						LogHolder.log(LogLevel.ALERT, LogType.ALL, "Caught SIGHUP. Reloading config...");

						try
							{
								loadConfig(null);
								ms_perfMeter.init();
							}
						catch (Exception ex)
							{
								System.out.println("Could not load configuration. Exiting...");
								LogHolder.log(LogLevel.ALERT, LogType.ALL, "Could not load configuration. Exiting...");
							}
					}

				if (signal.equals("SIGTERM"))
					{
						System.out.println("Exiting...");
						LogHolder.log(LogLevel.ALERT, LogType.ALL, "Caught SIGTERM. Exiting...");

						stopServer();

						System.exit(1);
					}
			}

		/**
		 * Generates a key pair for the infoservice
		 */
		private static void generateKeyPair(String isName, String passwd)
			{
				try
					{
						System.out.println("Start generating new KeyPair (this can take some minutes)...");
						KeyGenTest.generateKeys(isName, passwd);
						System.out.println("Finished generating new KeyPair!");
					}
				catch (Exception e)
					{
						System.out.println("Error generating KeyPair!");
						e.printStackTrace();
					}
			}

		private InfoService(String a_configFileName, String strPasswd) throws Exception
			{
				m_configFileName = a_configFileName;

				loadConfig(strPasswd);

				m_connectionCounter = 0;
			}

		public static void loadTemplatesFromDirectory(File a_dir) throws SignatureException
			{
				File file = null;

				if (a_dir == null)
					{
						return;
					}

				String[] files = a_dir.list();

				if (files == null)
					{
						return;
					}

				/* Loop through all files in the directory to find XML files */
				for (int i = 0; i < files.length; i++)
					{
						try
							{
								file = new File(a_dir.getAbsolutePath() + File.separator + files[i]);
								TermsAndConditionsTemplate tac = new TermsAndConditionsTemplate(file);
								if (!tac.isVerified())
									{
										throw new SignatureException("Cannot verify tac template file: " + tac.getId());
									}

								Database.getInstance(TermsAndConditionsTemplate.class).update(tac);
							}
						catch (XMLParseException ex)
							{
								LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "XMLParseException while loading Terms & Conditions: ",
										ex);
							}
						catch (IOException ex)
							{
								LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "IOException while loading Terms & Conditions: ", ex);
							}
					}
			}

		private void loadConfig(String strPasswd) throws Exception
			{
				Properties properties = new Properties();
				if (m_configFileName == null)
					{
						m_configFileName = Constants.DEFAULT_RESSOURCE_FILENAME;
					}
				try
					{
						properties.load(new FileInputStream(m_configFileName));
					}
				catch (Exception a_e)
					{
						System.out.println("Error reading configuration!");
						System.out.println(a_e.getMessage());
						System.exit(1);
					}
				new Configuration(properties, strPasswd);
			}

		private void startServer() throws Exception
			{
				HTTPConnectionFactory.getInstance().setTimeout(Constants.COMMUNICATION_TIMEOUT);
				/* initialize Distributor */

				/* initialize internal commands of InfoService */
				oicHandler = new InfoServiceCommands();
				/* initialize propagandist for our infoservice */
				if (!Configuration.getInstance().isPassive())
					{
						InfoServicePropagandist.generateInfoServicePropagandist(ms_perfMeter);
						Database.registerDistributor(InfoServiceDistributor.getInstance());
					}
				else
					{
						// suppress distributor warnings
						Database.registerDistributor(new IDistributor()
							{
								public void addJob(IDistributable a_distributable)
									{
									}
							});
						//in passive mode we obtain our information by requesting it from other services
						PassiveInfoServiceInitializer.init();
					}
				// start server
				LogHolder.log(LogLevel.EMERG, LogType.MISC, "InfoService -- Version " + INFOSERVICE_VERSION);
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.version"));
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.vendor"));
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.home"));
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.name"));
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.arch"));
				LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.version"));
				m_ThreadPool = new ThreadPool("ISConnection", Configuration.getInstance().getNrOfConcurrentConnections());
				TimedOutputStream.init();
				Enumeration<ListenerInterface> enumer = Configuration.getInstance().getHardwareListeners().elements();
				while (enumer.hasMoreElements())
					{
						InfoServiceServer server = new InfoServiceServer(enumer.nextElement(), this);
						Thread currentThread = new Thread(server, server.toString());
						currentThread.setDaemon(true);
						currentThread.start();
					}
			}

		private void stopServer()
			{
				// TODO: implement
			}

		protected static int getConnectionCounter()
			{
				return m_connectionCounter++;
			}

		protected static PerformanceMeter getPerfMeter()
			{
				return ms_perfMeter;
			}
	}

/*
Copyright (c) 2000, The JAP-Team
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

package update;

import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizard;
import gui.wizard.*;

import anon.infoservice.*;

import java.io.File;
import java.io.*;
import java.util.Vector;


import java.net.URL;

import jap.JAPConstants;
import jap.JAPMessages;
import jap.JAPUtil;
import jap.JAPController;

public final class JAPUpdateWizard extends gui.wizard.BasicWizard implements Runnable
	{
		public JAPWelcomeWizardPage welcomePage;
		public JAPDownloadWizardPage downloadPage;
		public JAPFinishWizardPage finishPage;
		private BasicWizardHost host;

		private Vector m_Pages;


		//private JAPUpdateWizard updateWizard;

		//private String pathToJapJar;
		//fileName->> Name of the chosen File without extension and path i.e. 'Jap'
		//extension->> the extension of the chosen File ie. '.jar'
		// path ->> path to the chosen File without extension i.e. 'C:\Programme\'
		private String m_strAktJapJarFileName;
		private String m_strAktJapJarExtension;
		private String m_strAktJapJarPath;

		private final static String EXTENSION_BACKUP = ".backup";
		private final static String EXTENSION_NEW = ".new";

		private boolean updateAborted = false;
		private boolean incrementalUpdate = false; // should be true by default
		//which version chose the user
		private String version;
		//which type dev or rel?
		private JAPVersionInfo japVersionInfo;

		//aktJapJar --> the original JAP.jar; cp_aktJapJar --> the copy of the original File extended by the current version-number
		//i.e. JAPaktVersion.jar; cp_updJapJar --> the downloaded Upgrade extended by the version-number
		//cp_updJapJar --> copy of the newJarFile without version-number named JAP.jar

		private File m_fileAktJapJar;
		private File m_fileJapJarCopy;
		private File m_fileNewJapJar;
		private File updJapJar;

		private int countPackages = 0;
		//private int countBytes = 0;
		//private int value = 0;
		//private int totalLength = 0;
		private byte[] m_arBufferNewJapJar=null;

		private Thread updateThread;
		//private UpdateListener updateListener;

		public JAPUpdateWizard(String version, JAPVersionInfo info)
			{
				this.version = version;
				japVersionInfo = info;
				//updateWizard = this;
				setWizardTitle("JAP Update Wizard");
				welcomePage = new JAPWelcomeWizardPage();
				downloadPage = new JAPDownloadWizardPage(version);
				finishPage = new JAPFinishWizardPage("");

				addWizardPage(0,welcomePage);
				addWizardPage(1,downloadPage);
				addWizardPage(2,finishPage);
				m_Pages = getPageVector();
				host = new BasicWizardHost(JAPController.getView(),this);
				invokeWizard(host);
			}

		private void startUpdateThread()
				{
/*					 updateListener = new UpdateListener(){
						public int progress(int lenData, int lenTotal, int state)
								{// set the icons and the progressbar values
								//check Abbruchbedingung --> return -1
								//boolean updateAborted = false;
								if(updateAborted)
								{
									 resetChanges();
									 return -1;
								}
 if(state == 7)//createNewJapJar()
											{
																	 downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
																	 countPackages = 0;
																	 countPackages += lenData;
																// the first step has the Zone from 455 to 490 in the ProgressBar
																 int value = (35 * countPackages)/lenTotal;
																 System.out.println(value+ " value 3th step");
																 downloadPage.progressBar.setValue((value+455));
																 downloadPage.progressBar.repaint();
																 return 0;
											}else if(state == 8)//creation of new JARFile failed
											{
																	//set blank

																	downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep3"));
																	resetChanges();

																	return -1;
											}else if(state == 9)// finshed creation of the new JarFile
											{
																 // downloadPage.m_labelIconStep3.setVisible(false);

																 downloadPage.m_labelIconStep3.setIcon(downloadPage.stepfinished);
																// downloadPage.m_labelIconStep3.setVisible(false);
																	 return 0;
											}else if(state == 10)//not needed yet
											{
													return 0;
											}else if(state == 11)//not needed yet
											{
													return 0;
											}else if(state == 12)//not needed yet
											{
													return 0;
											}else if(state == 13)//write the new JARFile in the directory
											{
																	 downloadPage.m_labelIconStep5.setIcon(downloadPage.arrow);

																	 countPackages += lenData;
																// the 5th step has the Zone from 490 to 500 in the ProgressBar
																	 int value = (10 * countPackages)/lenTotal;
																	 downloadPage.progressBar.setValue((value+490));
																	 downloadPage.progressBar.repaint();
																	return 0;
											}else if(state == 14)
											{
																 downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep5"));
																 resetChanges();
																 return -1;
											}else if(state == 15)
											{
																 downloadPage.m_labelIconStep5.setIcon(downloadPage.stepfinished);
																 host.setNextEnabled(true);
																 host.setFinishEnabled(false);
																 host.setCancelEnabled(false);
																 return 0;
											}else
											{
																	return -1;
											}

								}

					 };
*/
								updateThread = new Thread(this);
								updateThread.start();


				}

		public void run()
				{
				 // Start with Step 1 copy
				if(renameJapJar()!=0)
					{

						return;
					}
				// Step 2
				 if(downloadUpdate()!=0)
				 {
						return;
				 }
				//Step 3 or 3'
				if(incrementalUpdate)
					{
						if(applyJARDiffJAPJar()!=0)
							{
								return;
							}
					}
				else
					{
						if( createNewJAPJar()!=0)
							{
								return;
							}
					 }
				// Step 5
				if( overwriteJapJar()!=0)
					{
						return;
					}
				try {
							 // if(!aktJapJar.delete()) {downloadPage.showInformationDialog("Deleting original JAP.jar failed");}
							 // if(!updJapJar.renameTo(new File(path+extension))) {downloadPage.showInformationDialog("Renaming JAP_temp.jar failed.");}
								if(!m_fileNewJapJar.delete())
									{
										downloadPage.showInformationDialog("Deleting of JAP_new.jar failed!");
										resetChanges();
										return;
									}
									host.setNextEnabled(true);
									host.setFinishEnabled(false);
									host.setCancelEnabled(false);

						}
				catch(Exception e)
						{
								e.printStackTrace();
								downloadPage.showInformationDialog(e.toString());
								return;
						}

				}



		private void setJapJarFile (File japjarfile)
			{
				m_fileAktJapJar=japjarfile;
				parsePathToJapJar();
				//Setting the Texts according to the Jap.jar File choosen...
				String strFileNameJapJarBackup=m_strAktJapJarPath+m_strAktJapJarFileName+
																			JAPConstants.aktVersion+EXTENSION_BACKUP+m_strAktJapJarExtension;
				downloadPage.m_labelStep1_1.setText(JAPMessages.getString("updateM_labelStep1Part1"));
				downloadPage.tfSaveFrom.setText(m_fileAktJapJar.getAbsolutePath());
				downloadPage.m_labelStep1_2.setText(JAPMessages.getString("updateM_labelStep1Part2"));
				downloadPage.tfSaveTo.setText(strFileNameJapJarBackup);
				downloadPage.m_labelStep3.setText(JAPMessages.getString("updateM_labelStep3Part1")+" "+
																					m_strAktJapJarFileName+version+EXTENSION_NEW+
																					m_strAktJapJarExtension+
																					JAPMessages.getString("updateM_labelStep3Part2"));
				finishPage.tf_BackupOfJapJar.setText(strFileNameJapJarBackup);

			}

	/*	public String getSelectedFile()
			{
				return selectedFile;
			}
*/
		// called by the WizardHost when the User's clicked next
/*		public void startUpdate()
			{
				//start the Update
				setPath(selectedFile);
			}
*/
		 public WizardPage next(WizardPage currentPage, WizardHost host)
	 {

			int pageIndex=m_Pages.indexOf(currentPage);
			pageIndex++;
			host.setBackEnabled(true);
			//already the last page --> tell the user that Jap exits itself

			//next page is FinishWizardPage
			if(pageIndex==m_Pages.size()-1)
				{
					host.setFinishEnabled(true);
					host.setNextEnabled(false);
					try{
					updateThread.join();
						}catch(InterruptedException ie)
						{
								ie.printStackTrace();
						}
				}
			//if it is the DownloadWizardPage
			if(pageIndex == 1)
				{
						host.setBackEnabled(false);
						host.setFinishEnabled(false);
						host.setNextEnabled(false);
						setJapJarFile(welcomePage.getJapJarFile());
						host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
						startUpdateThread();


				}else
				{
						host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
				}

			return null;
	 }

	 public WizardPage finish(WizardPage currentPage, WizardHost host)
	 {
			//super.finish(currentPage,host);
			System.exit(0);
			return null;
	 }

	 public WizardPage back(WizardPage currentPage, WizardHost host)
	 {
			int pageIndex=m_Pages.indexOf(currentPage);
			// we are on the Finishpage --> don't go back to the first page
			if (pageIndex == (m_Pages.size()-1))
			{
					host.setBackEnabled(false);
			}

			super.back(currentPage,host);
			return null;
	 }

	 public void wizardCompleted()
	 {
		 try{
				 updateAborted = true;
				// updateThread.join();
				 //updateListener.progress(0,0,20);
				 }catch(Exception e)
				 {
					e.printStackTrace();
				 }
	 }

//User's clicked next and the path to the chosen Jar-File is being set
		/* public void setPath(String pathToJapJar)
			{

				this.pathToJapJar = pathToJapJar;

			}
*/
		private void parsePathToJapJar()
			{
				try
					{
						m_strAktJapJarFileName = m_fileAktJapJar.getName();
						m_strAktJapJarPath = m_fileAktJapJar.getCanonicalPath();
						m_strAktJapJarPath=m_strAktJapJarPath.substring(0,m_strAktJapJarPath.length()-
																														 m_strAktJapJarFileName.length());
						m_strAktJapJarExtension = m_fileAktJapJar.getName();
						int i=m_strAktJapJarExtension.lastIndexOf('.');
						m_strAktJapJarExtension=m_strAktJapJarExtension.substring(i);
						m_strAktJapJarFileName=m_strAktJapJarFileName.substring(0,i);
					}
				catch(Exception e)
					{
						e.printStackTrace();
					}
			}


		 //Step 1
			private int renameJapJar()
				{
					byte[]buffer = new byte[2048];
					//just copy the File and then rename the copy
					downloadPage.m_labelIconStep1.setIcon(downloadPage.arrow);
					try
						{
							//newFile = new File(prefix+JAPConstants.aktVersion2+suffix);
							m_fileJapJarCopy = new File(m_strAktJapJarPath+m_strAktJapJarFileName+
																					JAPConstants.aktVersion+EXTENSION_BACKUP+
																					m_strAktJapJarExtension);
							FileInputStream fis = new FileInputStream(m_fileAktJapJar);
							FileOutputStream fos = new FileOutputStream(m_fileJapJarCopy);
							int len;
							int totalLength = (int)m_fileAktJapJar.length();
							while ((len = fis.read(buffer))!=-1)
								{
									fos.write(buffer,0,len);
									totalLength-=len;
								}
							fis.close();
							fos.flush();
							fos.close();
							//TODO
							//if totalLength!=0 ...
							// the first step has the Zone from 0 to 5 in the ProgressBar
							downloadPage.progressBar.setValue(5);
							downloadPage.progressBar.repaint();
							downloadPage.m_labelIconStep1.setIcon(downloadPage.stepfinished);
							return 0;
						}
					catch(Exception e)
						{
							e.printStackTrace();
							downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep1"));
							resetChanges();
							return -1;
						}
				}


		 //Step 2
		private class JapDownloadListener implements DownloadListener
			{
				private int m_retDownload=-1;
				private int aktPos=0;
				public int getDownloadState()
					{
						return m_retDownload;
					}
				public int progress(byte[] data,int lenData,int lenTotal,int state)
					{
						if(updateAborted)
							{
								synchronized(this)
									{
										this.notify();
									}
								m_retDownload=-1;
								return -1;
							}
						if(state == DownloadListener.STATE_IN_PROGRESS)
							{
								if(m_arBufferNewJapJar == null)
									{
										m_arBufferNewJapJar = new byte[lenTotal];
										aktPos=0;
									}
								//write data into the RAM
								System.arraycopy(data,0,m_arBufferNewJapJar,aktPos,lenData);
								aktPos += lenData;
								// the Download has the Zone from 5 to 455 in the ProgressBar
								int value = ((450 * aktPos)/lenTotal);
								downloadPage.progressBar.setValue((value+5));
								downloadPage.progressBar.repaint();
								return 0;
							}
						else if(state == DownloadListener.STATE_ABORTED)
							{
								//tell the user that the download aborted
								downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep2"));
								//m_labelIconStep2.setIcon(blank);
								synchronized(this)
									{
										this.notify();
									}
								//wizardCompleted();
								m_retDownload=-1;
								return -1;
							}
						else if(state == DownloadListener.STATE_FINISHED)
							{
								synchronized(this)
									{
										this.notify();
									}
								m_retDownload=0;
								return 0;
							}
						return 0;
					}
				}

		private int downloadUpdate()
			{
				InfoService infoService;
				URL jarUrl;
				infoService = JAPController.getInfoService();
					//JAPVersionInfo japVersionInfo = infoService.getJAPVersionInfo(type);
				URL codeBase=japVersionInfo.getCodeBase();
					// ErrorMessage connection with infoservice failed
				try
					{
						if(incrementalUpdate)
							jarUrl = new URL(codeBase,japVersionInfo.getJAPJarFileName()+"?version-id="+"00.01.037"+"&current-version-id="+"00.01.037");
						else
						 jarUrl = new URL(codeBase,japVersionInfo.getJAPJarFileName()+"?version-id="+japVersionInfo.getVersion());
					}
				catch(Exception e)
					{
						return -1;
					}
				downloadPage.m_labelIconStep2.setIcon(downloadPage.arrow);
				int retDownload=-1;
				int aktPos=0;
				try
					{
						JapDownloadListener japDownloadListener=new JapDownloadListener();
//						final Object oSync=new Object();
						synchronized(japDownloadListener)
							{
								infoService.retrieveURL(jarUrl,japDownloadListener);
							japDownloadListener.wait();
						if(japDownloadListener.getDownloadState()==-1)
							{
								resetChanges();
								return -1;
							}
						downloadPage.m_labelIconStep2.setIcon(downloadPage.stepfinished);
						return 0;
					}
				}
						catch(Exception e)
						{
							resetChanges();
								e.printStackTrace();
								return -1;
						}
			}


		//Step 3 needed by a full Update
		private int createNewJAPJar()
			{
				try
					{
						 m_fileNewJapJar = new File(m_strAktJapJarPath+m_strAktJapJarFileName+version+EXTENSION_NEW+m_strAktJapJarExtension);
						 FileOutputStream fos = new FileOutputStream(m_fileNewJapJar);
						 if(m_arBufferNewJapJar == null)
								{
									fos.close();
									return -1;
								}
							downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
							fos.write(m_arBufferNewJapJar);
							fos.flush();
							fos.close();
						 // the creatNewJapJar step has the Zone from 455 to 490 in the ProgressBar
							downloadPage.progressBar.setValue(490);
							downloadPage.progressBar.repaint();
							downloadPage.m_labelIconStep3.setIcon(downloadPage.stepfinished);
							return 0;
					}
				catch(Exception e)
					{
						 e.printStackTrace();
						 downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep3"));
						 resetChanges();
						 return -1;
					}
			}
/////////////////////////////////////////////////////////////////////////////////
			//Step 3' needed by a differential Update
		 private synchronized int applyJARDiffJAPJar()
		 {
				//get the buffer (bufferJapJar) where the data is stored
				//apply the JarDiff
				//create a new File "Jap_"+newversion+".jar"

				try
					{
						 //cp_updJapJar = new File(m_strAktJapJarPath+version+ext_new+m_strAktJapJarExtension);
						 // FileOutputStream fos = new FileOutputStream(cp_updJapJar);
				/*		 if(m_arBufferNewJapJar == null)
							{
								return -1;
							}

							 if(listener.progress(m_arBufferNewJapJar.length,m_arBufferNewJapJar.length,UpdateListener.STATE_IN_PROGRESS_STEP3)!=0)
							{
								return -1;
							}

						 if(JAPUtil.applyJarDiff(m_fileAktJapJar.getAbsolutePath(), cp_updJapJar.getAbsolutePath(),m_arBufferNewJapJar)!=0)
							{
								return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
							}


					*/	 return 0;//listener.progress(0,0,UpdateListener.STATE_FINISHED_STEP3);
					}
				catch(Exception e)
					{
						 e.printStackTrace();
						 return -1;//listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
					}

		 }
/////////////////////////////////////////////////////////////////////////////////
		 //Step 5 create the new JAP.jar-File by overwriting the oldFile by the new downloaded file
		 private int overwriteJapJar()
			{
				 try
					 {
						 downloadPage.m_labelIconStep5.setIcon(downloadPage.arrow);
						 FileInputStream fis = new FileInputStream(m_fileNewJapJar);
						 FileOutputStream fos = new FileOutputStream(m_fileAktJapJar);
						 byte buffer[]= new byte[2048];
						 int n;
						 while((n = fis.read(buffer))!=-1)
								{
									fos.write(buffer,0,n);
								}
								fis.close();
								fos.flush();
								fos.close();
								// the 5th step has the Zone from 490 to 500 in the ProgressBar
								downloadPage.progressBar.setValue(500);
								downloadPage.progressBar.repaint();
								downloadPage.m_labelIconStep5.setIcon(downloadPage.stepfinished);
								return 0;

		 }catch(Exception e)
						 {
								e.printStackTrace();
								downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep5"));
								return -1;
						 }

			}

		// method deletes all Files created new while the updating if there is an abort
		// by the system
		private void resetChanges()
			{
				if(m_fileJapJarCopy!=null)
					{
						m_fileJapJarCopy.delete();
					}
				if(m_fileNewJapJar!=null)
					{
						m_fileNewJapJar.delete();
					}
				if(updJapJar!=null)
					{
						updJapJar.delete();
					}
				host.getDialogParent().dispose();
			}
	}
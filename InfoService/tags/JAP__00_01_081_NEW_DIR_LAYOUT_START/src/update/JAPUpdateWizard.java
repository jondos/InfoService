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
import update.UpdateListener;

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

    //same as pathToJapJar
    public String selectedFile;
    private Vector m_Pages;


    private JAPUpdateWizard updateWizard;

    private String pathToJapJar;
    //fileName->> Name of the chosen File without extension and path i.e. 'Jap'
    //extension->> the extension of the chosen File ie. '.jar'
    // path ->> path to the chosen File without extension i.e. 'C:\Programme\Jap'
    private String fileName,extension,path;

    private final static String ext_backup = ".backup";
    private final static String ext_new = ".new";

    private boolean updateAborted = false;
    private boolean incrementalUpdate = false; // should be true by default
    //which version chose the user
    private String version;
    //which type dev or rel?
    private JAPVersionInfo japVersionInfo;

    //aktJapJar --> the original JAP.jar; cp_aktJapJar --> the copy of the original File extended by the current version-number
    //i.e. JAPaktVersion.jar; cp_updJapJar --> the downloaded Upgrade extended by the version-number
    //cp_updJapJar --> copy of the newJarFile without version-number named JAP.jar

    private File cp_aktJapJar, aktJapJar, cp_updJapJar, updJapJar;

    private int countPackages = 0;
    private int countBytes = 0;
    private int value = 0;
    private int totalLength = 0;
    private byte[] bufferJapJar;

    private Thread updateThread;
    private UpdateListener updateListener;

    public JAPUpdateWizard(String version, JAPVersionInfo info)
      {
        this.version = version;
        japVersionInfo = info;
        updateWizard = this;
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
           updateListener = new UpdateListener(){
            public int progress(int lenData, int lenTotal, int state)
                {// set the icons and the progressbar values
                //check Abbruchbedingung --> return -1
                //boolean updateAborted = false;
                if(updateAborted)
                {
                   resetChanges();
                   return -1;
                }
                   if(state == UpdateListener.STATE_IN_PROGRESS_STEP1)// first step is going on ...
                      {
                                 //downloadPage.m_labelIconStep1.setText("");

                                 downloadPage.m_labelIconStep1.setIcon(downloadPage.arrow);
                                 countPackages += lenData;
                                // the first step has the Zone from 0 to 5 in the ProgressBar
                                 value = (5 * countPackages)/lenTotal;

                                 downloadPage.progressBar.setValue(value);
                                 downloadPage.progressBar.repaint();
                                 return 0;
                      }else if(state == 2)
                      {
                                  downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep1"));
                                  resetChanges();
                                  return -1;
                      }else if(state == UpdateListener.STATE_FINISHED_STEP1)
                      {
                                  downloadPage.m_labelIconStep1.setIcon(downloadPage.stepfinished);
                                  //downloadPage.m_labelIconStep2.setIcon(downloadPage.arrow);
                                  return 0;
                      }else if(state == 4)//download is going on
                      {
                                  downloadPage.m_labelIconStep2.setIcon(downloadPage.arrow);
                                  return 0;
                      }else if(state == 5)//download aborted
                      {
                                  resetChanges();
                                  return -1;
                      }else if(state == 6)
                      {            downloadPage.m_labelIconStep2.setIcon(downloadPage.stepfinished);
                                  //downloadPage.m_labelIconStep2.setVisible(false);
                                  return 0;
                      }else if(state == 7)//createNewJapJar()
                      {
                                   downloadPage.m_labelIconStep2.setIcon(downloadPage.stepfinished);
                                   downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
                                   countPackages = 0;
                                   countPackages += lenData;
                                // the first step has the Zone from 455 to 490 in the ProgressBar
                                 value = (35 * countPackages)/lenTotal;
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
                                   downloadPage.m_labelIconStep4.setIcon(downloadPage.arrow);

                                   countPackages += lenData;
                                // the 5th step has the Zone from 490 to 500 in the ProgressBar
                                   value = (10 * countPackages)/lenTotal;
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

                updateThread = new Thread(this);
                updateThread.start();


        }

    public void run()
        {
         // Start with Step 1 copy
        if(renameJapJar(updateListener)!=0)
          {

            return;
          }
        // Step 2
         if(downloadUpdate(updateListener)!=0)
         {
            return;
         }
        //Step 3 or 3'
        if(incrementalUpdate)
        {
            if(applyJARDiffJAPJar(updateListener)!=0)
              {
                return;
              }
        }
        else
          {
            if( createNewJAPJar(updateListener)!=0)
              {
                return;
              }
           }
        // Step 5
        if( createJapJar(updateListener)!=0)
          {
            return;
          }
        try {
               // if(!aktJapJar.delete()) {downloadPage.showInformationDialog("Deleting original JAP.jar failed");}
               // if(!updJapJar.renameTo(new File(path+extension))) {downloadPage.showInformationDialog("Renaming JAP_temp.jar failed.");}
                if(!cp_updJapJar.delete())
                  {
                    downloadPage.showInformationDialog("Deleting of JAP_new.jar failed!");
                    resetChanges();
                    return;
                  }
            }
        catch(Exception e)
            {
                e.printStackTrace();
                downloadPage.showInformationDialog(e.toString());
                return;
            }

        }



    public void setSelectedFile (String selectedFile)
      {
        this.selectedFile = selectedFile;
      }

    public String getSelectedFile()
      {
        return selectedFile;
      }

    // called by the WizardHost when the User's clicked next
    public void startUpdate()
      {
        //start the Update
        setPath(selectedFile);
      }

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
            //host.setNextEnabled(true);
            setSelectedFile(welcomePage.getSelectedFile());
            parsePathToJapJar(selectedFile);
            setPath(selectedFile);
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
     public void setPath(String pathToJapJar)
      {

        this.pathToJapJar = pathToJapJar;
        downloadPage.m_labelStep1_1.setText(JAPMessages.getString("updateM_labelStep1Part1"));//+" "+pathToJapJar+" "+JAPMessages.getString("updateM_labelStep1Part2")+" "+pre+JAPConstants.aktVersion2+suf+JAPMessages.getString("updateM_labelStep1Part3"));
        downloadPage.tfSaveFrom.setText(pathToJapJar);
        downloadPage.m_labelStep1_2.setText(JAPMessages.getString("updateM_labelStep1Part2"));
        downloadPage.tfSaveTo.setText(path+JAPConstants.aktVersion2+ext_backup+extension);
        downloadPage.m_labelStep3.setText(JAPMessages.getString("updateM_labelStep3Part1")+" "+fileName+version+ext_new+extension+JAPMessages.getString("updateM_labelStep3Part2"));
        finishPage.tf_BackupOfJapJar.setText(path+JAPConstants.aktVersion2+ext_backup+extension);

      }

      private void parsePathToJapJar(String pathToJapJar)
      {
        //System.out.println(pathToJapJar+" path");
        try{
          int indexPoint = pathToJapJar.lastIndexOf(".");
          //System.out.println("  "+indexPoint+" index");
          path = pathToJapJar.substring(0,(indexPoint));
          extension = pathToJapJar.substring(indexPoint);
          int indexSlash = pathToJapJar.lastIndexOf("\\");
          fileName = pathToJapJar.substring(indexSlash+1,indexPoint);
          //System.out.println(fileName+" FILENAME");
         }catch(Exception e)
         {
           e.printStackTrace();
         }
      }
     //Step 1
      private int renameJapJar( UpdateListener listener)
     {

         byte[]buffer = new byte[2048];
        //just copy the File and then rename the copy
          try
          {
             //newFile = new File(prefix+JAPConstants.aktVersion2+suffix);

             cp_aktJapJar = new File(path+JAPConstants.aktVersion2+ext_backup+extension);
             aktJapJar = new File(path+extension);
             FileInputStream fis = new FileInputStream(aktJapJar);
             FileOutputStream fos = new FileOutputStream(cp_aktJapJar);
             int n;
             int totalLength = (int)aktJapJar.length();
             while ((n = fis.read(buffer))!=-1)
                {

                    //System.out.println(n+" written");
                    fos.write(buffer,0,n);
                    //if listener.progress != 0 return;
                  if(listener.progress(n, totalLength, UpdateListener.STATE_IN_PROGRESS_STEP1)!=0)
                    {
                      fis.close();
                      fos.close();
                      //cp_aktJapJar.delete();
                      //cp_aktJapJar = null;
                      return -1;
                    }
                }
             fis.close();
             fos.flush();
             fos.close();
             return listener.progress(0, totalLength, UpdateListener.STATE_FINISHED_STEP1);
          }catch(Exception e)
          {
          e.printStackTrace();
          cp_aktJapJar.delete();
          return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP1);

          }
      }


     //Step 2
    private synchronized int downloadUpdate(UpdateListener listener)
      {
        InfoService infoService;
        URL jarUrl;
        final UpdateListener l = listener;
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
          //jarUrl = new URL(japVersionInfo.getCodeBase());
          //jarUrl =new URL(jarUrl.getProtocol(),jarUrl.getHost(),jarUrl.getPort(),jarUrl.getFile()+japVersionInfo.getJAPJarFileName());
        listener.progress(0,0,UpdateListener.DOWNLOAD_START);
         // System.out.println("Download "+codeBase+japVersionInfo.getJAPJarFileName()+"?version-id="+version+"&current-version-id="+JAPConstants.aktVersion2);


             //   }
            try
            {
                final Object oSync=new Object();
                synchronized(oSync)
                  {
                infoService.retrieveURL(jarUrl,
                new DownloadListener()
                {
                    public int progress(byte[] data,int lenData,int lenTotal,int state)
                        {
                             if(updateAborted)
                              {
                                synchronized(oSync)
                                  {
                                    oSync.notify();
                                  }

                                  l.progress(0,0,UpdateListener.DOWNLOAD_ABORT);
                                  return -1;
                              }
                            if(state == 1)
                              {

                                 if(totalLength == 0)
                                    {
                                       totalLength = lenTotal;
                                       bufferJapJar = new byte[totalLength];
                                    }
                                 //write data into the RAM
                                 System.arraycopy(data,0,bufferJapJar,countBytes,lenData);
                                 countBytes += lenData;
                                 countPackages += lenData;
                                // the Download has the Zone from 5 to 455 in the ProgressBar
                                 value = ((450 * countBytes)/lenTotal);
                                 downloadPage.progressBar.setValue((value+5));
                                 downloadPage.progressBar.repaint();
                                 //if (abbruch){return -1}
                                 return 0;
                              }else if(state == 2)
                              {
                                  //tell the user that the download aborted
                                  downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep2"));
                                  //m_labelIconStep2.setIcon(blank);
                                  l.progress(0,0,UpdateListener.DOWNLOAD_ABORT);
                                  synchronized(oSync)
                                  {
                                      oSync.notify();
                                  }
                                  //wizardCompleted();
                                  return -1;
                              }else if(state == 3)
                              {

                                  synchronized(oSync)
                                  {
                                      oSync.notify();
                                  }
                                  l.progress(0,0,UpdateListener.DOWNLOAD_READY);
                                  return 0;
                              }

                            return 0;

                        }
                });
                oSync.wait();
                return 0;
              }
            }
            catch(Exception e)
            {
                listener.progress(0,0,UpdateListener.DOWNLOAD_ABORT);
                e.printStackTrace();
                return -1;
            }
            //doesn't work
            //listener.progress(0,0,UpdateListener.DOWNLOAD_READY);

      }


      //Step 3 needed by a full Update
     private synchronized int createNewJAPJar(UpdateListener listener)
     {

        try
          {
             cp_updJapJar = new File(path+version+ext_new+extension);
             FileOutputStream fos = new FileOutputStream(cp_updJapJar);
             if(bufferJapJar == null)
                {
                  fos.close();
                  return -1;
                }
             if(listener.progress(bufferJapJar.length,bufferJapJar.length,UpdateListener.STATE_IN_PROGRESS_STEP3)!=0)
                {
                    fos.close();
                    return -1;
                }
             fos.write(bufferJapJar);
             fos.flush();
             fos.close();
             return listener.progress(0,0,UpdateListener.STATE_FINISHED_STEP3);
          }
        catch(Exception e)
          {
             e.printStackTrace();
             return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
          }

     }
/////////////////////////////////////////////////////////////////////////////////
      //Step 3' needed by a differential Update
     private synchronized int applyJARDiffJAPJar(UpdateListener listener)
     {
        //get the buffer (bufferJapJar) where the data is stored
        //apply the JarDiff
        //create a new File "Jap_"+newversion+".jar"

        try
          {
             cp_updJapJar = new File(path+version+ext_new+extension);
             // FileOutputStream fos = new FileOutputStream(cp_updJapJar);
             if(bufferJapJar == null)
              {
                return -1;
              }

               if(listener.progress(bufferJapJar.length,bufferJapJar.length,UpdateListener.STATE_IN_PROGRESS_STEP3)!=0)
              {
                return -1;
              }

             if(JAPUtil.applyJarDiff(aktJapJar.getAbsolutePath(), cp_updJapJar.getAbsolutePath(),bufferJapJar)!=0)
              {
                return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
              }


             return listener.progress(0,0,UpdateListener.STATE_FINISHED_STEP3);
          }
        catch(Exception e)
          {
             e.printStackTrace();
             return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
          }

     }
/////////////////////////////////////////////////////////////////////////////////
     //Step 5 create the new JAP.jar-File by overwriting the oldFile by the new downloaded file
     private int createJapJar(UpdateListener listener)
      {
         try
             {
                updJapJar = new File(path+"_temp"+extension);
                //oldFile.delete();
                FileInputStream fis = new FileInputStream(cp_updJapJar);
                //FileOutputStream fos = new FileOutputStream(updJapJar);
                FileOutputStream fos = new FileOutputStream(aktJapJar);
                byte buffer[]= new byte[2048];
                int n;
                while((n = fis.read(buffer))!=-1)
                {
                  fos.write(buffer,0,n);
                  //System.out.println("createJapJar "+n);
                  if(listener.progress(n,totalLength,UpdateListener.STATE_IN_PROGRESS_STEP5)!=0)
                    {
                      System.out.println("createJapJar aborted"+n);
                      fis.close();
                      fos.close();
                      //updJapJar.delete();
                      //cp_updJapJar.delete();
                      //cp_aktJapJar.delete();
                      return -1;
                    }
                }
                fis.close();
                fos.flush();
                fos.close();
                System.out.println("createJapJar finished "+n);
                return listener.progress(0,0,UpdateListener.STATE_FINISHED_STEP5);
             }catch(Exception e)
             {
                e.printStackTrace();
                return listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP5);
             }

      }
      // method deletes all Files created new while the updating if there is an abort
      // by the system
      private void resetChanges()
      {

        if(cp_aktJapJar!=null)
        {
          cp_aktJapJar.delete();
        }
        if(cp_updJapJar!=null)
        {
          cp_updJapJar.delete();
        }
        if(updJapJar!=null)
        {
          updJapJar.delete();
        }
        host.getDialogParent().dispose();

     /*    try{
            updateThread.join();
            }
        catch(Exception e)
          {
            e.printStackTrace();
          }*/
      }

      public static void main(String[]args)
      {
         //JAPUpdateWizard juw = new JAPUpdateWizard(JAPConstants.aktVersion2,InfoService.JAP_RELEASE_VERSION);
      }

  }
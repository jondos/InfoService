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

import JAPConstants;

import JAPController;
public final class JAPUpdateWizard extends gui.wizard.BasicWizard implements Runnable
  {
    public JAPWelcomeWizardPage welcomePage;
    public JAPDownloadWizardPage downloadPage;

    //same as pathToJapJar
    public String selectedFile;
    private Vector m_Pages;


    private JAPUpdateWizard updateWizard;

    private String pathToJapJar;
    //needed for renaming in step1
    String pre, suf;

    boolean rename = false;
    boolean renameSuccess = false;
    //which version chose the user
    private String version;
    //which type dev or rel?
    private int type;

    private File jnlpFile, newFile, oldFile, newJarFile, jarFile;

    private int countPackages = 0;
    private int value =0;
    private byte[] bufferJapJar;
    private OutputStream os_jarFile;

    private Thread updateThread;
    private UpdateListener updateListener;

    public JAPUpdateWizard(String version, int type)
      {
        this.version = version;
        this.type = type;
        setWizardTitle("JAP Update Wizard");
        welcomePage = new JAPWelcomeWizardPage(this);
        downloadPage = new JAPDownloadWizardPage( version, type, this );
        addWizardPage(0,welcomePage);
        addWizardPage(1,downloadPage);
        m_Pages = getPageVector();
        BasicWizardHost host=new BasicWizardHost(JAPController.getView(),this);
        invokeWizard(host);
      }

    private void startUpdateThread()
        {
           updateListener = new UpdateListener(){
            public int progress(int lenData, int lenTotal, int state)
                {// set the icons and the progressbar values
                   if(state == 1)// first step is going on ...
                      {
                                 downloadPage.m_labelIconStep1.setIcon(downloadPage.arrow);
                                 countPackages += lenData;
                                // the first step has the Zone from 0 to 5 in the ProgressBar
                                 value = (5 * countPackages)/lenTotal;
                                 downloadPage.progressBar.setValue(value);
                                 downloadPage.progressBar.repaint();
                                 return 0;
                      }else if(state ==2)
                      {
                                  downloadPage.showInformationDialog("Sichern von Jap.jar schlug fehl");
                                  return -1;
                      }else if(state == 3)
                      {
                                  downloadPage.m_labelIconStep1.setIcon(null);
                                  return 0;
                      }else if(state == 4)//download is going on
                      {
                                  downloadPage.m_labelIconStep2.setIcon(downloadPage.arrow);
                                  return 0;
                      }else if(state == 5)//download aborted
                      {

                                  return -1;
                      }else if(state == 6)
                      {
                                  downloadPage.m_labelIconStep2.setIcon(null);
                                  return 0;
                      }else if(state == 7)//createNewJapJar()
                      {
                                  downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
                                   countPackages += lenData;
                                // the first step has the Zone from 455 to 490 in the ProgressBar
                                 value = (35 * countPackages)/lenTotal;
                                 downloadPage.progressBar.setValue(value);
                                 downloadPage.progressBar.repaint();
                                 return 0;
                      }else if(state == 8)//creating of new JARFile failed
                      {
                                  //set blank
                                  downloadPage.showInformationDialog("Erzeugen der neuen JAP.jar schlug fehl.");
                                  return -1;
                      }else if(state == 9)// finshed creation of the new JarFile
                      {
                                  downloadPage.m_labelIconStep3.setIcon(null);
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
                                // the first step has the Zone from 490 to 500 in the ProgressBar
                                 value = (10 * countPackages)/lenTotal;
                                 downloadPage.progressBar.setValue(value);
                                 downloadPage.progressBar.repaint();
                                 return 0;
                      }else if(state == 14)
                      {
                                 downloadPage.showInformationDialog("Schreiben der neuen Jap.Jar schlug fehl.");
                                 return -1;
                      }else if(state == 15)
                      {
                                  downloadPage.m_labelIconStep5.setIcon(null);
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
         renameJapJar(pre,suf,updateListener);
         downloadUpdate(updateListener);
        }

    public JAPWelcomeWizardPage getWelcomeWizardPage()
        {
            return welcomePage;
        }

    public void setSelectedFile (String selectedFile)
      {
        this.selectedFile = selectedFile;
        //downloadPage.setPath(selectedFile);
      }

    public String getSelectedFile()
      {
        return selectedFile;
      }

    // called by the WizardHost when the User's clicked next
    public void startUpdate()
      {
        //start the Update
       // downloadPage.initialize();
        setPath(selectedFile);
      }

     public WizardPage next(WizardPage currentPage, WizardHost host)
   {

      int pageIndex=m_Pages.indexOf(currentPage);
      pageIndex++;
      host.setBackEnabled(true);
      if(pageIndex==m_Pages.size()-1)
        {
          host.setFinishEnabled(true);
          host.setNextEnabled(false);

        }
      ///host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      //if it is the DownloadWizardPage
      if(pageIndex == 1)
        {
            startUpdate();
            host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
            //host.setNextEnabled(true);
            startUpdateThread();


        }else
        {
            host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
        }

      return null;
   }

//-----------------------------------------------------------------------------<<
     public void setPath(String pathToJapJar)
      {

        this.pathToJapJar = pathToJapJar;
      /*  if (pathToJapJar.length()>50)
        {
           int i = pathToJapJar.length();
           int j = pathToJapJar.length();
           int l = 0;
            do
            {
             i = i/50;
             l = j-50;
            pathToJapJar.substring(l,j)
            }while(i==0)
        }*/

        //String pre;
        //String suf;
        int i;
        i = pathToJapJar.lastIndexOf(".");
        suf = pathToJapJar.substring(i);
        pre = pathToJapJar.substring(0,i);
        downloadPage.m_labelStep1.setText("<html><b>1. Sichern von "+pathToJapJar+" nach <BR>"+pre+JAPConstants.aktVersion2+suf+"</b></html>");

        //renameJapJar(pre, suf);
       // downloadUpdate();
      }
     //Step 1
      public void renameJapJar(String prefix, String suffix, UpdateListener listener)
     {
        //downloadPage.m_labelIconStep1.setIcon(downloadPage.arrow);

         byte[]buffer = new byte[2048];
        //just copy the File and then rename the copy
          try
          {
             newFile = new File(prefix+JAPConstants.aktVersion2+suffix);
             oldFile = new File(pathToJapJar);
             FileInputStream fis = new FileInputStream(oldFile);
             FileOutputStream fos = new FileOutputStream(newFile);
             int n;
             int totalLength = (int)oldFile.length();
             while ((n = fis.read(buffer))!=-1)
                {
                    listener.progress(n, totalLength, UpdateListener.STATE_IN_PROGRESS_STEP1);
                    System.out.println(n+" written");
                    fos.write(buffer,0,n);
                }
             fis.close();
             fos.flush();
             fos.close();
             listener.progress(0, totalLength, UpdateListener.STATE_FINISHED_STEP1);
          }catch(Exception e)
          {
          listener.progress(0,1,UpdateListener.STATE_ABORTED_STEP1);
          e.printStackTrace();
          }
      }

     //Step 2
    public synchronized void downloadUpdate(UpdateListener listener)
      {
        InfoService infoService;
        URL jarUrl;


                      infoService = new InfoService("infoservice.inf.tu-dresden.de",6543);
                      JAPVersionInfo japVersionInfo = infoService.getJAPVersionInfo(type);
                      jarUrl = japVersionInfo.getJarUrl();
                      listener.progress(0,0,UpdateListener.DOWNLOAD_START);
                      //os_jarFile = new OutputStream();
                     // m_labelIconStep2.setIcon(arrow);
             //   }
            try
            {
                infoService.retrieveURL(new URL("http","fg",3,"aktJap"),// jarUrl,
                new DownloadListener()
                {
                    public int progress(byte[] data,int lenData,int lenTotal,int state)
                        {

                            if(state == 1)
                              {
                                 //write data into the RAM
                               /*  try
                                 {
                                   //ByteArrayInputStream bais = new ByteArrayInputStream(data,0,lenData);
                                  //write(data)
                                  // ByteArrayOutputStream baos = new ByteArrayInputStream(

                                 }
                                 catch(IOException ioe)
                                 {ioe.printStackTrace();}*/
                                 countPackages += lenData;
                                // the Download has the Zone from 5 to 455 in the ProgressBar
                                 value = (450 * countPackages)/lenTotal;
                                 downloadPage.progressBar.setValue(value);
                                 downloadPage.progressBar.repaint();
                              }else if(state == 2)
                              {
                                  //tell the user that the download aborted
                                  downloadPage.showInformationDialog("Fehler beim Download des Updates.");
                                  //m_labelIconStep2.setIcon(blank);
                                  return -1;
                              }else if(state == 3)
                              {
                                 // m_labelIconStep2.setIcon(blank);
                                  return 0;
                              }
                            //System.out.println(value+" value"+countPackages+" Countpak" +lenTotal+ " Total");
                            return 0;
                        }
                });

            }
            catch(Exception e)
            {
                listener.progress(0,0,UpdateListener.DOWNLOAD_ABORT);
                e.printStackTrace();
            }
            listener.progress(0,0,UpdateListener.DOWNLOAD_READY);
            //m_labelIconStep2.setIcon(blank);
            //System.out.println("Hallo");
      }


      //Step 3
     public void createNewJAPJar(UpdateListener listener)
     {
        //get the buffer (bufferJapJar) where the data is stored
        //apply the JarDiff
        //create a new File "Jap_"+newversion+".jar"
        int index = pathToJapJar.lastIndexOf("\\");
        String path = pathToJapJar.substring(0,index);
        path = path+"Jap_"+version+".jar";
        try
          {
             newJarFile = new File(path);
             FileOutputStream fos = new FileOutputStream(newJarFile);
             fos.write(bufferJapJar);
             fos.flush();
             fos.close();
          }
        catch(Exception e)
          {
             e.printStackTrace();
          }

     }

     //Step 5
     public void createJapJar()
      {
         try
             {
                oldFile.delete();
                FileInputStream fis = new FileInputStream(newJarFile);
                FileOutputStream fos = new FileOutputStream(jarFile);
                int n;
                while((n=fis.read())!=-1)
                {
                  fos.write(n);
                }
                fis.close();
                fos.flush();
                fos.close();
             }catch(Exception e)
             {
                e.printStackTrace();
             }

      }
//----------------------------------------------------------------------------->>
  }
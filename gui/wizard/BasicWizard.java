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
package gui.wizard;

import java.util.Vector;

// this class shall provide the basics for the wizard
//as flow control and so on ...
public class BasicWizard implements Wizard
  {
    //private Dialog helpDialog;
    private WizardPage currentWizardPage;
    private BasicWizardHost wizardHost;
    //private JFrame browser;
    private Vector m_Pages;
    private String m_strTitle;
    private int indexOfWizardPage;
 //   final String[] german ={"Hilfe","Abbrechen","Zurück","Weiter","Fertig"};
  //  final String[] english ={"Help","Cancel","Back","Next","Finish"};

//private JAPController japController;

public BasicWizard()
  {
    m_Pages=new Vector();
    indexOfWizardPage = 0;
  }

// todo -- what does appear if the user's clicked help
public void help(WizardPage wtp, WizardHost wh)
{
 // return helpDialog;

}

// determine number and order of the wizardpages
public WizardPage invokeWizard(WizardHost host)
 {
    host.setBackEnabled(false);
    host.setFinishEnabled(false);
    host.setWizardPage((WizardPage)m_Pages.elementAt(0));
    return null;
 }
/*
 //user's clicked back
 public void getLastWizardPage()
 {
 if(indexOfWizardPage == 0)
 {
 wizardHost.setBackEnabled(false);
 }else
 {

 currentWizardPage = wtpArray[indexOfWizardPage-1];
 indexOfWizardPage--;
 if(indexOfWizardPage == 0){wizardHost.back.setEnabled(false);}
 wizardHost.next.setEnabled(true);

 wizardHost.setNextWizardPage(currentWizardPage,indexOfWizardPage);
 }
 }
// user's clicked Next
 public void getNextWizardPage()
 {
 try{
 indexOfWizardPage++ ;
 currentWizardPage =  next(currentWizardPage, wizardHost);
 //System.out.println(currentWizardPage.toString());

 if(indexOfWizardPage == (totalSteps-1))
 {
 wizardHost.next.setEnabled(false);
 }
 //System.out.println("index "+indexOfWizardPage );
 if (currentWizardPage == null)
    {
    System.out.println("currentWizardPage ist null -- getNextWizardPage()");
    }
    }catch(Exception ve)
    {
    ve.printStackTrace();
    }
 wizardHost.setNextWizardPage(currentWizardPage,indexOfWizardPage);
// System.out.println(currentWizardPage.toString()+" getNextWizardPage() wizbase");

/// wizardHost.setButtonTexts(german,"de");
 }

 public void setWizardTitle(String title)
 {

 }
*/
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
      host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      return null;
   }

 public WizardPage back(WizardPage currentPage, WizardHost host)
   {
      int pageIndex=m_Pages.indexOf(currentPage);
      pageIndex--;
      host.setNextEnabled(true);
      host.setFinishEnabled(false);
      if(pageIndex==0)
        host.setBackEnabled(false);
      host.setWizardPage((WizardPage)m_Pages.elementAt(pageIndex));
      return null;
   }

 public void addWizardPage(int index,WizardPage wizardPage)
  {
    m_Pages.insertElementAt(wizardPage,index);
  }


  public int initTotalSteps()
    {
      return m_Pages.size();
    }

 public WizardPage finish(WizardPage currentPage, WizardHost host){return null;}


  public void wizardCompleted()
    {

    }

/* public JAPWizardBase getWizardBase()
 {
 return this;
 }
*/
  public void setWizardTitle(String title)
    {
      m_strTitle=title;
    }

  public String getWizardTitle()
    {
      return m_strTitle;
    }
}


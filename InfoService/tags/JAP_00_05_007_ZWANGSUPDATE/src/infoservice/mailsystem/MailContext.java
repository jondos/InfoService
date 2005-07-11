/*
 Copyright (c) 2000 - 2004 The JAP-Team
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
package infoservice.mailsystem;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.mail.Session;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;

/**
 * This class stores the configuration for the JAP mailsystem (Singleton).
 */
public class MailContext {

  /**
   * This stores the instance of MailContext (Singleton).
   */
  private static MailContext ms_mcInstance = null;


  /**
   * This stores the Session instance of the Java Mail API. All mails should be sended using this
   * session instance, because the configuration for the Java Mail API is only stored here.
   */
  private Session m_mailSession;


  /**
   * Creates a new instance of MailContext. We load the configuration from the specified file and
   * configure the Java Mail API and the InfoServiceDatabase. If we cannot read from the config-
   * file, only default values are used (defaults of the Java Mail API with SMTP protocol and
   * empty InfoServiceDatabase, so it is not very useful).
   *
   * @param a_configFile The path and the filename of the configuration file.
   */
  private MailContext(String a_configFile) {
    Properties mailConfig = new Properties();
    /* set some default values, they can be overwritten by the properties loaded from the config
     * file
     */
    mailConfig.put("mail.stmp.sendpartial", "true");
    mailConfig.put("mail.transport.protocol", "smtp");
    try {
      mailConfig.load(new FileInputStream(a_configFile));
    }
    catch (Exception e) {
      /* unable to load the configuration -> we work with the default values of the Java Mail API
       * and without infoservices
       */
    }
    m_mailSession = Session.getInstance(mailConfig);

    /* try to read the infoservices to use */
    String infoServiceList = mailConfig.getProperty("MailSystemInfoServiceList");
    if (infoServiceList != null) {
      infoServiceList = infoServiceList.trim();
      /* we have a list of infoservices */
      StringTokenizer stInfoServiceList = new StringTokenizer(infoServiceList, ",");
      while (stInfoServiceList.hasMoreTokens()) {
        StringTokenizer stCurrentInfoService = new StringTokenizer(stInfoServiceList.nextToken(), ":");
        Database.getInstance(InfoServiceDBEntry.class).update(new InfoServiceDBEntry(null,
          new ListenerInterface(stCurrentInfoService.nextToken().trim(),
                      Integer.parseInt(stCurrentInfoService.nextToken().trim())
                      ).toVector(), false, true));
        /* we need all entries only for a short time (for creating one single response mail), so
         * there is no need to update the infoservice database
         */
      }
    }
    InfoServiceHolder.getInstance().setChangeInfoServices(true);
  }


  /**
   * Creates an instance of MailContext, if there is already one, the old one is overwritten
   * (Singleton).
   *
   * @param a_configFile The path and the filename of the configuration file to use for the
   *                     new instance.
   */
  public static void createInstance(String a_configFile) {
    ms_mcInstance = new MailContext(a_configFile);
  }

  /**
   * Returns the instance of MailContext (Singleton). If there was no one created until yet, null
   * is returned.
   *
   * @return The instance of MailContext.
   */
  public static MailContext getInstance() {
    return ms_mcInstance;
  }


  /**
   * Returns the initialized Session instance for the Java Mail API.
   *
   * @return The Session instance to use with the Java Mail API.
   */
  public Session getSession() {
    return m_mailSession;
  }

}



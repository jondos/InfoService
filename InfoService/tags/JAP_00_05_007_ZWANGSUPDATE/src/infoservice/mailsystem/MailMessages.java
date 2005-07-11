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
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
/**
 * This class is the implementation of the mail message localizations (Singleton).
 */
public class MailMessages {
  /**
   * This is the base of the resource files. The single single resource files append this base
   * .properties (default resource, if needed) or _??.properties, where ?? is the language code,
   * e.g. 'de' (localized resources).
   */
  private static final String RESOURCE_BASE = "mailsystem/messages/MailMessages";
  /**
   * This is the default localization (English as default).
   */
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  /**
   * This stores the instance of MailMessages (Singleton).
   */
  private static MailMessages ms_mmInstance;
  /**
   * This stores the resource for the currently used localization.
   */
  private ResourceBundle m_messageResource;
  /**
   * Returns the instance of MailMessages (Singleton). If there is no running instance, a new one
   * is created.
   *
   * @return The MailMessages instance.
   */
  public static MailMessages getInstance() {
    if (ms_mmInstance == null) {
      ms_mmInstance = new MailMessages();
    }
    return ms_mmInstance;
  }
  /**
   * Returns the localized version of the message assigned to the specified key in the resource
   * files. The current localization is obtained from the running instance of MailMessages
   * (Singleton).
   *
   * @param a_key A key, which specifies the wanted message.
   *
   * @return The localized version of the specified message. If we can't find a localized version
   *         of the message, the default localization is returned. If that also fails, the key
   *         itself is returned.
   */
  public static String getString(String a_key) {
    return getInstance().getLocaleString(a_key);
  }
  /**
   * Creates a new MailMessages instance. Here is only done some initialization.
   */
  private MailMessages() {
    setLocale(DEFAULT_LOCALE);
  }
  /**
   * Changes the localization, which is used when getString() is called.
   *
   * @param a_newLocale The new localization. If there is no resource for that localization, the
   *                    default one is used.
   */
  public void setLocale(Locale a_newLocale) {
    synchronized (this) {
      try {
        m_messageResource = PropertyResourceBundle.getBundle(RESOURCE_BASE, a_newLocale);
      }
      catch (Exception e) {
        /* the resources for the specified locale could not be found, try to get it for the default
         * locale
         */
        try {
          m_messageResource = PropertyResourceBundle.getBundle(RESOURCE_BASE, DEFAULT_LOCALE);
        }
        catch (Exception e2) {
          /* also the default locale isn't available, we can't do anything */
          m_messageResource = null;
        }
      }
    }
  }
  /**
   * Returns the localized version of the message assigned to the specified key in the resource
   * files.
   *
   * @param a_key A key, which specifies the wanted message.
   *
   * @return The localized version of the specified message. If we can't find a localized version
   *         of the message, the default localization is returned. If that also fails, the key
   *         itself is returned.
   */
  private String getLocaleString(String a_key) {
    String localeString = a_key;
    synchronized (this) {
      try {
        localeString = m_messageResource.getString(a_key);
      }
      catch (Exception e) {
        /* if there was an exception, the default value (the key itself) is returned */
      }
    }
    return localeString;
  }
}
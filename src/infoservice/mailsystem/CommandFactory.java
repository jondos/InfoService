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
import java.util.StringTokenizer;

import infoservice.mailsystem.commands.GetForwarderCommand;
import infoservice.mailsystem.commands.HelpCommand;

/**
 * This class finds the correct implementation of the reply-generating class depending on the
 * subject of the received mail.
 */
public class CommandFactory {
    
  /**
   * The default language to use, if no language code to use could be found in the subject
   * line of the received mail. The default language is English.
   */
  public static final Locale DEFAULT_LANGUAGE = Locale.ENGLISH;
  
  /**
   * The command code of the GetHelp command.
   */
  public static final int COMMAND_GETHELP = 0;
  
  /**
   * The command code of the GetForwarder command.
   */
  public static final int COMMAND_GETFORWARDER = 1;
    
  /**
   * The default command to use, if we could not find a matching command in the subject
   * line of the received mail. The default is the GetHelp command.
   */
  public static final int DEFAULT_COMMAND = COMMAND_GETHELP;
  
  
  /**
   * This method finds the matching command implementation and reply language depending on
   * the subject line of the received mail.
   *
   * @param a_mailSubject The subject line of a received mail.
   */
  public static MailSystemCommand getCommandImplementation(String a_mailSubject) {
    int commandCode = DEFAULT_COMMAND;
    Locale languageToUse = DEFAULT_LANGUAGE;
    StringTokenizer stMailCommand = new StringTokenizer(a_mailSubject, " ");
    try {
      String command = stMailCommand.nextToken().trim();
      if (command.equalsIgnoreCase("GetHelp")) {
        commandCode = COMMAND_GETHELP;
      }
      if (command.equalsIgnoreCase("GetForwarder")) {
        commandCode = COMMAND_GETFORWARDER;
      }
      String language = stMailCommand.nextToken().trim();
      if (language.equalsIgnoreCase("en")) {
        languageToUse = Locale.ENGLISH;
      }
      if (language.equalsIgnoreCase("de")) {
        languageToUse = Locale.GERMAN;
      }
    }
    catch (Exception e) {
      /* maybe no command or no language was specified -> use the default values */
    }
    MailMessages.getInstance().setLocale(languageToUse);
    /* the default is the help command */
    MailSystemCommand commandImplementation = new HelpCommand();
    if (commandCode == COMMAND_GETFORWARDER) {
      commandImplementation = new GetForwarderCommand();
    }
    return commandImplementation;
  }
  
}
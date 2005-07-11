/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package forward.client.captcha;

import java.awt.Image;

import anon.infoservice.ListenerInterface;

/**
 * This defines some methods, every image based captcha for JAP has to implement.
 */
public interface IImageEncodedCaptcha {
  
  /**
   * Returns the image with the captcha data.
   */
  public Image getImage();
  
  /**
   * Returns the character set which was used when the captcha was created. Only characters from
   * this set are in the captcha image.
   */
  public String getCharacterSet();
  
  /**
   * Returns the number of characters which are included in the captcha.
   */
  public int getCharacterNumber();
  
  /**
   * Solves the captcha and returns the included connection information for a forwarder as a
   * ListenerInterface. The key is the character string visible in the captcha image. If the
   * wrong key is specified, an Exception is thrown.
   *
   * @param a_key The key for solving the captcha (it's the string which is shown in the captcha
   *              image).
   *
   * @return The ListenerInterface with the connection information to a forwarder.
   */
  public ListenerInterface solveCaptcha(String a_key) throws Exception;
  
}
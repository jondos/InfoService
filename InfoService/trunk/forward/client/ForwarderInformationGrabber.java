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
package forward.client;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.InfoServiceHolder;
import forward.client.captcha.IImageEncodedCaptcha;
import forward.client.captcha.ZipBinaryImageCaptchaClient;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class ForwarderInformationGrabber {
   
  public static final int RETURN_SUCCESS = 0;
  
  public static final int RETURN_INFOSERVICE_ERROR = 1;
  
  public static final int RETURN_UNKNOWN_ERROR = 2; 
  
  public static final int RETURN_NO_CAPTCHA_IMPLEMENTATION = 3;
   
  private int m_errorCode; 
  
  private IImageEncodedCaptcha m_captcha;
   
  public ForwarderInformationGrabber() {
    m_captcha = null;
    Element japForwarderNode = InfoServiceHolder.getInstance().getForwarder();
    if (japForwarderNode != null) {
      /* get the CaptchaEncoded node */
      NodeList captchaEncodedNodes = japForwarderNode.getElementsByTagName("CaptchaEncoded");
      if (captchaEncodedNodes.getLength() > 0) {
        Element captchaEncodedNode = (Element)(captchaEncodedNodes.item(0));
        m_errorCode = findCaptchaImplementation(captchaEncodedNode);
      }
      else {
        /* no CaptchaEncoded node -> the infoservice only returns valid forwarder nodes ->
         * return unknown error
         */
        m_errorCode = RETURN_UNKNOWN_ERROR;
      }
    }
    else {
      /* we could not get a forwarder entry from the infoservice network -> we can't reach any
       * infoservice or no infoservice knows a forwarder
       */
      m_errorCode = RETURN_INFOSERVICE_ERROR;
    }     
  }
  
  public ForwarderInformationGrabber(String a_xmlData) {
    m_captcha = null;
    try {
      /* parse the user input */
      ByteArrayInputStream in = new ByteArrayInputStream(a_xmlData.getBytes());
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
      NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
      if (japForwarderNodes.getLength() > 0) {
        Element japForwarderNode = (Element) (japForwarderNodes.item(0));
        NodeList captchaEncodedNodes = japForwarderNode.getElementsByTagName("CaptchaEncoded");
        if (captchaEncodedNodes.getLength() > 0) {
          Element captchaEncodedNode = (Element) (captchaEncodedNodes.item(0));
          m_errorCode = findCaptchaImplementation(captchaEncodedNode);
        }
        else {
          /* invalid input, the XML information pasted in by the user should always be valid */
          m_errorCode = RETURN_UNKNOWN_ERROR;
        }
      }
      else {
        /* invalid input, the XML information pasted in by the user should always be valid */
        m_errorCode = RETURN_UNKNOWN_ERROR;
      }
    }
    catch (Exception e) {
      /* invalid input, the XML information pasted in by the user should always be valid */
      m_errorCode = RETURN_UNKNOWN_ERROR;
    } 
  }
  
  public int getErrorCode() {
    return m_errorCode;
  }
  
  public IImageEncodedCaptcha getCaptcha() {
    return m_captcha;
  }

  private int findCaptchaImplementation(Element a_captchaEncodedNode) {
    int returnCode = RETURN_UNKNOWN_ERROR;
    /* read the captcha format */
    NodeList captchaDataFormatNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaDataFormat");
    if (captchaDataFormatNodes.getLength() > 0) {
      Element captchaDataFormatNode = (Element) (captchaDataFormatNodes.item(0));
      if (ZipBinaryImageCaptchaClient.CAPTCHA_DATA_FORMAT.equals(captchaDataFormatNode.getFirstChild().getNodeValue())) {
        /* the captcha has the ZIP_BINARY_IMAGE format */
        try {
          m_captcha = new ZipBinaryImageCaptchaClient(a_captchaEncodedNode);
          returnCode = RETURN_SUCCESS;
        }
        catch (Exception e) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "ForwarderInformationGrabber: findCaptchaImplementation: Error while creating the captcha implementation: " + e.toString());
          returnCode = RETURN_UNKNOWN_ERROR;
        }
      }
      else {
        /* we don't know other implementations yet */
        returnCode = RETURN_NO_CAPTCHA_IMPLEMENTATION;
      }
    }
    else {
      /* no CaptchaDataFormat node */
      returnCode = RETURN_UNKNOWN_ERROR; 
    }     
    return returnCode;
  }
  
}   
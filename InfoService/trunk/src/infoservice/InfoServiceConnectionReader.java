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

import java.io.InputStream;


/**
 * This is the implementation for reading data from an InputStream with a limitof bytes maximally
 * read.
 */
public class InfoServiceConnectionReader {
  
  /**
   * This stores the InputStream where the data is read from.
   */
  private InputStream m_inputStream;

  /**
   * This stores the currently number of bytes which can be read from the InputStream until the
   * data limit is exhausted.
   */
  private int m_byteLimit;
  
  
  /**
   * Constructs a new InfoServiceConnectionReader with a read-limitation for an InputStream.
   *
   * @param a_inputStream The InputStream where the data shall be read from.
   * @param a_byteLimit The maximum number of bytes allowed to read from the stream.
   */
  public InfoServiceConnectionReader(InputStream a_inputStream, int a_byteLimit) {
    m_inputStream = a_inputStream;
    m_byteLimit = a_byteLimit;
  }
  
  
  /**
   * Reads one byte from the underlying InputStream. If the call is successful, the limit of bytes
   * able to read from the stream is also decremented by 1. If the end of the stream is reached or
   * there was an exception while reading from the stream, the byte limit is not decremented. In
   * the case of a read exception, this exception is thrown. If the byte limit is exhausted, also
   * an exception is thrown.
   *
   * @return The byte read from the stream or -1, if the end of the stream was reached.
   */
  public int read() throws Exception {
    boolean limitReached = false;
    synchronized (this) {
      if (m_byteLimit < 1) {
        limitReached = true;
      }
      else {
        m_byteLimit--;
      }
    }
    if (limitReached == true) {
      throw (new Exception("Cannot read more bytes, message size limit reached."));
    }
    int returnByte = -1;
    try {
      returnByte = m_inputStream.read();
    }
    catch (Exception e) {
      synchronized (this) {
        /* nothing was read -> re-increase the counter */
        m_byteLimit++;
      }
      throw (e);
    }  
    if (returnByte == -1) {
      synchronized (this) {
        /* nothing was read -> re-increase the counter */
        m_byteLimit++;
      }
    }
    return returnByte;   
  }

}   
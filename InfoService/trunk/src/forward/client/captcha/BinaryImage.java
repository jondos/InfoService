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

import java.awt.Button;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

/**
 * This is a helper class for handling binary images. We need support for AWT components here
 * because Java 1.1 doesn't support images without AWT components. So this class cannot be used
 * in a text-console. If you need console support, look in the InfoService source tree, there
 * you can find an implementation, which can also run in a console, but needs a newer Java
 * version than 1.1 (we need support for java.awt.image.BufferedImage there). This implementation
 * of BinaryImage contains no support for writing the binary image format.
 */
public class BinaryImage {
  
  /**
   * Transfroms the data of our own binary image format back to a Image. The returned Image uses
   * the default color model. The black pixels are shown in a more eye-friendly dark gray.
   *
   * @param a_binaryData A image in our own binary image format.
   *
   * @return A Image of the input data or null, if the input data are not in the binary image
   *         format.
   */
  public static Image binaryToImage(byte[] a_binaryData) {
    Image resultImage = null;
    try {
      ByteArrayInputStream binaryData = new ByteArrayInputStream(a_binaryData);
      DataInputStream transformer = new DataInputStream(binaryData);
      /* get the size information */
      int width = transformer.readInt();
      int height = transformer.readInt();
      if ((width < 0) || (height < 0) || (a_binaryData.length != 8 + ((height * width) + 7) / 8)) {
        throw (new Exception("BinaryImage: binaryToImage: The binary image has an invalid size."));
      }
      /* create the pixel array */
      int[] pixels = new int[width * height];
      
      /* read the binary image map */
      int currentByte = binaryData.read();
      for (int i = 0; i < width * height; i++) {
        if ((currentByte & 0x80) == 0x80) {
          /* black pixel in the image, make it dark gray */
          pixels[i] = Color.darkGray.getRGB();
        }
        else {
          /* white pixel in the image */
          pixels[i] = Color.white.getRGB();
        } 
        /* get the next pixel in the 0x80 position */
        currentByte = currentByte << 1;
        if ((i % 8) == 7) {
          /* read the next byte with information about the next 8 pixels */
          currentByte = binaryData.read();
          /* attention currentByte can be -1, if width * height % 8 == 0 and we are at the end
           * of the image
           */
        }
      }
      /* create the image via a awt component -> simply take a button */
      resultImage = (new Button()).createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }
    catch (Exception e) {
      /* anything was wrong with the binary data */
      resultImage = null;
    }
    /* that's it */
    return resultImage;
  }

  /**
   * Compresses the specified data with the ZLIB in the best compression mode.
   *
   * @param a_data The data to compress.
   *
   * @return The compressed data or null, if there was an error while the compression.
   */
  public static byte[] zipBinaryData(byte[] a_data) {
    byte[] resultData = null;
    try {
      Deflater zipper = new Deflater(Deflater.BEST_COMPRESSION);
      ByteArrayOutputStream zippedData = new ByteArrayOutputStream();
      DeflaterOutputStream zipStream = new DeflaterOutputStream(zippedData, zipper);
      zipStream.write(a_data, 0, a_data.length);
      zipStream.finish();
      resultData = zippedData.toByteArray();
    }
    catch (Exception e) {
      /* should not happen */
    }
    return resultData;
  }

  /**
   * Decompresses the specified data.
   *
   * @param a_data The ZLIB compressed data.
   *
   * @return The uncompressed data or null, if the specified data are not ZLIB compressed.
   */
  public static byte[] unzipBinaryData(byte[] a_data) {
    byte[] resultData = null;
    try {
      ByteArrayOutputStream unzippedData = new ByteArrayOutputStream();
      Inflater unzipper = new Inflater();
      unzipper.setInput(a_data);
      byte[] currentByte = new byte[1];
      while (unzipper.inflate(currentByte) != 0) {
        unzippedData.write(currentByte);
      }
      unzippedData.flush();
      resultData = unzippedData.toByteArray();
    }
    catch (Exception e) {
      /* something was wrong with the compressed data */
    }
    return resultData;
  }
    
}    
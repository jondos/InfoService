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
package anon.infoservice;

import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JAPVersionInfo
  {
    private int m_Type;
    private String m_Version;
    private String m_Date="";
    private URL m_JarUrl;
    private String m_Description="";

    protected JAPVersionInfo(byte[] xmlJnlp,int type) throws Exception
      {
        m_Type=type;
        parse(xmlJnlp);
      }

    private void parse(byte[] xml) throws Exception
      {
        try
          {
            Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml));
            Element root=doc.getDocumentElement();
            m_Version=root.getAttribute("version"); //the JAP version
            String strCodeBase=root.getAttribute("codebase");
            NodeList nlResources=root.getElementsByTagName("resources");
            NodeList nlJars=((Element)nlResources.item(0)).getElementsByTagName("jar");
            for(int i=0;i<nlJars.getLength();i++)
              {
                try
                  {
                    Element elemJar=(Element)nlJars.item(i);
                    String part=elemJar.getAttribute("part");
                    if(part.equals("jap"))
                      m_JarUrl=new URL(elemJar.getAttribute("href"));
                  }
                catch(Exception e)
                  {
                  }
              }
          }
        catch(Exception ex)
          {
            throw ex;
          }
      }

      public String getVersion()
        {
          return m_Version;
        }

      public String getDate()
        {
          return m_Date;
        }
      public String getDescription()
      {
         return m_Description;
      }

     /* public static String getCodeBase()
      {
        return m_codeBase;
      }*/

      public URL getJarUrl()
      {
        return m_JarUrl;
      }
  }

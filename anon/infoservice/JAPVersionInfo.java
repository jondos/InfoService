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

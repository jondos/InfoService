package update;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0 */

import gui.*;
import JAPInfoService;
import JAPModel;

import HTTPClient.*;
import java.io.IOException;
import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class JAPUpdateConnection
{

  private String releasedVersion, developmentVersion, jarPath;
  private static String codeBase;
  private String attrInformation = "";
  private File jnlpReleaseFile, jnlpDevelopmentFile;
  private byte[] data;
  private Document doc;
  private JAPModel japModel;

  public JAPUpdateConnection(JAPModel japModel)
  {
     this.japModel = japModel;
  }

  public void connect(String typeLate)
  {
   if(typeLate.equals((Object)"Released"))
   {
      typeLate = "Release";
   }
   data = new byte[1024];
   String jnlpRelease = "";

  try
    {
	///HTTPConnection con = new HTTPConnection("infoservice.inf.tu-dresden.de");
	HTTPResponse   rsp;// = con.Get("/jap"+typeLate+".jnlp");
        rsp = japModel.getInfoService().getConInfoService().Get("/jap"+typeLate+".jnlp");
	if (rsp.getStatusCode() >= 300)
	{
	    System.err.println("Received Error: "+rsp.getReasonLine());
	    System.err.println(rsp.getText());
	}
	else{

	    data = rsp.getData();
             jnlpRelease = new String(data);
             System.out.println(jnlpRelease);
             parseFile(typeLate, data);
             }

    }catch (ParseException pe)
    {
	System.err.println("Error parsing Content-Type: " + pe.toString());
    }
    catch (ModuleException me)
    {
	System.err.println("Error handling request: " + me.getMessage());
    }
    catch (IOException ioe)
    {
	System.err.println(ioe.toString());
    }
  }

  public void parseFile(String typeLate, byte[] data)
  {
     try{
       jnlpReleaseFile = new File("jap"+typeLate+".jnlp");
       FileOutputStream fos = new FileOutputStream(jnlpReleaseFile);
       fos.write(data);
       fos.flush();
       fos.close();
      try{
      doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(jnlpReleaseFile);
      Node node = doc.getChildNodes().item(0);

     System.out.println( node.getNodeName());
      NamedNodeMap nodemap = node.getAttributes();
      node = nodemap.getNamedItem("version");
      System.out.println( node.getNodeName());
      if(typeLate.equals((Object)"Development"))
          {
         developmentVersion = node.getNodeValue();
          }
      else
          {
          releasedVersion = node.getNodeValue();
          }
       node = nodemap.getNamedItem("codebase");
       codeBase = node.getNodeValue();
           NodeList nl = doc.getChildNodes();
           node = nl.item(0);
         //  nodemap = doc.getAttributes();

           nl = node.getChildNodes();
           String nodeValue[] = new String[nl.getLength()];
           for(int i=0; i<nl.getLength();i++)
             {
           node = nl.item(i);


           nodeValue[i] = node.getNodeValue();

         if(node.hasChildNodes()) {
               //   if(nodeValue[i].equals((Object)"information"))
               //   {
                     NodeList nlInformation = node.getChildNodes();
                      for(int j=0; j< nlInformation.getLength();j++)
                         {
                              Node nodeInformation = nlInformation.item(j);
                              System.out.println( nodeInformation.getNodeName()+" information");



                                   if(nodeInformation.getNodeName().equals("description"))
                                       {
                                                    System.out.println( nodeInformation.getNodeName()+" equals description");
                                                    if(nodeInformation.hasChildNodes()&&(!nodeInformation.hasAttributes()))
                                                        {
                                                    //attrInformation = nodeInformation.getAttributes();
                                                            //attrInformation = nodeInformation.getFirstChild().getNodeValue();
                                                            attrInformation = attrInformation.concat(nodeInformation.getFirstChild().getNodeValue()+"\n");
                                                            System.out.println(nodeInformation.getLocalName()+" has" +attrInformation);

                                                        }
                                       }
                                       else if(nodeInformation.getNodeName().equals("resources"))
                                       {
                                          if(nodeInformation.hasChildNodes()&&(!nodeInformation.hasAttributes()))
                                                        {
                                                           NodeList nlResources = nodeInformation.getChildNodes();
                                                                  for(int k=0; k < nlResources.getLength(); k++)
                                                                      {
                                                                          Node nodeResources = nlResources.item(k);
                                                                                      if (nodeResources.getNodeName().equals("jar"))
                                                                                           {
                                                                                              NamedNodeMap nodeMapJar =  nodeResources.getAttributes();
                                                                                              jarPath = nodeMapJar.getNamedItem("href").getNodeValue();
                                                                                           }//if jar
                                                                      }//for nl Resources
                                                        }// if
                                    }//else if
                         }//for
              //    }


           System.out.println( node.getNodeName()+" desc");
           }
        }

            }catch(ParserConfigurationException pce)
            {
             pce.printStackTrace();
            }catch(SAXException se)
            {
            se.printStackTrace();
            }


      }
   catch(IOException ioex)
   {
     ioex.printStackTrace();
   }
  }

  public String getDevelopmentVersion()
  {
     return developmentVersion;
  }

  public String getReleasedVersion()
  {
    return releasedVersion;
  }

  public String getDescription()
  {
     return attrInformation;
  }

  public Document getDocument()
  {
    return doc;
  }

  public static String getCodeBase()
  {
    System.out.println(codeBase);
    return codeBase;
  }

  public String getJarPath()
  {
    System.out.println(jarPath);
    return jarPath;
  }
}
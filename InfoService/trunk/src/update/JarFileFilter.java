package update;

import java.io.File;
import javax.swing.filechooser.*;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */

public class JarFileFilter extends FileFilter
{

  private final String jarExtension = "jar";

  public JarFileFilter()
  {
  }
  public boolean accept(File parm1)
  {
      if (parm1.isDirectory())
         {
            return true;
         }

      String extension = getExtension(parm1);

   if(extension!=null)
       {

        if(extension.equals(jarExtension))
          {
              return true;
          }else
          {
              return false;
          }
        }
     return false;
  }
  public String getDescription()
  {
    String description = "Jar File "+"(*."+jarExtension+")";
    return description;
  }

  private String getExtension(File f)
  {
      String extension = null;
      String s;
        try{
              s = f.getName();
            }catch(Exception e)
            {
               e.printStackTrace();
               return null;
            }
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            extension = s.substring(i+1).toLowerCase();
        }
        return extension;
  }
}
package infoservice.agreement.logging;

import infoservice.agreement.interfaces.IInfoService;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * @author LERNGRUPPE A little file logger.
 * 
 */
public class FileLogger
{
    private boolean newGame = true;

    private IInfoService infoservice;

    /**
     * Creates a logger and uses the infoservice object to create a file name.
     * 
     * @param infoservice1
     *            A Infoservice.
     */
    public FileLogger(IInfoService infoservice1)
    {
        this.infoservice = infoservice1;
    }

    /**
     * Write out a given log string.
     * 
     * @param a_logString
     */
    public void writeOut(String a_logString)
    {
        String message = this.infoservice.getIdentifier() + "  ";
        message += a_logString;
        message += "\n";
        try
        {
            FileOutputStream fos = null;
            if (this.newGame)
            {
                fos = new FileOutputStream("logFromInfoservice" + this.infoservice.getIdentifier()
                        + ".txt");
                this.newGame = false;
                String head = "\nLog for infoservice " + this.infoservice.getIdentifier()
                        + "\n\n\n";
                fos.write(head.getBytes());
            } else
            {
                fos = new FileOutputStream("logFromInfoservice" + this.infoservice.getIdentifier()
                        + ".txt", true);

            }
            fos.write(message.getBytes());
            fos.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}

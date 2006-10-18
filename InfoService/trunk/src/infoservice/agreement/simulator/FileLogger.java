package infoservice.agreement.simulator;

import java.io.FileWriter;
import java.io.IOException;

public class FileLogger extends Logger
{

    FileWriter out;

    public FileLogger()
    {
        super();
        try
        {
            out = new FileWriter("log.txt");
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void log(LogHolder holder, String message)
    {
        try
        {
            out.write(holder.getPrefix() + holder.getIdentifier() + ":\t" + message + "\n");
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void flush()
    {
        try
        {
            out.flush();
            out.close();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void step()
    {
        try
        {
            out.write("\n");
            out.write("\n");
            out.write("\n");
        } catch (Exception e)
        {
            e.printStackTrace();

        }
    }

}

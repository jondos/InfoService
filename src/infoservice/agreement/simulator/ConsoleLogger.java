package infoservice.agreement.simulator;

public class ConsoleLogger extends Logger
{
    public void log(LogHolder holder, String message)
    {
        System.err.println(holder.getPrefix() + holder.getIdentifier() + ":\t" + message);
    }

    public void step()
    {
        System.err.println();
        System.err.println();
        System.err.println();
    }

}

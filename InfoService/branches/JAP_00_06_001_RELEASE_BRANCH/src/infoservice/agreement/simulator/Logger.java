package infoservice.agreement.simulator;

public abstract class Logger
{
    private static Logger instance;

    public abstract void log(LogHolder holder, String message);

    public static Logger getInstance()
    {
        if (instance == null)
            instance = new ConsoleLogger();
        return instance;
    }

    public void flush()
    {
        // TODO Auto-generated method stub

    }

    public void init()
    {
        // TODO Auto-generated method stub

    }

    public abstract void step();
}

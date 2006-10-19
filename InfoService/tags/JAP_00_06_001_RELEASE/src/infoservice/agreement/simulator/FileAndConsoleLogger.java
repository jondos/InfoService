package infoservice.agreement.simulator;

public class FileAndConsoleLogger extends Logger
{

    private FileLogger fileLogger = new FileLogger();

    private ConsoleLogger consoleLogger = new ConsoleLogger();

    public FileAndConsoleLogger()
    {
        super();
    }

    public void log(LogHolder holder, String message)
    {
        fileLogger.log(holder, message);
        consoleLogger.log(holder, message);
    }

    public void flush()
    {
        fileLogger.flush();
    }

    public void step()
    {
        fileLogger.step();
        consoleLogger.step();
    }

}

package logging;

final public class LogLevel
  {
    /** Indicates level type of message: Emergency message*/
    public final static int EMERG     = 0;
    /** Indicates level type of message: Alert message */
    public final static int ALERT     = 1;
    /** Indicates level type of message: For instance to  use when catching Exeption to output a debug message.*/
    public final static int EXCEPTION = 2; //2000-07-31(HF): CRIT zu EXCEPTION geaendert, wegen besserem Verstaendnis
    /** Indicates level type of message: Error message */
    public final static int ERR       = 3;
    /** Indicates level type of message: Warning */
    public final static int WARNING   = 4;
    /** Indicates level type of message: Notice */
    public final static int NOTICE    = 5;
    /** Indicates level type of message: Information */
    public final static int INFO      = 6;
    /** Indicates level type of message, e.g. a simple debugging message to output something */
    public final static int DEBUG     = 7;
  }
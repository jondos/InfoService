package logging;

final public class LogType
  {
    /** Indicates a GUI related message (binary: <code>00000001</code>) */
    public final static int GUI = 1;
    /** Indicates a network related message (binary: <code>00000010</code>) */
    public final static int NET = 2;
    /** Indicates a thread related message (binary: <code>00000100</code>) */
    public final static int THREAD = 4;
    /** Indicates a misc message (binary: <code>00001000</code>) */
    public final static int MISC = 8;
  }
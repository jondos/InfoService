package rmi;
import AnonServerDBEntry;

/** This class had to be made because of bugs in the JAVA RMI Compiler
 *  It provides all features of AnonServerDBEntry
 */
public class MixCascade {
    private String host;
    private int    port;
    private String name;

    public MixCascade (String n,String h, int p) {
        host = h;
        port = p;
        name=n;
    }

    public MixCascade (AnonServerDBEntry entry) {
        host = entry.getHost();
        name = entry.getName();
        port = entry.getPort();
    }

    public String getName()
    {
        return name;//host+":"+Integer.toString(port);
    }

    public int getPort(){
        return port;
    }

    public String getHost(){
        return host;
    }

    public AnonServerDBEntry getAnonServerDBEntry() {
        return new AnonServerDBEntry(name, host, port);
    }
}
package anon.xmlrpc;

public abstract class Server
  {
    public abstract int start();
    public abstract void stop();
    static public Server generateServer()
      {
        try
          {
            Class c=Class.forName("anon.xmlrpc.ServerImpl");
            return (Server)c.newInstance();
          }
        catch(Exception e)
          {
            return null;
          }
      }
 }
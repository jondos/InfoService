package anon.xmlrpc;

public abstract class Server
  {
    public abstract int start();
    public abstract void stop();
    public static Server generateServer()
      {
        return new ServerImpl();
      }
  }
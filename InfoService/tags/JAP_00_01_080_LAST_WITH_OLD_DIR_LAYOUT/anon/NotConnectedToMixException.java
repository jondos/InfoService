package anon;

import java.net.ConnectException;
public class NotConnectedToMixException extends ConnectException
{
  public NotConnectedToMixException(String msg)
    {
      super(msg);
    }
}
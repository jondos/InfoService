package jpi;
public class RequestException extends Exception
{
    private int m_errorCode;
    private String m_bodyText;

    public RequestException (int errorCode)
    {
        this(errorCode,null);
    }

    public RequestException (int errorCode, String bodyText)
    {
        m_errorCode=errorCode;
        m_bodyText=bodyText;
    }

    public int getErrorCode()
    {
        return m_errorCode;
    }

    public String getBodyText()
    {
        return m_bodyText;
    }
}


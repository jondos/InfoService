package infoservice.agreement.common;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class TimeoutThread extends Thread
{
    private boolean m_canceled = false;

    private TimeoutListener m_listener;

    private long m_timeout;

    private Object m_value;

    public TimeoutThread(TimeoutListener a_listener, Object a_value, long a_timeout)
    {
        this.m_listener = a_listener;
        this.m_value = a_value;
        this.m_timeout = a_timeout;
    }

    public TimeoutThread(TimeoutListener a_listener, long a_timeout)
    {
        this.m_listener = a_listener;
        this.m_timeout = a_timeout;
    }

    public void run()
    {
        try
        {
            Thread.sleep(m_timeout);
        }
        catch (InterruptedException e)
        {
            LogHolder.log(LogLevel.WARNING, LogType.AGREEMENT, "Unable to sleet in thread!", e);
        }
        if (!this.m_canceled)
        {
            this.m_listener.timeout(m_value);
        }
    }

    public void cancel()
    {
        m_canceled = true;
    }
}

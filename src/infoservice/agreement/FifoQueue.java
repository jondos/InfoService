package infoservice.agreement;

import infoservice.agreement.multicast.messages.RawMessage;

/**
 * 
 * @author LERNGRUPPE A first in first out queue used for holding zipped
 *         messages.
 * 
 */
class FifoQueue
{
    /**
     * An array for holding the messages.
     */
    private RawMessage[] m_messages = new RawMessage[10];

    /**
     * An index pointer.
     */
    private int m_writeIndex = 0;

    /**
     * Push a message into the stack.
     * 
     * @param a_message
     *            A message to push.
     */
    public synchronized void push(RawMessage a_message)
    {
        if (m_writeIndex >= m_messages.length)
        {
            RawMessage[] tmp = new RawMessage[m_messages.length + 10];
            System.arraycopy(m_messages, 0, tmp, 0, m_messages.length);
            m_messages = new RawMessage[tmp.length];
            System.arraycopy(tmp, 0, m_messages, 0, tmp.length);
            push(a_message);
        } else
        {
            m_messages[m_writeIndex] = a_message;
            m_writeIndex++;
        }
    }

    /**
     * Get a message from the stack.
     * 
     * @return The message.
     */
    public synchronized RawMessage pop()
    {
        RawMessage result = m_messages[0];
        if (m_messages.length > 10)
        {
            RawMessage[] tmp = new RawMessage[m_messages.length - 1];
            System.arraycopy(m_messages, 1, tmp, 0, m_messages.length - 1);
            m_messages = new RawMessage[tmp.length];
            System.arraycopy(tmp, 0, m_messages, 0, tmp.length);
        } else
        {
            System.arraycopy(m_messages, 1, m_messages, 0, m_messages.length - 1);
        }
        if (m_writeIndex > 0)
            m_writeIndex--;
        for (int i = m_writeIndex; i < m_messages.length; i++)
        {
            m_messages[i] = null;
        }
        return result;
    }

    /**
     * Clears the stack.
     * 
     */
    public void clear()
    {
        m_messages = new RawMessage[10];
    }

    /**
     * Count the entries.
     * 
     * @return Number of entries hold in the queue.
     */
    public int getMessageCount()
    {
        int count = 0;
        for (int i = 0; i < m_messages.length; i++)
        {
            if (m_messages[i] != null)
                count++;
        }
        return count;
    }
}

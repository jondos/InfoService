package anon.tor;

import anon.tor.cells.*;
final class CellQueue
{
	final class CellQueueEntry
	{
		Cell m_Cell;
		CellQueueEntry m_next;
		CellQueueEntry(Cell c)
		{
			m_Cell=c;
			m_next=null;
		}
	}
	private CellQueueEntry m_firstEntry;
	private CellQueueEntry m_lastEntry;

	public CellQueue()
	{
		m_firstEntry=null;
		m_lastEntry=null;
	}

	public synchronized void addElement(Cell c)
	{
		CellQueueEntry entry=new CellQueueEntry(c);
		if(m_lastEntry==null)
		{
			m_firstEntry=m_lastEntry=entry;
		}
		else
		{
			m_lastEntry.m_next=entry;
			m_lastEntry=entry;
		}
	}

	public synchronized Cell removeElement()
	{
		if(m_firstEntry==null)
			return null;
		Cell c=m_firstEntry.m_Cell;
		m_firstEntry=m_firstEntry.m_next;
		if(m_firstEntry==null)
			m_lastEntry=null;
		return c;
	}

	public synchronized boolean isEmpty()
	{
		return m_firstEntry==null;
	}
}

/*
 Copyright (c) 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
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

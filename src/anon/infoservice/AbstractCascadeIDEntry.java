/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package anon.infoservice;

/**
 * This database class stores the IDs of all mixes in a cascade in a single string. It may be
 * used to determine previously known cascades.
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractCascadeIDEntry extends AbstractDatabaseEntry
{
	private String m_ID;
	private long m_version;
	private String m_cascadeID;

	/**
	 * Creates a new CascadeIDEntry from the mix IDs of a given cascade.
	 * @param a_cascade MixCascade
	 * @param a_expireTime the time in ms when this databse entry expires
	 * @throws java.lang.IllegalArgumentException if the given cascade is null
	 */
	public AbstractCascadeIDEntry(MixCascade a_cascade, long a_expireTime) throws IllegalArgumentException
	{
		super(a_expireTime);
		if (a_cascade == null)
		{
			throw new IllegalArgumentException("Given cascade is null!");
		}
		m_ID = a_cascade.getMixIDsAsString();
		m_version = System.currentTimeMillis();
		m_cascadeID = a_cascade.getId();
	}

	/**
	 * Creates a new CascadeIDEntry from the mix IDs of a given cascade.
	 * @param a_cascade MixCascade
	 * @param a_expireTime the time in ms when this databse entry expires
	 * @throws java.lang.IllegalArgumentException if the given AbstractCascadeIDEntry is null
	 */
	public AbstractCascadeIDEntry(AbstractCascadeIDEntry a_entry, long a_expireTime) throws IllegalArgumentException
	{
		super(a_expireTime);
		if (a_entry == null)
		{
			throw new IllegalArgumentException("Given cascade is null!");
		}
		m_ID = a_entry.getId();
		m_version = System.currentTimeMillis();
		m_cascadeID = a_entry.getCascadeId();
	}

	/**
	 * The cascade ID, that means the ID of the first mix in the cascade.
	 * @return the ID of the first mix in the cascade
	 */
	public final String getCascadeId()
	{
		return m_cascadeID;
	}

	/**
	 * The concatenated ID of all mixes in the cascade.
	 * @return the concatenated ID of all mixes in the cascade
	 */
	public final String getId()
	{
		return m_ID;
	}

	/**
	 * Returns version number which is used to determine the more recent infoservice entry, if two entries
	 * are compared (higher version number -> more recent entry).
	 *
	 * @return The version number for this entry.
	 */
	public final long getVersionNumber()
	{
		return m_version;
	}
}

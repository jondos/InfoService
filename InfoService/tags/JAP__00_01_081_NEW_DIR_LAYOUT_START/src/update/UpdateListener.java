/*
Copyright (c) 2000, The JAP-Team
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

package update;

/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */

  public interface UpdateListener
  {
    public final static int STATE_IN_PROGRESS_STEP1=1;
    public final static int STATE_ABORTED_STEP1=2;
    public final static int STATE_FINISHED_STEP1=3;

    public final static int DOWNLOAD_START = 4;
    public final static int DOWNLOAD_ABORT = 5;
    public final static int DOWNLOAD_READY = 6;

    public final static int STATE_IN_PROGRESS_STEP3=7;
    public final static int STATE_ABORTED_STEP3=8;
    public final static int STATE_FINISHED_STEP3=9;

    public final static int STATE_IN_PROGRESS_STEP4=10;
    public final static int STATE_ABORTED_STEP4=11;
    public final static int STATE_FINISHED_STEP4=12;

    public final static int STATE_IN_PROGRESS_STEP5=13;
    public final static int STATE_ABORTED_STEP5=14;
    public final static int STATE_FINISHED_STEP5=15;



    /** Invoked to signal the progress of the Update
     *  @param lenData, the size of the data-piece
     *  @param lenTotal, the total size of data to manipulte(copy, writing, etc.)
     *  @param state, the current download state (STATE_IN_PROGRESS_STEPX, STATE_ABORTED_STEPX)
     *  @return 0, if the update should continue
     *          -1, if the download should be aborted
     */
    public int progress(int lenData,int lenTotal,int state);
    //public int progress(byte[] data,int lenData,int lenTotal,int state);
  }
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
  }
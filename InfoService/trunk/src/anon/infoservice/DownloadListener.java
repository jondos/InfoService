package anon.infoservice;

public interface DownloadListener
	{
		public final static int STATE_IN_PROGRESS=1;
		public final static int STATE_ABORTED=2;
		public final static int STATE_FINISHED=3;

		/** Invoked to signal the progress of a download.
		 *  @param data contains some piece of downloaded data
		 *  @param lenData the size of the data-piece
		 *  @param lenTotal the total size of data to download
		 *  @param state the current download state (STATE_IN_PROGRESS, STATE_ABORTED)
		 *  @return 0 if the download should continue
		 *          -1 if the download should be aborted
		 */
		public int progress(byte[] data,int lenData,int lenTotal,int state);
	}
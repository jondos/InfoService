
public final class JAPMacintosh extends JAP {
	
	JAPMacintosh(String[] argv) {
		super(argv);
	}
	
	protected void registerMRJHandlers() {
		//Register MRJ handlers for about and quit.
		MRJI IMRJI = new MRJI();
		com.apple.mrj.MRJApplicationUtils.registerQuitHandler(IMRJI);
		com.apple.mrj.MRJApplicationUtils.registerAboutHandler(IMRJI);
	}

	// Inner class defining the MRJ Interface
	class MRJI implements com.apple.mrj.MRJQuitHandler, com.apple.mrj.MRJAboutHandler
	{
		public void handleQuit() {
			model.goodBye();
		}
		public void handleAbout() {
			model.aboutJAP();
		}
	}
	
	public static void main(String[] argv) {
		JAPMacintosh japOnMac = new JAPMacintosh(argv);
		japOnMac.startJAP();
		japOnMac.registerMRJHandlers();
	}
	
}

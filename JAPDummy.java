import java.util.*;

public class JAPDummy implements Runnable {
	private JAPModel model;
	private JAPView view;
	static int seed = 1;
	
	public JAPDummy (JAPModel m, JAPView v) {
		this.model = m;
		this.view  = v;
	}
	
	public void run() {
		Random rand = new Random(seed++);
		int i = 0;
		int j = model.trafficSituation;
//		String[] ani = { "-" , "\\" , "|" , "/" }; 
		String[] ani = { "." , ".." , "..." , "....", "....." }; 

		while(true) {
			if (i==ani.length) i=0;
			model.status1=model.msg.getString("statusRunning") + ani[i];
			i++;
			//
			float r = rand.nextFloat();
			if (r > .5) {
				if(j < model.MAXPROGRESSBARVALUE) j+=2;
			} else {
				if (j > 2) j-=2;
			}
			model.trafficSituation = j;
			// Update view
//			view.updateValues();
			model.notifyJAPObservers();
			try {
				Thread.sleep(1000);
			} catch (Exception e) { 
				System.out.println("Exception catched in JAPDummy"); 
			}
		}
	}
}
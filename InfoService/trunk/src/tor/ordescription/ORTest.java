package tor.ordescription;
import java.io.IOException;

/*
 * Created on Mar 25, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ORTest {

	public static void main(String[] args) throws IOException {
		ORList orl= new ORList();
		orl.updateList("18.244.0.188",9033);
	}
}

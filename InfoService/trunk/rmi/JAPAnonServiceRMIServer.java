package rmi;
import java.rmi.Naming;
/*
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */


/** Creates the RMI-Server so that the Client can connect. Note: You need to run RMIRegistry first on your system */
public class JAPAnonServiceRMIServer {

    public JAPAnonServiceRMIServer() {
        JAPAnonServiceInterface jasi = null;
        try {
            jasi = new JAPAnonServiceImpl();
        } catch (Exception e) {
            System.out.println("1 ---");
            e.printStackTrace();
        }
        try {
            Naming.rebind("rmi://localhost:1099/AnonService", jasi);
        } catch (Exception e) {
            System.out.println("2 ---");
            e.printStackTrace();
        }
    }

    /** FOR TESTING PURPOSES ONLY */
    public static void main(String args[]) {
        new JAPAnonServiceRMIServer();
    }
}
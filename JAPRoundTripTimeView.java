import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.BindException;
import java.net.UnknownHostException;
import java.io.IOException;


/**
 * User Interface for an Mix-Cascade echo.
 * 
 * @version 0.1
 * @author  Jens Hillert
 */
public class JAPRoundTripTimeView implements Runnable {
	
	private final boolean DEBUG = true;
	/** Specifies the timeout between 2 connections in milliseconds */
	public static final int TIMEOUT = 1000;
	/** Specifies the number of requests for each turn */
	public static final int MAX_REQUESTS = 10;
	
	// statisics
	private int   minAtAll = 50000;
	private int   sumAtAll = 0;
	private int   maxAtAll = 0;
	private int   avgAtAll = 0;
	private int   sum = 0;
	private int[] minPerConnection = new int[JAPRoundTripTime.MAX_STATIONS];
	private int[] avgPerConnection = new int[JAPRoundTripTime.MAX_STATIONS];
	private int[] maxPerConnection = new int[JAPRoundTripTime.MAX_STATIONS];
	private int[] sumPerConnection = new int[JAPRoundTripTime.MAX_STATIONS];
	
	int[] times = new int[JAPRoundTripTime.MAX_STATIONS];
	private int anzahlStationen;
	private int durchlaufNummer           = 1;
	private boolean absoluteWerteAnzeigen = true;
	private boolean testRuns              = false;
	private String[] allMyAddressesStringArray;
	private Thread rttThread;
	private AnonServerDBEntry[] serverList;
	private	JPanel panelCenter            = new JPanel(true);
	private JPanel[] progressPanel        = new JPanel[JAPRoundTripTime.MAX_STATIONS + 2];
	private final JButton startButton     = new JButton("Start");
	private final JButton stopButton      = new JButton("Stop");
	private final JComboBox mixComboBox   = new JComboBox();
	private final JComboBox adrComboBox   = new JComboBox();
	private JLabel[] myProgressBarDesc1   = new JLabel[JAPRoundTripTime.MAX_STATIONS];
	private JLabel[] myProgressBarDesc2   = new JLabel[JAPRoundTripTime.MAX_STATIONS];
	private JProgressBar[] myProgressBar1 = new JProgressBar[JAPRoundTripTime.MAX_STATIONS];
	private JProgressBar[] myProgressBar2 = new JProgressBar[JAPRoundTripTime.MAX_STATIONS];
	private JLabel sumProgressBarDesc1    = new JLabel("Gesamtzeit:");
	private JLabel sumProgressBarDesc2    = new JLabel("min - / avg - / max -", JLabel.RIGHT);
	private JProgressBar sumProgressBar1  = new JProgressBar();
	private JProgressBar sumProgressBar2  = new JProgressBar();
	
	/**
	 * The Main Method
	 */
    public static void main(String[] args) {
        System.out.println("Starting Application ...\n");
 		AnonServerDBEntry[] initialServerList = new AnonServerDBEntry[2];
		AnonServerDBEntry myEntry0 = new AnonServerDBEntry("obiwan:4453", "obiwan", 4453);
		AnonServerDBEntry myEntry1 = new AnonServerDBEntry("192.168.0.2:4453", "192.168.0.2", 4453);
		initialServerList[0] = myEntry0;
		initialServerList[1] = myEntry1;
		new JAPRoundTripTimeView (initialServerList);
    }
	
	/**
	 * Constructor 
	 */
	JAPRoundTripTimeView (AnonServerDBEntry[] initialServerList) {
        try {
            UIManager.setLookAndFeel (UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }
		
		// Creating Frame & Components
        JFrame mainFrame = new JFrame("Round Trip Time");
		Component contents = this.createComponents(initialServerList);
		mainFrame.getContentPane().add(contents, BorderLayout.CENTER);
		
		// Finish setting up the Frame and showing ist
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
		mainFrame.pack();
		mainFrame.setVisible(true);
	}
	
	
	/**
	 * This Method is used by the main method to define all of the Windows Components.
	 */
	private Component createComponents(AnonServerDBEntry[] initialServerList) {
		JPanel myPanel = new JPanel();
		
		// The Northern Part Definitions
		GridBagLayout gbLayoutNorth = new GridBagLayout();
		GridBagConstraints c        = new GridBagConstraints();
		JPanel panelNorth           = new JPanel(gbLayoutNorth);
		JLabel myLabel              = new JLabel("Mix Kaskade: ");
		JRadioButton valueAbsoluteButton   = new JRadioButton("Absolute Werte");
		JRadioButton valuePercentButton    = new JRadioButton("Werte in Prozent");
		ButtonGroup chooseValueButtonGroup = new ButtonGroup();
		JPanel panelControlls1 = new JPanel();
		panelControlls1.setLayout(new BoxLayout(panelControlls1, BoxLayout.Y_AXIS));

		// The Middle Part Definitions
		GridBagLayout myGridBag = new GridBagLayout();
		panelCenter.setLayout(myGridBag);
		JScrollPane scrollPane = new JScrollPane(panelCenter);
		for (int i = 0; i < (JAPRoundTripTime.MAX_STATIONS + 2); i++) {
			progressPanel[i]   = new JPanel(new GridLayout(0,2,2,2), true);
		}

		// The Southern Part Definitions
		JPanel panelSouth   = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		// startButton & stopButton are global
		final JButton closeButton = new JButton("Schließen");
		
		// Northern part *************************************
		chooseValueButtonGroup.add(valueAbsoluteButton);
		chooseValueButtonGroup.add(valuePercentButton);
		RadioListener myListener = new RadioListener();
		valueAbsoluteButton.setActionCommand("Show absolute value");
		valuePercentButton.setActionCommand("Show in percent");
		valueAbsoluteButton.addActionListener(myListener);
		valueAbsoluteButton.setSelected(true);
		absoluteWerteAnzeigen = true;
		valuePercentButton.addActionListener(myListener);
		panelControlls1.add(valuePercentButton);
		panelControlls1.add(valueAbsoluteButton);
		
		
		panelNorth.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		//c.ipadx = 5;
		//c.ipady = 5;
		c.fill  = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		panelNorth.add(myLabel, c);
		c.weightx = 1;
		c.insets  = new Insets(0,10,0,10);
		panelNorth.add(mixComboBox, c);
		c.weightx    = 0;
		c.gridheight = 2;
		c.insets  = new Insets(0,0,0,0);
		panelNorth.add(panelControlls1, c);
		c.gridheight = 1;
		c.gridx   = 0;
		c.gridy   = 1;
		c.insets  = new Insets(0,0,0,0);
		panelNorth.add(new JLabel("Lokale Adresse: "), c);
		c.gridx   = 1;
		c.gridy   = 1;
		c.insets  = new Insets(0,10,0,10);
		panelNorth.add(adrComboBox, c);

		// setting the Combo List Entries
		mixComboBox.setEditable(true);
		setMixList(initialServerList);
		setAdrList();
 		
		// Center part ***************************************
		panelCenter.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		sumProgressBarDesc1.setIcon(new ImageIcon("Server16.gif"));
		sumProgressBar1.setString("");
		sumProgressBar1.setStringPainted(true);
		sumProgressBar2.setString("");
		sumProgressBar2.setStringPainted(true);
		progressPanel[0].add(sumProgressBarDesc1);
		progressPanel[0].add(sumProgressBarDesc2);
		progressPanel[0].add(sumProgressBar1);
		progressPanel[0].add(sumProgressBar2);
		
		progressPanel[1] = new JPanel(new GridLayout(0,1,2,2), true);
		progressPanel[1].setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
		progressPanel[1].add(new JSeparator(SwingConstants.HORIZONTAL));
		
		// adding Progress Bars
		myProgressBarDesc1[0] = new JLabel("lokal - Mix 1");
		myProgressBarDesc1[0].setIcon(new ImageIcon("Host16.gif"));
		myProgressBar1[0]     = new JProgressBar(0,100);
		myProgressBar1[0].setString("");
		myProgressBar1[0].setStringPainted(true);
		myProgressBarDesc2[0] = new JLabel("min - / avg - /max -", JLabel.RIGHT);
		myProgressBar2[0]     = new JProgressBar(0,100);
		myProgressBar2[0].setString("");
		myProgressBar2[0].setStringPainted(true);
		progressPanel[2].add(myProgressBarDesc1[0]);
		progressPanel[2].add(myProgressBarDesc2[0]);
		progressPanel[2].add(myProgressBar1[0]);
		progressPanel[2].add(myProgressBar2[0]);
		
		
		for (int i = 1; i < (JAPRoundTripTime.MAX_STATIONS - 1); i++){
			myProgressBarDesc1[i] = new JLabel("Mix " + (i) + " - Mix " + (i + 1));
			myProgressBarDesc1[i].setIcon(new ImageIcon("Server16.gif"));
			myProgressBarDesc2[i] = new JLabel("min - / avg - /max -", JLabel.RIGHT);
			myProgressBar1[i]     = new JProgressBar(0,100);
			myProgressBar1[i].setString("");
			myProgressBar1[i].setStringPainted(true);
			myProgressBar2[i]     = new JProgressBar(0,100);
			myProgressBar2[i].setString("");
			myProgressBar2[i].setStringPainted(true);
			progressPanel[i+2].add(myProgressBarDesc1[i]);
			progressPanel[i+2].add(myProgressBarDesc2[i]);
			progressPanel[i+2].add(myProgressBar1[i]);
			progressPanel[i+2].add(myProgressBar2[i]);
		}
		// putting all the stuff togehter in panelCenter.
		for (int i = 0; i < 3; i++) {
			c.gridx = 0;
			c.gridy = i;
			c.fill  = GridBagConstraints.BOTH;
			c.weightx = 1;
			myGridBag.setConstraints(progressPanel[i], c);
			panelCenter.add(progressPanel[i]);
		}
		
//SK13		
		JLabel l=new JLabel(" ");
		c.gridy = 3;
		c.weighty=1;
		myGridBag.setConstraints(l, c);
		panelCenter.add(l);
//END SK13		
		
		// Southern part *************************************
		startButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        testRuns = true;
				stopButton.setEnabled(true);
				startButton.setEnabled(false);
				startRequest();
		    }
		});
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        testRuns = false;
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				stopRequest();
		    }
		});
		closeButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				stopRequest();
		        System.exit(0);
		    }
		});
		
		panelSouth.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		panelSouth.add(startButton);
		panelSouth.add(stopButton);
		panelSouth.add(closeButton);
			
		// Putting it all togehter into the top Level Container.
		myPanel.setPreferredSize(new Dimension(450, 400));
		myPanel.setMinimumSize(new Dimension(450, 400));
		myPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		myPanel.setLayout(new BorderLayout());
		myPanel.add(panelNorth,"North");
		//JPanel tmpPanel=new JPanel(new BorderLayout());
		//tmpPanel.setBorder(BorderFactory.createLineBorder(tmpPanel.getForeground()));
		//scrollPane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		//tmpPanel.add(scrollPane, "North");
		myPanel.add(scrollPane, "Center");
		myPanel.add(panelSouth,  "South");
		
		return myPanel;
	}
	
	/**
	 * This method is invoked by the run() method
	 * it ist responsible for the actualisation of the
	 * progressBars
	 */
	private void doRequest(String host, int port, String localAddress) {
		try {
			JAPRoundTripTime rttTest = new JAPRoundTripTime(host, port, localAddress);
			
			// getting the results
			times = rttTest.getRoundTripTime();
			if (anzahlStationen < times.length) {
				anzahlStationen = times.length;
				showAmountOfProgressBars(anzahlStationen);
			}
			
			// doing some calculation
			sum = 0;
			for (int i = 0; i < times.length; i++) {
				sum = sum + times[i];
			}
			
			sumAtAll = sumAtAll + sum;
			if (sum > maxAtAll) maxAtAll = sum;
			if (sum < minAtAll) minAtAll = sum;
			avgAtAll = sumAtAll / durchlaufNummer;
			
			// actualising the Swing Components in another Method for Thread Safety
			showProgressBars (times);
		
		// catching all the exceptions that can be thrown in JAPRoundTripTime
		} catch (BindException e) {
			System.out.println("ERROR: " + e.getMessage());
		} catch (UnknownHostException e) {
			System.out.println("ERROR: " + e.getMessage());
		} catch (IOException e) {
			  System.out.println("ERROR: " + e.getMessage());
		} catch (Exception e) {
			  System.out.println("SWING ERROR: " + e.getMessage());
		}
	}
	
	/**
	 * Actualizing the Components ThreadSafe
	 */
	void showProgressBars (int[] myTimes) throws Exception {
		final int[] times = myTimes;
		Runnable actualizeProgressBars = new Runnable() {
			public void run() {
				sumProgressBar1.setMaximum(maxAtAll);
				sumProgressBar1.setValue(sum);
				sumProgressBar1.setString(sum+" ms");
				
				sumProgressBar2.setMaximum(maxAtAll);
				sumProgressBar2.setValue(avgAtAll);
				sumProgressBar2.setString(avgAtAll+" ms");
				sumProgressBarDesc2.setText("min " + minAtAll 
											+ " / avg " + avgAtAll 
											+ " / max " + maxAtAll);
				
				for (int i = 0; i < (times.length); i++) {
					sumPerConnection[i] = sumPerConnection[i] + times[i];
					if (minPerConnection[i] > times[i]) minPerConnection[i] = times[i];
					if (maxPerConnection[i] < times[i]) maxPerConnection[i] = times[i];
					avgPerConnection[i] = sumPerConnection[i] / durchlaufNummer;
								
					if (absoluteWerteAnzeigen) {
						myProgressBar1[i].setMaximum(maxAtAll);
						myProgressBar1[i].setValue(times[i]);
						myProgressBar1[i].setString(times[i] + " ms");

						myProgressBar2[i].setMaximum(maxAtAll);
						myProgressBar2[i].setValue(avgPerConnection[i]);
						myProgressBar2[i].setString(avgPerConnection[i] + " ms");
					} else {
						myProgressBar1[i].setMaximum(100);
						myProgressBar1[i].setValue((times[i] * 100 / sum));
						myProgressBar1[i].setString((times[i] * 100 / sum) + " %");

						myProgressBar2[i].setMaximum(maxAtAll);
						myProgressBar2[i].setValue((avgPerConnection[i] * 100 / avgAtAll));
						myProgressBar2[i].setString((avgPerConnection[i] * 100 / avgAtAll) + " %");
					}
					myProgressBarDesc2[i].setText("min " + minPerConnection[i] 
												  + " / avg " + avgPerConnection[i]  
												  + " / max " + maxPerConnection[i]);
				}
			}
		};
		SwingUtilities.invokeAndWait(actualizeProgressBars);
	}
	
	
	/**
	 * Actualizing the amount of the ProgressBars
	 */
	void showAmountOfProgressBars (int myAmount) throws Exception {
		final int amount = myAmount;
		Runnable actualizeProgressBars = new Runnable() {
			public void run() {
				GridBagLayout myGridBag = new GridBagLayout();
				GridBagConstraints c = new GridBagConstraints();
				
				// remove all components from
				panelCenter.removeAll();
				panelCenter.setLayout(myGridBag);
				
				// add all necessary Components
				for (int i = 0; i < (amount + 2); i++) {
					c.gridx = 0;
					c.gridy = i;
					c.fill  = GridBagConstraints.BOTH;
					c.weightx = 1;
					myGridBag.setConstraints(progressPanel[i], c);
					panelCenter.add(progressPanel[i]);
				}
//SK13
				JLabel l=new JLabel(" ");
				c.gridy=amount+2;
				c.weighty=1;
				myGridBag.setConstraints(l, c);
				panelCenter.add(l);
//END SK13
			}
		};
		SwingUtilities.invokeAndWait(actualizeProgressBars);
	}
	
	
	/**
	 * stops a request, is invoked by the stopButton
	 */
	private void stopRequest() {
		testRuns = false;
		rttThread = null;
		sumAtAll = 0;
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
	}
	
	/**
	 * starts a request, is invoked by the startButton
	 */
	private void startRequest() {
		testRuns = true;
		sumAtAll = 0;
		maxAtAll = 0;
		minAtAll = JAPRoundTripTimeView.TIMEOUT;
		avgAtAll = 0;
		anzahlStationen = 0;
		for (int i = 0; i < JAPRoundTripTime.MAX_STATIONS; i++) {
			minPerConnection[i] = JAPRoundTripTimeView.TIMEOUT;
			avgPerConnection[i] = 0;
			maxPerConnection[i] = 0;
			sumPerConnection[i] = 0;
		}
		if (rttThread == null) {
			rttThread = new Thread(this, "RoundTripTime");
			rttThread.start();
		}
	}
	
	/** start Method - function is provided by startRequest */
	private void start() {
	}
	
	/** 
	 * Sends MAX_REQUEST request to the server, sleeping 
	 * for TIMEOUT milliseconds between each request.
	 * 
	 * <br>If there are more than one local addresses for each address a
	 * reply address packet is sent to figure out, which one fits.
	 * 
	 * @see MAX_REQUEST
	 * @see TIMEOUT
	 */
	public synchronized void run() {
		int anzahlStationen = 1;
		Thread myThread = Thread.currentThread();
		durchlaufNummer = 0;
		String[] replyAddress = new String[2];
		replyAddress[0] = "localhost";
		replyAddress[1] = "1";
		String myString = new String(mixComboBox.getSelectedItem().toString());
		String address  = new String(myString.substring(0, myString.lastIndexOf(":")));
		int port        = 4453;
		try {
			port = Integer.parseInt(new String(myString.substring(1 + myString.lastIndexOf(":"), myString.length()).trim()));
		} catch (NumberFormatException e){}
		/*DEBUG*/System.out.println("Anfrage an: " + address + ":" + port);

// LOKALE ANTWORTADRESSE AUS DER LISTBOX LESEN UND ANZAHL DER MIXE MANUELL BESTIMMEN.				 
		replyAddress[0] = adrComboBox.getSelectedItem().toString();
				 
// LOKALE ANTWORTADRESSE UND MIXANZAHL AUTOMATISCH BESTIMMEN				 
/*		// Get the local reply addresses
		// Repeat figuring out the address until the 1st answer arrives
		while ((replyAddress[0] == "localhost") && (testRuns == true)) {
			if (mixComboBox.getSelectedIndex() >= 0){
				replyAddress = JAPRoundTripTime.getLocalReplyAddress(serverList[mixComboBox.getSelectedIndex()].getHost(), 
																			serverList[mixComboBox.getSelectedIndex()].getPort());
			} else {
				replyAddress = JAPRoundTripTime.getLocalReplyAddress(address, port);
			}
			//DEBUG/System.out.println("Rückgabewert: " + replyAddress[1]);
		}
		if ((replyAddress[1] == "1") || (replyAddress[1] == "0")) stopRequest();
		// get the amount of different mixes
		anzahlStationen = Integer.parseInt(replyAddress[1].trim());
		/*DEBUG/System.out.println("Anzahl der Stationen:" + anzahlStationen);
*/
		try {
			showAmountOfProgressBars(anzahlStationen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// do all the requests until the stop button is pressed.
		while (rttThread == myThread) {
			if ((testRuns == true) /*&& (durchlaufNummer < MAX_REQUESTS)*/) {
				durchlaufNummer++;
				
				// predefined entry from mixComboBox
				if (mixComboBox.getSelectedIndex() >= 0){
					/*DEBUG doRequest(serverList[mixComboBox.getSelectedIndex()].getHost(), 
							 serverList[mixComboBox.getSelectedIndex()].getPort(), 
							 allMyAddressesStringArray[adrComboBox.getSelectedIndex()]);*/
					doRequest(serverList[mixComboBox.getSelectedIndex()].getHost(), 
							 serverList[mixComboBox.getSelectedIndex()].getPort(), 
							 replyAddress[0]);
				// user defined entry in mixComboBox
				} else if (mixComboBox.getSelectedIndex() == -1) {
					/*DEBUGdoRequest(address, port, allMyAddressesStringArray[adrComboBox.getSelectedIndex()]);*/
					doRequest(address, port, replyAddress[0]);
				}
				try {
					Thread.sleep(TIMEOUT);
				} catch (InterruptedException e) {
					// Get back to work
				}
			} else {
				stopRequest();
			}
		}
		
		// Hide all progress bars that are not necessary
		for (int i = 3; i < JAPRoundTripTime.MAX_STATIONS + 2; i++) {
			;
		}
	}
	
	/** Method for automaticly stopping the Thread on Exit. */
	private void stop() {
		rttThread = null;
	}
	
	/** Used to import a list of AnonServerDBEntry into the ComboBox */
	public void setMixList(AnonServerDBEntry[] anonServerList){
		serverList = new AnonServerDBEntry[anonServerList.length];
		System.arraycopy (anonServerList, 0, serverList, 0, anonServerList.length);
		
		// Combo Box aktualisieren!!!
		mixComboBox.removeAllItems();
		for (int i = 0; i < anonServerList.length; i++) {
			mixComboBox.addItem(serverList[i].getName());
		}
	}
	
	/** Gets all available local Addresses */
	public void setAdrList () {
		allMyAddressesStringArray = new String[1];
		InetAddress[] allMyAddresses;
		allMyAddressesStringArray[0] = "ERROR";
		try {
			int soManyAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName()).length;
			allMyAddressesStringArray = new String[soManyAddresses];
			allMyAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			/*DEBUG*/System.out.println("All local Addresses:");
			for (int i = 0; i < soManyAddresses; i++){
				allMyAddressesStringArray[i] = allMyAddresses[i].getHostAddress();
				/*DEBUG*/System.out.println((i + 1) + ". address " + allMyAddresses[i].getHostAddress());
			}
		} catch (IOException e) {
			System.out.println("Your local IP-Adress could not be find. Are you connected to the Internet?");
			e.printStackTrace();
			allMyAddressesStringArray = new String[1];
			allMyAddressesStringArray[0] = "localhost";
		}
		
		// Combo Box aktualisieren
		adrComboBox.removeAllItems();
		for (int i = 0; i < allMyAddressesStringArray.length; i++) {
			adrComboBox.addItem(allMyAddressesStringArray[i]);
		}
	}
	
	
    /** Listens to the radio buttons. */
    class RadioListener implements ActionListener { 
        public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == "Show in percent"){
				absoluteWerteAnzeigen = false;
			} else {
				absoluteWerteAnzeigen = true;
			};
        }
    }

}

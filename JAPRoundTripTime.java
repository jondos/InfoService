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
import java.net.*;
import java.io.*;
import java.util.Date;

/**
 * This class creates an echo request with a timestamp, sends it 
 * to the first Mix and waits for the answer of all Mixes coming 
 * back the same way (from Mix 1)
 * 
 * @version 0.1
 * @author  Jens Hillert
 */
public class JAPRoundTripTime {
	
	boolean DEBUG = true;
	/** maximum numbers of mixes including the local client. */
	public static final int MAX_STATIONS = 11;
	/** Specifies the maximum timeout to wait for an answer */
	public static final int TIMEOUT = 5000;
	
	private byte[] sendbuf    = new byte[2 * 2 + 7 * 2 ];
	private byte[] receivebuf = new byte[2 * 2 + MAX_STATIONS * 4 * 2 ];
	private InetAddress address;
	private InetAddress myAddress;
//	private InetAddress[] allMyAddresses;
	private DatagramSocket socket;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private int port;
	private String[] allMyAddressesStringArray;
		
	/**
	 * Sends a request to the Server hostName:portNumber.<br>
	 * myAdressIndex should be used only if there are more than one 
	 * local addresses.
	 * @param hostName 
	 *			a String representing a valid internet address
	 * @param portNumber 
	 *			specifies the port that should be used for the connection
	 * @param localHost 
	 *			identifies the local internet address - this is 
	 *			useful if one client has more than 1 ip addresses
	 * @exception UnknownHostException 
	 *			if it is not possible to set or get the local or remote 
	 *			address
	 * @exception SocketException
	 *			if some errors occur while binding sockets
	 */
	JAPRoundTripTime(String hostName, int portNumber, String localHost) 
						throws UnknownHostException,  SocketException {
		try {
			myAddress = InetAddress.getByName(localHost);
			//DEBUG*/System.out.println(myAddress.getHostName());
		} catch (UnknownHostException e) {
			JAPDebug.out(JAPDebug.WARNING ,JAPDebug.NET , "Could not get local host address, are you "
									+" connected to the Internet?.");
			throw (new UnknownHostException (e.getMessage() 
									+ ": Could not get local host address, "
									+ "are you connected to the Internet?."));
		}
		changeHost(hostName, portNumber);
		//DEBUG*/System.out.println("Anfrage an folgende Adresse: " + hostName + ":" + (((sendbuf[16] & 0xff) << 8) + (sendbuf[17] & 0xff)));

		// Configuring Header, Address & Port Number
		for (int i = 4; i <= 11; i ++){
			sendbuf[i] = (byte) 0;
		}
		sendbuf[0] = (byte) (1 << 7);
		//DEBUG*/System.out.println(Integer.toHexString(sendbuf[0] & 0xff) + " " + Integer.toHexString(sendbuf[1] & 0xff));
		for (int i = 0; i <= 3; i ++) {
			sendbuf[12 + i] = myAddress.getAddress()[0 + i];
			//DEBUG*/System.out.println("." + (((byte) myAddress.getAddress()[0 + i]) & 0xff));
		}
	}
	
	/**
	 * Set the remote hostname & port
	 */
	private void changeHost(String hostName, int port) 
								throws UnknownHostException, SocketException {
		changeHost(hostName);
		changePort(port);
	}
	
	/**
	 * Set the remote hostname
	 */
	private void changeHost(String hostName) throws UnknownHostException {
		try {
			address = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			throw(new UnknownHostException(e.getMessage() + "Remote host '" 
							+ hostName + "' could not be reached. Server may be down."));
		}
	}
	
	/**
	 * Set the remote port number
	 */
	private void changePort(int portNumber) throws SocketException {
		this.port = portNumber;
		sendbuf[16] = (byte) ((port) >>> 8);
		sendbuf[17] = (byte) (port);
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			throw (new SocketException(e.getMessage() 
					+ ": Could not locally bind to Socket number: " + port));
		}
		try {
			socket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) {
			System.out.println ("WARNING: " + e.getMessage() 
								+ ": Could not set Socket Timeout");
		}
	}
	
	/**
	 * Calculates the time between the sending and receiving of the
	 * data. Only one packet should be send in a time.
	 * 
	 * @return Array of int with the dimension of MAX_STATIONS
	 */
	public synchronized int[] getRoundTripTime() 
											throws IOException, BindException {
		/** The results for each station in milliseconds */
		int[] result      = new int[MAX_STATIONS];
			
		/** The results for each station in milliseconds sortet by station with station 1 leading */
		int[] finalResult = new int[MAX_STATIONS];
			
		long  preresult = 0;
		int   firstStationNumber = 0;
		Date sendDate;
		Date receiveDate;

		// set senbuf data section & receivebuf to zero.
		for (int i = 4; i <= 11; i++){
			sendbuf[i] = (byte) 0;
		}
		for (int i = 0; i < receivebuf.length; i++){
			receivebuf[i] = (byte) 0;
		}
			
		// get local time an write the packet format to sendbuf
		sendDate = new Date(System.currentTimeMillis());
		for (int i = 0; i <= 7; i ++) {
			sendbuf[4 + i] = (byte) (sendDate.getTime() >>> ((7 - i) * 8));
			//DEBUG*/System.out.println("Byte\t" + (4 + i) + "\t" + sendbuf[4 + i]);
		}
		//DEBUG*/System.out.println(sendDate.getTime());

		try {	
			// Sending the Packet away
			sendPacket = new DatagramPacket(sendbuf,sendbuf.length, 
										address, port);
			socket.send(sendPacket);
			//DEBUG*/System.out.print("Paket wurde gesendet ... ");
			
			// receiving the anwer
			receivePacket = new DatagramPacket(receivebuf, receivebuf.length);
			socket.receive(receivePacket);
			//DEBUG*/System.out.println("Rueckantwort erhalten");
			
			// get local time again an calculate a result.
			receiveDate = new Date(System.currentTimeMillis());
			//DEBUG*/System.out.println("Zeitdifferenz: " + (receiveDate.getTime() - sendDate.getTime()));

			// close the connection
			socket.close();
		} catch (IOException e) {
			throw new IOException(e.getMessage()); 
		} finally {
			socket.close();
		}
		
		// getting out the times in ms of the receipt
		for (int i = 0; i < MAX_STATIONS; i++) {
			preresult = 0;
			for (int j = 0; j < 8; j++) {
					preresult = preresult | 
								(((long)receivebuf[4 + i*8 + j]) & 0xff) 
								  << ((7 - j) * 8);
			}
			if (preresult == sendDate.getTime()) {
				firstStationNumber = i;
				result[i] = (int) ((receiveDate.getTime() 
								   - sendDate.getTime()));
			} else {
				result[i] = (int) (preresult);
			}
		}
		
		finalResult = new int[firstStationNumber+1];
		// Sorting the results by stations.
		for (int i=firstStationNumber; i >= 0; i--) {
			finalResult[firstStationNumber-i] = result [i];
		}
		// Calculate the finalResult except the last result.
		for (int i = 0; i <= (firstStationNumber - 1); i++) {
			finalResult[i] = finalResult[i] - finalResult[i+1];
			if (finalResult[i] < 0) finalResult[i] = 0;
		}
		for (int i = 0; i <= firstStationNumber; i++) {
			finalResult[i] = finalResult[i] / 2;
		}
		
		return finalResult;
	}
	
	
	/** Gets all available local Addresses 
	 * 
	 * @return The String Array has 2 Elements which are: 1st local reply address, 2nd amount of mixes in the chain.
	 */
	public synchronized static String[] getLocalReplyAddress (String remoteAddress, int portNumber) {
		
		String[] result                    = new String[2];
		String[] allMyAddressesStringArray = new String[1];
		InetAddress[] allMyAddresses;
		allMyAddressesStringArray[0]       = "ERROR";
		result[0] = "localhost";
		result[1] = "0";
		
		try {
			int soManyAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName()).length;
			allMyAddressesStringArray = new String[soManyAddresses];
			allMyAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			JAPDebug.out(JAPDebug.INFO ,JAPDebug.NET, "all local addresses:");
			for (int i = 0; i < soManyAddresses; i++){
				allMyAddressesStringArray[i] = allMyAddresses[i].getHostAddress();
				JAPDebug.out(JAPDebug.INFO ,JAPDebug.NET, (i + 1) + ". address " + allMyAddresses[i].getHostAddress());
			}
		} catch (IOException e) {
			System.out.println("Your local IP-Adress could not be found. Are you connected to the Internet?");
			e.printStackTrace();
			allMyAddressesStringArray = new String[1];
			allMyAddressesStringArray[0] = "localhost";
		}
		
		
		
		// Anfrage an den Server senden und die Addresse zurückgeben, die im gleichen Netzwerk liegt.
		for (int i = 0; i < allMyAddressesStringArray.length; i++) {
			try {
				JAPRoundTripTime rttTest = new JAPRoundTripTime(remoteAddress, portNumber, allMyAddressesStringArray[i]);
				int[] times = rttTest.getRoundTripTime();
				result[0] = allMyAddressesStringArray[i];
				result[1] = Integer.toString(times.length);
				JAPDebug.out(JAPDebug.INFO ,JAPDebug.NET, "\n" + allMyAddressesStringArray[i]);
				return result;
			} catch (IOException e) {
				JAPDebug.out(JAPDebug.ERR ,JAPDebug.NET, e.getMessage() + "\n Fehler bei Versuch der Adresse " + allMyAddressesStringArray[i]);
			} finally {
				result[0] = "localhost";
				result[1] = "0";
			}
		}
		result[0] = "localhost";
		result[1] = "0";
		return result;
	}
}

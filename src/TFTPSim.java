
// TFTPSim.java
// This class is the beginnings of an error simulator for a simple TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  Upon receiving a response, it passes it on to the 
// client.
// One socket (23) is used to receive from the client, and another to send/receive
// from the server.  A new socket is used for each communication back to the client.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim extends TFTPFunctions {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private static long delayTime = 0;
	private static int packetNumber = 0;
	private static boolean verboseMode = true; // false for quiet and true for verbose
	private static Type packetType;
	private int clientPort, serverPort = 69;
	private InetAddress clientAddress;
	private static InetAddress serverAddress = null;
	static String ipAddress = "";

	public static enum Type {
		RRQ, WRQ, REQ, DATA, ACK
	};

	public static enum Mode { // error simulation modes
		NORMAL, LOSS, DELAY, DUPLICATE, ERROR4, ERROR5
	};

	public static enum Err4Mode { // options for simulating error 4
		OPCODE, BLOCKNUM, FILENAME, MODE
	};

	private static Mode mode = Mode.NORMAL;
	private static Err4Mode err4Mode;

	public TFTPSim() {
		try {
			// Construct a datagram socket and bind it to port 23
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(23);
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets from the server.
			sendReceiveSocket = new DatagramSocket();
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void delayMode() {
		try {
			System.out.println("------------------------------------------------------\nDelaying " + packetType
					+ " packet number " + packetNumber + "...");
			System.out.println("------------------------------------------------------");
			Thread.sleep(delayTime * 1000); // put this thread to sleep to delay the transfer to the server
		} catch (InterruptedException e) {
			// TODO Auto-generated catch packetCount
			e.printStackTrace();
		}
	}
	
	public void duplicateMode(byte[] data, int len, InetAddress address, int port, String src, String dest, int packetCount, Type receivedType) {
		sendPacket = new DatagramPacket(data, len, address, port);
		len = sendPacket.getLength();
		System.out.println("\nSimulator: sending duplicate packet from " + src + " to " + dest);
		if (verboseMode) {
			System.out.println("Packet number: " + packetCount);
			System.out.println("Packet type: " + receivedType);
			verboseMode(sendPacket.getAddress(), sendPacket.getPort(), len, data);
		}

		if (src.equals("server")) sendPacketFromSocket(sendSocket, sendPacket);
		else sendPacketFromSocket(sendReceiveSocket, sendPacket);
		// Send the duplicate datagram packet to the server via the send/receive socket.
		
	}
	
	public void lossMode(DatagramSocket socket, Type receivedType, boolean isClient) {
		System.out.println("------------------------------------------------------\nLosing " + packetType
				+ " packet number " + packetNumber + "...");
		System.out.println("------------------------------------------------------");

		// Wait to receive the loss packet again
		// This is to keep everything in sync (receiving and send from the right sockets)
		byte[] data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		System.out.println("Simulator: Waiting for packet.");
		receivePacketFromSocket(socket, receivePacket);

		// Process the received datagram.
		int len = receivePacket.getLength();
		if (isClient) {
			clientPort = receivePacket.getPort();
			clientAddress = receivePacket.getAddress();
		}
		else serverPort = receivePacket.getPort();
		if (verboseMode) {
			System.out.println("Simulator: Packet received:");
			verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
		}

		clientAddress = receivePacket.getAddress();
	}
	
	public Type getType(byte[] data) {
		Type type = null;
		if (data[1] == 1)
			type = Type.RRQ;
		if (data[1] == 2)
			type = Type.WRQ;
		if (data[1] == 3)
			type = Type.DATA;
		if (data[1] == 4)
			type = Type.ACK;
		return type;
	}
	
	public void passOnTFTP() throws IOException {
		byte[] data;

		//int clientPort, serverPort = 69,
		int j = 0, len, packetCount = 0;
		//InetAddress clientAdress;
int k=0;
		for (;;) { // loop forever

			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("Simulator: Waiting for packet.");
			receivePacketFromSocket(receiveSocket, receivePacket);

			// Process the received datagram.
			len = receivePacket.getLength();
			if(k==0)
			clientPort = receivePacket.getPort();
			
			if (verboseMode) {
				System.out.println("Simulator: Packet received:");
				verboseMode(receivePacket.getAddress(), clientPort, len, data);
			}

			if (k==0)
			clientAddress = receivePacket.getAddress();

			
			// Save the type of the received packet
			Type receivedType = getType(data);

			if (packetType == receivedType)
				packetCount++;

			// Perform delayed packet if selected
			// the packet type matches the one chose in configurations
			if (mode == Mode.DELAY && packetCount == packetNumber && receivedType == packetType) {
				delayMode();
			} else if (mode == Mode.DUPLICATE && packetCount == packetNumber && receivedType == packetType) {
				duplicateMode(data, len, serverAddress, serverPort, "client", "server", packetCount, receivedType);
			} else if (mode == Mode.LOSS && packetCount == packetNumber && receivedType == packetType) {
				lossMode(receiveSocket, receivedType, true);
				receivedType = getType(data);
				if (packetType == receivedType)
					packetCount++;
			}

			try {
				Thread.sleep(15);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Construct a datagram packet that is to be sent to a specified port
			// on a specified host.
			if ((mode == Mode.ERROR4 && packetCount == packetNumber && receivedType == packetType)
					|| (mode == Mode.ERROR4 && (err4Mode == Err4Mode.FILENAME || err4Mode == Err4Mode.MODE))) {
				sendPacket = error4Packet(data, serverPort, serverAddress, len);
				mode = Mode.NORMAL;
			} else {
				sendPacket = new DatagramPacket(data, len, serverAddress, serverPort);
				// Test an 'unknown' TID
				if (mode == Mode.ERROR5 && packetCount == packetNumber && receivedType == packetType) {
					System.out.println("\nSimulator:  sending a packet with an 'unknown' TID from client to server.");
					createUnknownTIDTestThread(sendPacket);
				}
			}

			len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("\nSimulator: sending packet.");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), len, data);
			}

			// Send the datagram packet to the server via the send/receive socket.
			sendPacketFromSocket(sendReceiveSocket, sendPacket);

			if (checkIfLastDataPacket(data, len, sendReceiveSocket, sendSocket, clientAddress, clientPort, true)) {
				System.out.println("Received all data packets");
				serverPort = 69;
				packetCount=0;
				configSim();
				continue;
			}
			
			receivedType = getType(data);

			// Construct a DatagramPacket for receiving packets up
			// to 100 bytes long (the length of the byte array).
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("Simulator: Waiting for packet.");
			receivePacketFromSocket(sendReceiveSocket, receivePacket);

			serverPort = receivePacket.getPort();
			len = receivePacket.getLength();
			if (verboseMode) {
				// Process the received datagram.
				System.out.println("Simulator: Packet received:");
				verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
			}
			// Save the type of the received packet
			receivedType = getType(data);
			
			if (packetType == receivedType)
				packetCount++;

			// Perform delayed packet if selected
			// the packet type matches the one chose in configurations
			if (mode == Mode.DELAY && packetCount == packetNumber && receivedType == packetType) {
				delayMode();
			} else if (mode == Mode.DUPLICATE && packetCount == packetNumber + 1 && receivedType == packetType) {
				duplicateMode(data, len, clientAddress, clientPort, "server", "client", packetCount, receivedType);
			} else if (mode == Mode.LOSS && packetCount == packetNumber && receivedType == packetType) {
				lossMode(sendReceiveSocket, receivedType, false);
				// Save the type of the received packet
				receivedType = getType(data);
				if (packetType == receivedType)
					packetCount++;
			}

			if (mode == Mode.ERROR4 && packetCount == packetNumber && receivedType == packetType
					|| (mode == Mode.ERROR4 && (err4Mode == Err4Mode.FILENAME || err4Mode == Err4Mode.MODE))) {
				sendPacket = error4Packet(data, serverPort, serverAddress, len);
				mode = Mode.NORMAL;
			} else {
				// Construct a datagram packet that is to be sent to a specified port
				// on a specified host.
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientAddress, clientPort);
			}

			// Test an 'unknown' TID
			if (mode == Mode.ERROR5 && packetCount == packetNumber && receivedType == packetType) {
				System.out.println("\nSimulator: sending a packet with an 'unknown' TID from server to client.");
				createUnknownTIDTestThread(sendPacket);
			}

			len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("Simulator: Sending packet:");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), len, data);
			}

			sendPacketFromSocket(sendSocket, sendPacket);

			if (checkIfLastDataPacket(data, len, receiveSocket, sendReceiveSocket, serverAddress, serverPort,false)) {
				System.out.println("Received all data packets");
				packetCount=0;
				configSim(); // Re-config SIM and continue to wait for packets.
				continue; 
			}
			
			receivedType = getType(data);

			System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
			System.out.println();
			// We're finished with this socket, so close it.
			// sendSocket.close();
		} // end of loop
	}

	/**
	 * Helper method that checks if we have received the last data packet. If so, a last ACK N for data packet N is first received and then forwarded to either the client or server.
	 * @param data - the data packet to check to see if it's the last one.
	 * @param receiveSocket - the socket that is to receive the last ACK.
	 * @param sendSocket - the socket on which to send the last ACK.
	 * @param addressToSendTo - the address to send the last ACK to.
	 * @param portToSendTo - the port to send the last ACK to.
	 * @return boolean - true if it WAS the last data packet and false otherwise.
	 */
	private boolean checkIfLastDataPacket(byte[] data, int len, DatagramSocket receiveSocket, DatagramSocket sendSocket, InetAddress addressToSendTo, int portToSendTo, boolean isClient) {
		if (data[1] == 3 && sendPacket.getLength() < 516) {
			try {
				Thread.sleep(15);
			} catch (InterruptedException e) {
				
			} 
			// Wait to receive the last ACK.
			data = new byte[516];
			receivePacket = new DatagramPacket(data, len);
			receivePacketFromSocket(receiveSocket, receivePacket);
			System.out.println("Simulator: packet received.");			
			if (verboseMode) {
				verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
			}
			// Forward the last ACK.
			sendPacket = new DatagramPacket(data, len, addressToSendTo, portToSendTo);
			
			//if (isClient) 
				sendPacketFromSocket(sendSocket, sendPacket);
			//else sendPacketFromSocket(sendReceiveSocket, sendPacket);
			
			System.out.println("Simulator: packet sent.");
			if (verboseMode) {
				verboseMode(addressToSendTo, portToSendTo, len, data);
			}
 			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * "Menu" for configuring the settings of client application
	 */
	public static void configSim() {
		Scanner sc = new Scanner(System.in); // scanner for getting user's input
		String input = " ";

		// option to set which test mode
		// loops until valid input (0, 1, 2, 3 or nothing)
		while (!(input.equals("0") || input.equals("1") || input.equals("2") || input.equals("3") || input.equals("4")
				|| input.equals("5") || input.equals(""))) {
			System.out.print("\nEnter:\n0 to run in normal mode\n1 for simulated loss packet");
			System.out.print("\n2 for simulated delayed packet\n3 for simulated duplicate packet");
			System.out.print("\n4 for simulated illegal TFTP operation\n5 for simulated unknown transfer ID");
			System.out.print("\nor nothing to stay in " + mode + " mode: ");
			input = sc.nextLine();
			// setting the mode accordingly
			if (input.equals("0"))
				mode = Mode.NORMAL;
			if (input.equals("1"))
				mode = Mode.LOSS;
			if (input.equals("2"))
				mode = Mode.DELAY;
			if (input.equals("3"))
				mode = Mode.DUPLICATE;
			if (input.equals("4"))
				mode = Mode.ERROR4;
			if (input.equals("5"))
				mode = Mode.ERROR5;
		}
		System.out.println("Running in " + mode + " mode");

		if (mode == Mode.ERROR4) { // sub menu for error 4 options
			while (!(input.equals("0") || input.equals("1") || input.equals("2") || input.equals("3"))) {
				System.out.print(
						"\nEnter:\n0 for error in opcode\n1 for error in block number\n2 for invalid file name (too large or missing)");
				System.out.print("\n3 for invalid request mode: ");
				input = sc.nextLine();
				// setting the mode accordingly
				if (input.equals("0"))
					err4Mode = Err4Mode.OPCODE;
				if (input.equals("1"))
					err4Mode = Err4Mode.BLOCKNUM;
				if (input.equals("2"))
					err4Mode = Err4Mode.FILENAME;
				if (input.equals("3"))
					err4Mode = Err4Mode.MODE;
			}
			System.out.println("Simulating error 4 with " + err4Mode + " issue.");
		}

		if (!(mode == Mode.NORMAL
				|| (mode == Mode.ERROR4 && (err4Mode == Err4Mode.FILENAME || err4Mode == Err4Mode.MODE)))) {
			input = "";
			// User inputs to specify which type of packet to lose/delay/duplicate
			// loops until valid input (0, 1, or 2)
			while (!(input.equals("0") || input.equals("1") || input.equals("2") || input.equals("3"))) {
				System.out.print("\nEnter:\n0 for " + mode + " RRQ packets\n1 for " + mode + " WRQ packets");
				System.out.print("\n2 for DATA packets\nor 3 for " + mode + " ACK packets: ");
				input = sc.nextLine();
				// setting the mode accordingly
				if (input.equals("0"))
					packetType = Type.RRQ;
				if (input.equals("1"))
					packetType = Type.WRQ;
				if (input.equals("2"))
					packetType = Type.DATA;
				if (input.equals("3"))
					packetType = Type.ACK;
			}

			boolean invalid = true; // used to break while loop when the entered input is a valid number

			// User inputs to specify which packet number to lose/delay/duplicate
			while (invalid) {
				// If the request is to duplicate a read or write request then, according to our
				// logic, it can only be the 1st packet.
				if ((packetType == Type.RRQ || packetType == Type.WRQ) && mode == Mode.DUPLICATE) {
					packetNumber = 1;
					invalid = false;
				} else {
					System.out.print(
							"\nWhich " + packetType + " packet to " + mode + " (" + packetType + " packet number): ");
					input = sc.nextLine();
					try {
						packetNumber = Integer.valueOf(input);
						invalid = false;
					} catch (NumberFormatException e) {
						System.out.println("Invalid value entered! Please enter a NUMBER.");
					}
				}
			}

			if (mode == Mode.DELAY) {
				invalid = true; // used to break while loop when the entered input is a valid number
				while (invalid) {
					System.out.println("\nEnter the amount of time to delay packet (seconds): ");
					input = sc.nextLine();
					try {
						delayTime = Long.parseLong(input);
						invalid = false;
					} catch (NumberFormatException e) {
						System.out.println("Invalid value entered! Please enter a NUMBER.");
					}
				}
				System.out.println(packetType + " number " + packetNumber + " packet will be delayed by " + delayTime
						+ " seconds.");
			}
		}

		boolean changeSettings = true;
		if (serverAddress != null) { //not first time configuring
			input = " ";
			System.out.println("\nEnter 1 to change configurations or nothing to leave unchanged: ");
			while (!(input.equals("1") || input.equals(""))) {
				input = sc.nextLine();
				if (input.equals("1")) changeSettings=true;
				else changeSettings = false;
			}
		}
		if (changeSettings) {
			input = " ";
			// option to toggle verbose or quiets mode
			while (!(input.equals("1") || input.equals(""))) { // loops until valid input (1 or nothing)
				System.out.println("\nEnter '1' to toggle between quiet and verbose mode ");
				System.out.print("or nothing to stay in " + (verboseMode ? "verbose" : "quiet") + " mode: ");
				input = sc.nextLine();
				// toggling verboseMode
				if (input.equals("1"))
					verboseMode = verboseMode ? false : true;
			}
			System.out.println("Running in " + (verboseMode ? "verbose" : "quiet") + " mode");
			
			input = ""; // reset input
			System.out.println("\nCurrent IP is: " + (ipAddress.equals("") ? "undefined" : ipAddress));
			// option to set the IP address.
			// User must input IP address at the first launch.
			// Once an IP has been set, the user can enter nothing to keep it unchanged.
			while (input.equals("")) {
				System.out.println("Enter the IP address of server or nothing to keep IP address unchanged: ");
				input = sc.nextLine();
	
				if (input.equals("")) {
					if (ipAddress.equals(""))
						System.out.println("An IP has not been entered yet!");
					else
						input = "entered"; // set input to arbitrary string to leave loop
				} else {
					ipAddress = input;
					System.out.println("IP address is now: " + ipAddress);
				}
			}
			//Set serverAddress
			try {
				serverAddress = InetAddress.getByName(ipAddress);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			System.out.println("\n------------------------------------------------------\nConfigurations are now set up.");
			System.out.println("------------------------------------------------------");
		}
	}

	/**
	 * 
	 * @param packet
	 */
	private void createUnknownTIDTestThread(DatagramPacket packet) {
		Thread unknownTIDThread = new Thread(new unknownTIDTestThread(packet), "Unknown TID Test Thread");
		unknownTIDThread.start();
	}

	/**
	 * Create a new thread so that we can test invalid TIDs
	 */
	private class unknownTIDTestThread implements Runnable {
		private DatagramPacket packet;

		public unknownTIDTestThread(DatagramPacket packet) {
			this.packet = packet;
		}

		public void run() {
			try {
				System.out.println("Simulator: Starting a new thread to simulate an invalid TID.");

				// Create a new socket with a new/different TID.
				DatagramSocket socket = new DatagramSocket();

				// Send the packet using this new TID
				socket.send(packet);

				// New packet to receive the (expected) error from the host.
				byte[] data = new byte[516];
				DatagramPacket errorPacket = new DatagramPacket(data, data.length);

				// Should receive an invalid TID error packet
				socket.receive(errorPacket);

				// TODO verify that it is an error packet and that the error packet has a valid
				// error code???
				if (verboseMode) {
					int len = errorPacket.getLength();
					// Process the received datagram.
					System.out.println("Simulator: Packet received:");
					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
				}
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				System.out.println("UnknownTIDTransferHandler thread terminated.\n");
			}
		}
	}

	/**
	 * 
	 * @param data
	 * @param port
	 * @param clientAdress
	 * @return
	 */
	public DatagramPacket error4Packet(byte[] data, int port, InetAddress clientAdress, int len) {
		DatagramPacket sendPacket = null;

		if (err4Mode == Err4Mode.OPCODE) {
			data[0] = 9;
			data[1] = 9;
			System.out.println(
					"-------------------------------Creating packet with invalid opcode-------------------------------");
			sendPacket = new DatagramPacket(data, len, clientAdress, port);
		}

		if (err4Mode == Err4Mode.FILENAME) {
			byte[] temporary = new byte[len];
			temporary[0] = data[0];
			temporary[1] = data[1];

			int i;
			for (i = 2; i < len; i++) {
				if (data[i] == 0)
					break;
			}
			System.arraycopy(data, i, temporary, 2, len - 2);
			System.out.println(
					"---------------------------------Creating packet with invalid file name---------------------------------");
			sendPacket = new DatagramPacket(temporary, temporary.length, clientAdress, port);
		}

		if (err4Mode == Err4Mode.MODE) {
			byte[] temporary = new byte[len];
			System.arraycopy(data, 0, temporary, 0, len - 6);
			System.arraycopy("pctet".getBytes(), 0, temporary, temporary.length - 6, 5);
			System.out.println(
					"---------------------------------Creating packet with invalid mode---------------------------------");
			sendPacket = new DatagramPacket(temporary, temporary.length, clientAdress, port);
		}

		if (err4Mode == Err4Mode.BLOCKNUM) {
			// Byte 3 is always the second block number byte
			data[3] = (byte) (data[2] + 3); // adding 3 to block number
			System.out.println(
					"---------------------------------Creating packet with out of synch block number---------------------------------");
			sendPacket = new DatagramPacket(data, len, clientAdress, port);
		}
		return sendPacket;
	}

	public static void main(String args[]) throws IOException {
		TFTPSim sim = new TFTPSim();
		System.out.println("Welcome to the TFTP Error Simulation application");
		configSim();
		sim.passOnTFTP();
	}
}
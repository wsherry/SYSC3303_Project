// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPClient extends TFTPFunctions {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	static int connectionPort;
	private static boolean verboseMode = false; // false for quiet and true for verbose
	static String ipAddress = "192.168.0.21";
	private static String clientDirectory = "C:\\Alexei's Stuff\\Carleton University";
	static boolean finishedRequest = false;
	static boolean changeMode = true;
	private boolean running = true;
	private ArrayList<Integer> processedACKBlocks = new ArrayList<>();

	public static enum Mode {
		NORMAL, TEST
	};

	private static Mode run = Mode.NORMAL;

	public static enum RequestType {
		READ, WRITE
	};

	private static RequestType request;

	public TFTPClient() {
		try {
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
			// Set socket timeout to allow for retransmission
			sendReceiveSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Send and receive packets
	 */
	public void sendAndReceive() {
		Scanner sc = new Scanner(System.in);
		// user toggle verbose or quiets mode
		String input = "";
		boolean received = false;
		byte readWrite = (byte) 1; // default value for initialization
		int timeoutCount = 0;

		while (running) {
			// After a request has been completed the user gets prompted
			// to enter the configuration "menu" again
			// TO BE IMPLEMENTED AFTER FILE TRANSFER IMPLEMENTATION IS COMPLETED
			if (finishedRequest) { // finishedRequest should be true after a file has been fully read or written
				System.out.println("\nEnter 1 to change configurations or nothing to leave unchanged: ");
				while (!(input.equals("1") || input.equals(""))) {
					input = sc.nextLine();
					if (input.equals("1"))
						configClient();
				}
				finishedRequest = false; // set finishedRequest to false so the user only gets prompt after a request
			}

			// NOT CORRECTLY IMPLEMENTED YET.
			// THIS SHOULD ONLY PROMPT USER AFTER A REQUEST HAS FULLY COMPLETED
			// User chooses read or write request
			if (changeMode) {
				input = "";
				while (!(input.equals("1") || input.equals("2") || input.equals("3"))) {
					System.out.println("\nEnter '1' for read or '2' write request or '3' to quit: ");
					input = sc.nextLine();

					if (input.equals("1")) {
						readWrite = (byte) 1;
						request = RequestType.READ;
					} else if (input.equals("2")) {
						readWrite = (byte) 2;
						request = RequestType.WRITE;
					} else if (input.equals("3"))
						running = false;
					else
						System.out.print(input + " is not 1 or 2 or .\n");
				}
				changeMode = false;
			}

			if (!running)
				break; // break out of loop if user choose to quit
			// INCOMPLETED, need to implement error detection for checking if file exists
			input = " ";
			String fileName = "";
			// while (found){ //keep asking for file until valid file is found in directory
			System.out.println("\nEnter the name of the file: ");
			fileName = sc.nextLine();
			// TODO CHECK IF FILE EXISTS
			// }

			byte[] msg = new byte[100], // message we send
					fn, // filename as an array of bytes
					md, // mode as an array of bytes
					data; // reply as array of bytes
			String mode; // filename and mode as Strings
			int j, len, sendPort;

			if (run == Mode.NORMAL)
				sendPort = 69;
			else
				sendPort = 23;
			System.out.println("Client: creating packet.");

			// Prepare a DatagramPacket and send it via sendReceiveSocket
			// to sendPort on the destination host (also on this machine).

			msg[0] = 0;
			msg[1] = readWrite;
			// convert to bytes
			fn = fileName.getBytes();

			// and copy into the msg
			System.arraycopy(fn, 0, msg, 2, fn.length);

			// now add a 0 byte
			msg[fn.length + 2] = 0;
			mode = "octet";
			// convert to bytes
			md = mode.getBytes();

			// and copy into the msg
			System.arraycopy(md, 0, msg, fn.length + 3, md.length);

			len = fn.length + md.length + 4; // length of the message
			// length of filename + length of mode + opcode (2) + two 0s (2)
			// end with another 0 byte
			msg[len - 1] = 0;

			try {
				sendPacket = new DatagramPacket(msg, len, InetAddress.getByName(ipAddress), sendPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}

			len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("Client: sending packet.");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), len, msg);
			}

			// Form a String from the byte array, and print the string.
			String sending = new String(msg, 0, len);
			System.out.println(sending);

			// Append the directory to the beginning of the file
			fileName = clientDirectory + "//" + fileName;

			// Send the datagram packet to the server via the send/receive socket.
			received = false;// reset flag for receiving packet
			while (!received) {// set to true when client receives response to read/write request
				sendPacketFromSocket(sendReceiveSocket, sendPacket);

				System.out.println("Client: Packet sent.");

				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array).
				data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);

				if (request == RequestType.READ) {
					boolean testMode = false;
					if (run == Mode.TEST)
						testMode = true;
					receiveFiles(fileName, sendPort, "Client", sendReceiveSocket, testMode, true, verboseMode,
							connectionPort);
					if (finishedRequest) {
						break;
					}
				} else {
					System.out.println("Client: Waiting for packet.");
					try {
						// Block until a datagram is received via sendReceiveSocket.
						sendReceiveSocket.receive(receivePacket);
					} catch (InterruptedIOException ie) {
						timeoutCount++;
						if(timeoutCount == MAX_TIMEOUT) {
							System.out.println("Maximum timeouts occurred without response. Terminating transfer.");
							finishedRequest = true;
							changeMode = true;
							break;
						}
						System.out.println("Client Timed out. Resending packet.");
						continue;
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					connectionPort = receivePacket.getPort();
					received = true;

					// Process the received datagram.
					if (run != Mode.TEST)
						sendPort = receivePacket.getPort();

					len = receivePacket.getLength();

					if (verboseMode) {
						System.out.println("Client: Packet received:");
						verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
					}

					boolean ackVerified = false;
					byte[] readAck = new byte[] { 0, 3, 0, 1 };
					byte[] writeAck = new byte[] { 0, 4, 0, 0 };
					if (request == RequestType.READ) {
						ackVerified = Arrays.equals(readAck, data);
					} else {
						ackVerified = Arrays.equals(writeAck, data);
					}

					processedACKBlocks.add(data[2] * 10 + data[3]); // A this point it should be ACK 0.
					transferFiles(sendReceiveSocket, receivePacket, "Client", fileName, sendPort, processedACKBlocks,
							verboseMode);
				}
			}
		}
		System.out.println("Client is off");
		// We're finished, so close the socket.
		sendReceiveSocket.close();
	}

	/**
	 * "Menu" for configuring the settings of client application
	 */
	public static void configClient() {
		Scanner sc = new Scanner(System.in); // scanner for getting user's input
		String input = " ";

		// option to set normal or test mode
		while (!(input.equals("1") || input.equals("2") || input.equals(""))) { // loops until valid input (1, 2 or
																				// nothing)
			System.out.println("\nEnter '1' to run in normal mode or '2' for test mode ");
			System.out.print("or nothing to stay in " + (run == Mode.NORMAL ? "normal" : "test") + " mode: ");
			input = sc.nextLine();
			// setting the mode accordingly
			if (input.equals("1"))
				run = Mode.NORMAL;
			if (input.equals("2"))
				run = Mode.TEST;
		}
		System.out.println("Running in " + (run == Mode.NORMAL ? "normal" : "test") + " mode");

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

		//don't ask for IP address if running in test mode
		//the error simulator will ask for the IP address since it's sending packets to the server.
		if (run != Mode.TEST) {
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
		} else { //set ipAdress to local ip address
			try {
				ipAddress = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		input = "";
		System.out.println(
				"\nCurrent client directory is: " + (clientDirectory.equals("") ? "undefined" : clientDirectory));
		// option to set the file directory.
		// User must input file directory at the first launch.
		// Once a file directory has been set, the user can enter nothing to keep it
		// unchanged.
		while (input.equals("")) {
			System.out.println("Enter the client of directory or nothing to keep the directory unchanged: ");
			input = sc.nextLine();

			if (input.equals("")) {
				if (clientDirectory.equals(""))
					System.out.println("A client directory has not been entered yet!");
				else
					input = "entered"; // set input to arbitrary string to leave loop
			} else {
				clientDirectory = input;
				System.out.println("Client directory is now: " + clientDirectory);
			}
		}

		System.out.println("\n------------------------------------------------------\nConfigurations are now set up.");
		System.out.println("------------------------------------------------------");
	}

	public static void main(String args[]) {
		TFTPClient c = new TFTPClient();
		System.out.println("Welcome to the TFTP client application");
		configClient();
		c.sendAndReceive();
	}
}
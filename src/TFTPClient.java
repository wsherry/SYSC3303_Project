// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPClient {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private static boolean verboseMode = false; // false for quiet and true for verbose
	private static String ipAddress = "192.168.1.32";
	private static String clientDirectory = "C:\\Alexei's Stuff\\Carleton University";
	private static boolean finishedRequest = false;
	private static boolean changeMode = true;
	private boolean running = true;
	private static final int TIMEOUT = 3000; //Delay for timeout when waiting to receive file 
	private ArrayList<Integer> processedACKBlocks = new ArrayList<>();
	
	// we can run in normal (send directly to server) or test
	// (send to simulator) mode
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
			//Set socket timeout to allow for retransmission
			sendReceiveSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		Scanner sc = new Scanner(System.in);
		// user toggle verbose or quiets mode
		String input = "";
		boolean received = false;

		byte readWrite = (byte) 1; // default value for initialization

		while (running) {
			// After a request has been completed the user gets prompted
			// to enter the configuration "menu" again
			// TO BE IMPLEMENTED AFTER FILE TRANSFER IMPLEMENTATION IS COMPLETED
			if (finishedRequest) { // finishedRequest should be true after a file has been fully read or written
				System.out.println("Enter 1 to change configurations or nothing to leave unchanged: ");
				while (!(input.equals("1") || input.equals(""))) {
					input = sc.nextLine();
					if (input.equals("1"))
						configClient();
				}
				finishedRequest = false; // set finishedRequest to false so the user only gets prompt after a request
											// finishes
			}

			// NOT CORRECTLY IMPLEMENTED YET.
			// THIS SHOULD ONLY PROMPT USER AFTER A REQUEST HAS FULLY COMPLETED
			// User chooses read or write request
			if (changeMode) {
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
				changeMode=false;
			}

			if (!running)
				break; // break out of loop if user choose to quit
			// INCOMPLETED, need to implement error detection for checking if file exists
			input = " ";
			String fileName = "";
			// while (found){ //keep asking for file until valid file is found in directory
			System.out.println("\nEnter the name of the file: ");
			fileName = sc.nextLine();
			// CHECK IF FILE EXISTS
			// }

			byte[] msg = new byte[100], // message we send
					fn, // filename as an array of bytes
					md, // mode as an array of bytes
					data; // reply as array of bytes
			String mode; // filename and mode as Strings
			int j, len, sendPort;

			// that test vs. normal will be entered by the user.

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
			// format is: source array, source index, dest array,
			// dest index, # array elements to copy
			// i.e. copy fn from 0 to fn.length to msg, starting at
			// index 2

			// now add a 0 byte
			msg[fn.length + 2] = 0;

			// now add "octet" (or "netascii")
			mode = "octet";
			// convert to bytes
			md = mode.getBytes();

			// and copy into the msg
			System.arraycopy(md, 0, msg, fn.length + 3, md.length);

			len = fn.length + md.length + 4; // length of the message
			// length of filename + length of mode + opcode (2) + two 0s (2)
			// second 0 to be added next:

			// end with another 0 byte
			msg[len - 1] = 0;

			// Construct a datagram packet that is to be sent to a specified port
			// on a specified host.
			// The arguments are:
			// msg - the message contained in the packet (the byte array)
			// the length we care about - k+1
			// InetAddress.getLocalHost() - the Internet address of the
			// destination host.
			// In this example, we want the destination to be the same as
			// the source (i.e., we want to run the client and server on the
			// same computer). InetAddress.getLocalHost() returns the Internet
			// address of the local host.
			// 69 - the destination port number on the destination host.
			try {

				//sendPacket = new DatagramPacket(msg, len, InetAddress.getLocalHost(), sendPort);
				// */
				sendPacket = new DatagramPacket(msg, len, InetAddress.getByName(ipAddress),

				 sendPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}

			len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("Client: sending packet.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				for (j = 0; j < len; j++) {
					System.out.println("byte " + j + " " + msg[j]);
				}
			} else {
				System.out.println("Client: sending packet.");
			}

			// Form a String from the byte array, and print the string.
			String sending = new String(msg, 0, len);
			System.out.println(sending);

			//Append the directory to the beginning of the file
			fileName = clientDirectory + "//" + fileName;
			
			// Send the datagram packet to the server via the send/receive socket.
			received = false;//reset flag for receiving packet
			while(!received){//set to true when client receives response to read/write request
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
	
				System.out.println("Client: Packet sent.");
	
				// Construct a DatagramPacket for receiving packets up
				// to 100 bytes long (the length of the byte array).
	
				data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				
				if (request != RequestType.READ) {
					System.out.println("Client: Waiting for packet.");
					try {
						// Block until a datagram is received via sendReceiveSocket.
						sendReceiveSocket.receive(receivePacket);
					} catch(InterruptedIOException ie) {
						System.out.println("Client Timed out. Resending packet.");
						continue;
					}catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					received = true;
					
					// Process the received datagram.
					if (run != Mode.TEST) sendPort = receivePacket.getPort();
					len = receivePacket.getLength();
					if (verboseMode) {
						System.out.println("Client: Packet received:");
						System.out.println("From host: " + receivePacket.getAddress());
						System.out.println("Host port: " + receivePacket.getPort());
						System.out.println("Length: " + len);
						System.out.println("Containing: ");
						for (j = 0; j < len; j++) {
							System.out.println("byte " + j + " " + data[j]);
						}
					} else {
						System.out.println("Client: Packet received.");
					}
	
					boolean ackVerified = false;
					byte[] readAck = new byte[] { 0, 3, 0, 1 };
					byte[] writeAck = new byte[] { 0, 4, 0, 0 };
					if (request == RequestType.READ) {
						ackVerified = Arrays.equals(readAck, data);
					} else {
						ackVerified = Arrays.equals(writeAck, data);
					}
	
					//if (!ackVerified) // re-send request
					//if (request == RequestType.WRITE) {
						processedACKBlocks.add(data[2]*10+data[3]);	// Add this point it should be ACK 0.
						transferFiles(fileName, sendPort);
					//}
				} else {
					receiveFiles(fileName);
					if(finishedRequest) {
						break;
					}
				}
				System.out.println();
			}
		}
		System.out.println("Client is off");
		// We're finished, so close the socket.
		sendReceiveSocket.close();
	}

	/*
	 * public void receiveFiles(String fileName, int sendPort) { byte[] data = new
	 * byte[516]; int len = 516; receivePacket = new DatagramPacket(data,
	 * data.length); System.out.println("Client: Waiting for data packet."); while
	 * (len == 516) { try { // Block until a datagram is received via
	 * sendReceiveSocket. sendReceiveSocket.receive(receivePacket); } catch
	 * (IOException e) { e.printStackTrace(); System.exit(1); } // Process the
	 * received datagram. len = receivePacket.getLength();
	 * System.out.println("Client: Data Packet received."); if (verboseMode) {
	 * System.out.println("From host: " + receivePacket.getAddress());
	 * System.out.println("Host port: " + receivePacket.getPort());
	 * System.out.println("Length: " + len); System.out.println("Containing: "); for
	 * (int j = 0; j < len; j++) { System.out.println("byte " + j + " " + data[j]);
	 * } } byte[] ack = new byte[] { 0, 4, 0, 0 }; try { sendPacket = new
	 * DatagramPacket(ack, ack.length, InetAddress.getByName(ipAddress), sendPort);
	 * } catch (UnknownHostException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); } try { sendReceiveSocket.send(sendPacket); } catch
	 * (IOException e) { e.printStackTrace(); System.exit(1); } } }
	 */

	/**
	 * Receives for read request
	 * @param fileName
	 */
	public void receiveFiles(String fileName) {
		ArrayList<Integer> processedBlocks = new ArrayList<>();
		
		//used to differentiate between read request response and regular file transfer
		boolean requestResponse = true;
		int sendPort = -1; 
		if (run == Mode.TEST) sendPort = 23;
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

			while (true) {
				byte[] data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				int len = receivePacket.getLength();

				System.out.println("Client: Waiting for data packet.");
				if(requestResponse) {
					try {
						// Block until a datagram is received via sendReceiveSocket.
						sendReceiveSocket.receive(receivePacket);
						if (run != Mode.TEST) sendPort = receivePacket.getPort();
						requestResponse = false;
					}catch(InterruptedIOException io) {
						System.out.println("Client timed out. resending request.");
						finishedRequest = true;
						changeMode = true;
						break;
					}catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				} else {
					try {
						// Block until a datagram is received via sendReceiveSocket.
						sendReceiveSocket.setSoTimeout(300000);
						sendReceiveSocket.receive(receivePacket);
						sendReceiveSocket.setSoTimeout(TIMEOUT);
					}catch(InterruptedIOException io) {
						System.out.println("Client has exceeded idle time. Cancelling transfer.");
					}catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
				// Process the received datagram.
				len = receivePacket.getLength();
				data = receivePacket.getData();

				System.out.println("Client: Data Packet received.");

				// Check if it's a duplicate packet. If it is, we still want to send an ACK but not rewrite to the file.
				if (!processedBlocks.contains(data[2]*10+data[3])) {
					// This block number has not been processed. Write it to the file.
					try {
						out.write(data, 4, len - 4);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					processedBlocks.add(data[2]*10+data[3]);
				} else {					
					if (verboseMode) {		
						System.out.println("Client: Duplicate data packet received. Ignoring it by not writing it again.");	
						// TODO We should send an Nth ACK for the Nth duplicate data packet that was received.
					}		
				}		

				if (verboseMode) {
					System.out.println("From host: " + receivePacket.getAddress());
					System.out.println("Host port: " + receivePacket.getPort());
					System.out.println("Length: " + len);
					System.out.println("Containing: ");
					for (int j = 0; j < len; j++) {
						System.out.println("byte " + j + " " + data[j]);
					}
				}

				byte[] ack = new byte[] { 0, 4, data[2], data[3]};

				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getByName(ipAddress), sendPort);

				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				if (verboseMode) {
					System.out.println("Client: Sending ACK packet:");
					System.out.println("To host: " + receivePacket.getAddress());
					System.out.println("Destination host port: " + receivePacket.getPort());
					System.out.println("Length: " + sendPacket.getLength());
					System.out.println("Containing: ");
					for (int j = 0; j < sendPacket.getLength(); j++) {
						System.out.println("byte " + j + " " + ack[j]);
					}
				} else {
					System.out.println("Client: ACK Packet sent.");
				}
				if (len < 516) {
					System.out.println("Received all data packets");
					finishedRequest = true;
					changeMode = true;
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filename
	 * @param sendPort
	 */
	public void transferFiles(String filename, int sendPort) {
		int blockNum = 1; // Data blocks start at one.
		byte[] data = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);

		ArrayList<byte[]> msgBuffer = readFileIntoBlocks(filename);
		
		for (int i = 0; i < msgBuffer.size(); i++) {
			byte[] msg = msgBuffer.get(i);
			msg[0] = 0;
			msg[1] = 3;
			msg[2] = blockNumBytes(blockNum)[0];
			msg[3] = blockNumBytes(blockNum)[1];

			try {
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(ipAddress), sendPort);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Data Packet sent.");

			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch(InterruptedIOException ie) {
				System.out.println("Client Timed out. Resending packet.");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// Check if the received packet is a duplicate ACK. If it is, then we should not be re-sending the Nth data packet for the ACK. Sorcerer's Apprentice Bug. 	
			if (!processedACKBlocks.contains(data[2]*10+data[3])) {		
				processedACKBlocks.add(data[2]*10+data[3]);		
			}  else {		
				if (verboseMode) {		
					System.out.println("Client: Duplicate ACK data packet received. Ignoring it by not re-sending data block  number [" + data[2]*10+data[3] + "] and waiting for the next datablock.");		
				}		
			}

			int len = receivePacket.getLength();
			if (verboseMode) {
				System.out.println("Client: Packet received:");
				System.out.println("Block number: " + receivePacket.getData()[2] + receivePacket.getData()[3]);
				System.out.println("From host: " + receivePacket.getAddress());
				System.out.println("Host port: " + receivePacket.getPort());
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				for (int j = 0; j < len; j++) {
					System.out.println("byte " + j + " " + data[j]);
				}
			} else {
				System.out.println("Client: Packet received.");
			}
			blockNum++;

			if (sendPacket.getLength() < 516) {
				System.out.println("Client: Last packet sent.");
				finishedRequest = true;
				changeMode = true;
			}

		}
	}

	/**
	 * Converts the blocknumber as an int into a 2 byte array
	 * @param blockNum
	 * @return
	 */
	private byte[] blockNumBytes(int blockNum) {
		byte[] blockNumArray = new byte[2];

		// create the corresponding block number in 2 bytes
		byte block1 = (byte) (blockNum / 10);
		byte block2 = (byte) (blockNum % 10);
		blockNumArray[0] = block1;
		blockNumArray[1] = block2;
		return blockNumArray;

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

		System.out.println("\n------------------------------------------------------\nConfigerations are now set up.");
		System.out.println("------------------------------------------------------");
	}

	public static void main(String args[]) {
		TFTPClient c = new TFTPClient();
		System.out.println("Welcome to the TFTP client application");
		configClient();
		c.sendAndReceive();
	}
	
	private ArrayList<byte[]> readFileIntoBlocks(String fileName) {
		ArrayList<byte[]> msgList = new ArrayList<>();
		byte[] dataBuffer = new byte[512];
		BufferedInputStream bis;
		int bytesRead;
		try {
			bis = new BufferedInputStream(new FileInputStream(fileName));	
			while ((bytesRead = bis.read(dataBuffer, 0, 512)) != -1) {
				byte[] msg = new byte[bytesRead + 4];	
				System.arraycopy(dataBuffer, 0, msg, 4, bytesRead);
				msgList.add(msg);
			}
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return msgList;
	}
}
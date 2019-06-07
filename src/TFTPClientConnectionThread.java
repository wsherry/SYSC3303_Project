import java.io.*;
import java.net.*;
import java.util.*;

//import TFTPClient.RequestType;

public class TFTPClientConnectionThread extends TFTPFunctions implements Runnable {

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private int connectionPort;
	private boolean verboseMode = false; // false for quiet and true for verbose
	private Request request;
	// responses for valid requests
	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };

	String fileName;
	static boolean doneProcessingRequest = true;

	// List to keep track of whom the server is already talking to.
	// Used to handle duplicate packets from the same source.
	static ArrayList<String> establishedCommunications = new ArrayList<String>();
	private ArrayList<Integer> processedACKBlocks = new ArrayList<>();

	private String serverDirectory;

	public TFTPClientConnectionThread(boolean verboseMode, String serverDirectory) {
		try {
			// Construct a datagram socket and bind it to port 69
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		this.verboseMode = verboseMode;
		this.serverDirectory = serverDirectory;
	}
	
	/**
	 * Closes the receive socket
	 */
	public void closeSocket() {
		receiveSocket.close();
	}

	public void checkRequestForError(int len, byte[] data) {
		int j = 0, k = 0;
		if (request != Request.ERROR) { // check for filename
			// search for next all 0 byte
			for (j = 2; j < len; j++) {
				if (data[j] == 0)
					break;
			}
			if (j == len || j == 2)
				request = Request.ERROR; // didn't find a 0 byte or filename is 0 bytes long
		}

		if (request != Request.ERROR) { // check for mode
			// search for next all 0 byte
			for (k = j + 1; k < len; k++) {
				if (data[k] == 0)
					break;
			}
			if (k == len || k == j + 1 || k != len - 1)
				request = Request.ERROR; // didn't find a 0 byte or mode is 0 bytes long or other stuff at end of packet
		}
	}

	public void run() {
		int len;

		while (true) { // loop forever

			if (doneProcessingRequest) {
				byte[] data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("------------------------------------------------------");
				System.out.println("Type 'quit' to shutdown.");
				try {
					System.out.println(
							"Server (" + InetAddress.getLocalHost().getHostAddress() + ") : Waiting for packet.");
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}

				// Block until a datagram packet is received from receiveSocket.
				try {
					receiveSocket.setSoTimeout(0);
					receiveSocket.receive(receivePacket);
				} catch (SocketException exception) { // the socket has been closed for server shut down
					System.out.println("Server is off");
					break;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// Process the received datagram.
				len = receivePacket.getLength();
				if (verboseMode) {
					System.out.println("Server: Packet received:");
					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
				}

				// Form a String from the byte array.
				String received = new String(data, 0, len);
				fileName = received.split("\0")[1].substring(1);
				System.out.println(fileName);

				if (data[0] != 0)
					request = Request.ERROR; // bad
				else if (data[1] == 1)
					request = Request.READ; // could be read
				else if (data[1] == 2)
					request = Request.WRITE; // could be write
				else
					request = Request.ERROR; // bad

				// check request for any errors
				checkRequestForError(len, data);

				if (request == Request.ERROR) { // it was invalid, close socket on port 69 (so things work next time)
												// and quit
					receiveSocket.close();
					try {
						throw new Exception("Not yet implemented");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				establishedCommunications.add(receivePacket.getSocketAddress().toString());

				Runnable reqThread = new TFTPsendThread(request, receivePacket, verboseMode);
				new Thread(reqThread).start();

			}
		} // end of loop
	}

	public void sendPacketFromSocket(DatagramSocket socket, DatagramPacket packet) {
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public class TFTPsendThread implements Runnable {

		private DatagramSocket sendReceiveSocket;
		private DatagramPacket receivePacket, sendPacket;
		private boolean verboseMode = false; // false for quiet and true for verbose
		private static final int TIMEOUT = 4000;
		private Request request;

		public TFTPsendThread(Request request, DatagramPacket receivePacket, boolean verboseMode) {
			try {
				sendReceiveSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(0);
			}

			this.receivePacket = receivePacket;
			this.request = request;
			this.verboseMode = verboseMode;
			doneProcessingRequest = false;
			connectionPort = receivePacket.getPort();
		}

		public void run() {
			// Create a response.
			if (request == Request.READ) {
				//transferFiles(serverDirectory + "\\" + fileName, receivePacket.getPort(), receivePacket);
				transferFiles(sendReceiveSocket, receivePacket,"Server", serverDirectory + "\\" + fileName, receivePacket.getPort(), processedACKBlocks, verboseMode);

			} else if (request == Request.WRITE) {
				byte[] response = writeResp;
				send(response);
			}

		}

		private void send(byte[] response) {
			sendPacket = new DatagramPacket(response, response.length, receivePacket.getAddress(),
					receivePacket.getPort());

			int len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("TFTPClientConnectionThread: Sending packet:");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), len, response);
			}

			sendPacketFromSocket(sendReceiveSocket, sendPacket);
			System.out
					.println("TFTPClientConnectionThread: packet sent using port " + sendReceiveSocket.getLocalPort());

			//receiveFiles(fileName, sendReceiveSocket.getLocalPort());
			receiveFiles(fileName, sendReceiveSocket.getLocalPort(), "Server", sendReceiveSocket, true, false, verboseMode, connectionPort);
			// We're finished with this socket, so close it.
			sendReceiveSocket.close();
		}

		/**
		 * write to file
		 * 
		 * @param fileName
		 * @param sendPort
		 */
//		public void receiveFiles(String fileName, int sendPort) {
//			ArrayList<Integer> processedDataBlocks = new ArrayList<>();
//
//			try {
//				// sendReceiveSocket.setSoTimeout(TIMEOUT);
//				BufferedOutputStream out = new BufferedOutputStream(
//						new FileOutputStream(serverDirectory + "\\" + fileName));
//				while (true) {
//					byte[] data = new byte[516];
//					receivePacket = new DatagramPacket(data, data.length);
//					int len = receivePacket.getLength();
//
//					System.out.println("Server: Waiting for data packet.");
//					try {
//						// Block until a datagram is received via sendReceiveSocket, or until
//						// idle for exceptional amount of time
//						sendReceiveSocket.setSoTimeout(300000);
//						sendReceiveSocket.receive(receivePacket);
//						sendReceiveSocket.setSoTimeout(TIMEOUT);
//					} catch (InterruptedIOException ie) {
//						// NOT IMPLEMENTED. Behind current version, might not work with changes
//						System.out.println("Server idle timeout. Closing connection.");
//						break;
//					} catch (IOException e) {
//						e.printStackTrace();
//						System.exit(1);
//					}
//
//					// Check if the received packet is a duplicate write request from a socket and
//					// port that is being handled. If so, ignore the packet and continue waiting.
//					if (establishedCommunications.contains(receivePacket.getSocketAddress().toString())
//							&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 2)) {
//						System.out.println("Server: Received a duplicate WRQ packet. Ignoring it.");
//						continue;
//
//					}
//
//					// Check if packet came from correct source. Send back ERROR packet code 5 if
//					// not.
//					if (connectionPort != receivePacket.getPort()) {
//						System.out.println(
//								"Received Packet from unknown source. Responding with ERROR CODE 5 packet and continuing.");
//						byte[] err = new byte[] { 0, 5, 0, 5 };
//						sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
//								receivePacket.getPort());
//						try {
//							sendReceiveSocket.send(sendPacket);
//						} catch (IOException e) {
//							e.printStackTrace();
//							System.exit(1);
//						}
//						continue;
//					}
//
//					// Process the received datagram.
//					len = receivePacket.getLength();
//					data = receivePacket.getData();
//
//					if (data[1] == 5 && data[3] == 3) {
//						System.out.println("ERROR code 5: ACK Packet sent to wrong port. Waiting for proper DATA.");
//						continue;
//					}
//
//					System.out.println("Server: Data Packet received.");
//
//					// Check if it's a duplicate packet. If it is, we still want to send an ACK but
//					// not rewrite to the file.
//					if (!processedDataBlocks.contains(data[2] * 10 + data[3])) {
//						// This block number has not been processed. Write it to the file.
//						try {
//							out.write(data, 4, len - 4);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						processedDataBlocks.add(data[2] * 10 + data[3]);
//					} else {
//						if (verboseMode) {
//							System.out.println(
//									"Server: Duplicate data packet received. Ignoring it by not writing it again.");
//							// TODO We should send an Nth ACK for the Nth duplicate data packet that was
//							// received.
//						}
//					}
//
//					if (verboseMode)
//						verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
//
//					byte[] ack = new byte[] { 0, 4, data[2], data[3] };
//
//					sendPacket = new DatagramPacket(ack, ack.length, receivePacket.getAddress(),
//							receivePacket.getPort());
//					sendPacketFromSocket(sendReceiveSocket, sendPacket);
//
//					if (verboseMode) {
//						System.out.println("TFTPClientConnectionThread: Sending ACK packet:");
//						verboseMode(receivePacket.getAddress(), receivePacket.getPort(), sendPacket.getLength(), ack);
//					}
//
//					if (len < 516) {
//						System.out.println("Received all data packets");
//						doneProcessingRequest = true;
//						establishedCommunications.remove(receivePacket.getSocketAddress().toString());
//						try {
//							out.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						break;
//					}
//				}
//			} catch (IOException e2) {
//				// TODO Auto-generated catch block
//				e2.printStackTrace();
//			}
//		}

		/**
		 * read files
		 * 
		 * @param filename
		 * @param sendPort
		 * @param receivePacket
		 */
//		public void transferFiles(String filename, int sendPort, DatagramPacket receivePacket) {
//			ArrayList<Integer> processedACKBlocks = new ArrayList<>();
//			int blockNum = 1; // You start at data block one when reading from a server.
//			byte[] data = new byte[100];
//			receivePacket.setData(data, 0, data.length);
//			
//			try {
//				sendReceiveSocket.setSoTimeout(TIMEOUT);
//			} catch (SocketException e1) {
//				e1.printStackTrace();
//			}
//
//			ArrayList<byte[]> msgBuffer = readFileIntoBlocks(serverDirectory + "\\" + fileName);
//
//			for (int i = 0; i < msgBuffer.size(); i++) {
//				byte[] msg = msgBuffer.get(i);
//				byte[] blockNumsInBytes = blockNumBytes(blockNum);
//				msg[0] = 0;
//				msg[1] = 3;
//				msg[2] = blockNumsInBytes[0];
//				msg[3] = blockNumsInBytes[1];
//				System.out.println("before sendPacket");
//
//				sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), sendPort);
//				sendPacketFromSocket(sendReceiveSocket, sendPacket);
//
//				if (verboseMode) {
//					System.out.println("Server: Packet sent:");
//					verboseMode(sendPacket.getAddress(), sendPacket.getPort(), sendPacket.getLength(), msg);
//				}
//
//				System.out.println("Server: Waiting for packet.");
//
//				try {
//					// Block until a datagram is received via sendReceiveSocket.
//					sendReceiveSocket.receive(receivePacket);
//				} catch (InterruptedIOException ie) {
//					System.out.println("Server timeout. Resending packet");
//					i--;
//					continue;
//				} catch (IOException e) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//
//				// Check if the received packet is a duplicate read request from a socket and
//				// port that is being handled. If so, ignore the packet and continue waiting.
//				if (establishedCommunications.contains(receivePacket.getSocketAddress().toString())
//						&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 1)) {
//					System.out.println("Server: Received a duplicate RRQ packet. Ignoring it.");
//					i--;
//					continue;
//				}
//
//				// Check if packet came from correct source. Send back ERROR packet code 5 if
//				// not.
//				if (sendPort != receivePacket.getPort()) {
//					System.out.println("Received Packet from unknown source. Responding with ERROR and continuing.");
//					byte[] err = new byte[] { 0, 5, 0, 5 };
//					sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
//							receivePacket.getPort());
//					sendPacketFromSocket(sendReceiveSocket, sendPacket);
//					i--;
//					continue;
//				}
//
//				// Check if packet received is an ERROR Packet
//				if (receivePacket.getData()[1] == 5) {
//					if (receivePacket.getData()[3] == 5) {
//						System.out.println("ERROR code 5: DATA Packet sent to wrong port. Resending last DATA packet.");
//						i--;
//						continue;
//					}
//				}
//
//				// Check if the received packet is a duplicate ACK. If it is, then we should not
//				// be re-sending the Nth data packet for the ACK. Sorcerer's Apprentice Bug.
//				if (!processedACKBlocks.contains(data[2] * 10 + data[3])) {
//					processedACKBlocks.add(data[2] * 10 + data[3]);
//				} else {
//					if (verboseMode) {
//						System.out.println(
//								"Server: Duplicate ACK data packet received. Ignoring it by not re-sending data block number ["
//										+ data[2] * 10 + data[3] + "] and waiting for the next datablock.");
//					}
//				}
//				int len = receivePacket.getLength();
//				if (verboseMode) {
//					System.out.println("Server: Packet received:");
//					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
//				}
//				if (sendPacket.getLength() < 516) {
//					System.out.println("Server: Last packet sent.");
//				}
//				blockNum++;
//			}
//			System.out.println("Finished Read");
//			doneProcessingRequest = true;
//			establishedCommunications.remove(receivePacket.getSocketAddress().toString());
//		}

	}
}
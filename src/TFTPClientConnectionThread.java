//package Iteration1;

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
			try {
				Thread.sleep(15);
			} catch (InterruptedException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
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

	public class TFTPsendThread implements Runnable {

		private DatagramSocket sendReceiveSocket;
		private DatagramPacket receivePacket, sendPacket;
		private boolean verboseMode = false; // false for quiet and true for verbose
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
				transferFiles(sendReceiveSocket, receivePacket, "Server", serverDirectory + "\\" + fileName,
						receivePacket.getPort(), processedACKBlocks, verboseMode);
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
			receiveFiles(serverDirectory + "\\" + fileName, sendReceiveSocket.getLocalPort(), "Server",
					sendReceiveSocket, true, false, verboseMode, connectionPort);
			// We're finished with this socket, so close it.
			sendReceiveSocket.close();
		}

	}

}
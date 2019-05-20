
// TFTPServer.java
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TFTPServer {
	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket receivePacket, sendPacket;
	private DatagramSocket receiveSocket;
	private static boolean verboseMode = false; // false for quiet and true for
												// verbose
	private static String serverDirectory = "";
	private static boolean fisnishedRequest = false;
	private Request req;
	
	//Responses for valid requests
	public static final byte[] readResp = {0, 3, 0, 1};
	public static final byte[] writeResp = {0, 4, 0, 0};

	//Type of transfer needed, true = Read transfer and false = write transfer
	private boolean transferType = false;
	
	//Finished responding RRQ and WRQ, false means not done, and true means completed 
	private boolean completedRequestResponse = false;
	
	//File path DESKTOP
	//TODO Must be changed based on system
	private static final String DESKTOP = "C:\\Users\\user\\Desktop";

	public TFTPServer() {
		try {
			// Construct a datagram socket and bind it to port 69
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @return the req
	 */
	public Request getReq() {
		return req;
	}

	/**
	 * @param req
	 *            the req to set
	 */
	public void setReq(Request req) {
		this.req = req;
	}

	/**
	 * @return the transferType
	 */
	public boolean getTransferType() {
		return transferType;
	}

	/**
	 * @param transferType the transferType to set
	 */
	public void setTransferType(boolean transferType) {
		this.transferType = transferType;
	}

	/**
	 * @return the completedRequestResponse
	 */
	public boolean getCompletedRequestResponse() {
		return completedRequestResponse;
	}

	/**
	 * @param completedRequestResponse the completedRequestResponse to set
	 */
	public void setCompletedRequestResponse(boolean completedRequestResponse) {
		this.completedRequestResponse = completedRequestResponse;
	}
	
	public void receiveAndSendTFTP() throws Exception {
		byte[] data = new byte[4];
		byte[] dataResponse = null;
		Scanner sc = new Scanner(System.in);
		String input = "";
		
		BufferedOutputStream out;
		BufferedInputStream in;

		int len, j = 0;

		for (;;) { // loop forever
			input = "";
			// After a request has been completed the user gets prompted
			// to enter the configuration "menu" again
			// TO BE IMPLEMENTED AFTER FILE TRANSFER IMPLEMENTATION IS COMPLETED
			if (fisnishedRequest) { // fisnishedRequest should be true after a
									// file has been fully read or written
				System.out.println("Enter 1 to change configerations or nothing to leave configs unchanged: ");
				while (!(input.equals("1") || input.equals(""))) {
					input = sc.nextLine();
					if (input.equals("1"))
						configServer();
				}
				fisnishedRequest = false; // set fisnishedRequest to false so
											// the user only gets prompt after a
											// request finishes
			}

			// Construct a DatagramPacket for receiving packets up
			// to 100 bytes long (the length of the byte array).

			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("------------------------------------------------------");
			System.out.println("Server (" + InetAddress.getLocalHost().getHostAddress() + ") : Waiting for packet.");
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// Process the received datagram.
			len = receivePacket.getLength();
			if (verboseMode) {
				System.out.println("Server: Packet received:");
				System.out.println("From host: " + receivePacket.getAddress());
				System.out.println("Host port: " + receivePacket.getPort());
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
			} else {
				System.out.println("Server: Packet received.");
			}

			// print the bytes
			for (j = 0; j < len; j++) {
				System.out.println("byte " + j + " " + data[j]);
			}

			// Form a String from the byte array.
			String received = new String(data, 0, len);
			System.out.println(received);
			
			//If request is a read or write, verify the request and create appropriate response and response to request is not completed
			if(this.getReq() == Request.READ || this.getReq() == Request.WRITE && this.getCompletedRequestResponse() == false){
				dataResponse = extractAndVerifyRequest(receivePacket);
			}
			
			//Start writing data as response, and RRQ and WRQ is completed
			//Write Transfer
			else if (this.getCompletedRequestResponse() == true && this.getTransferType() == true){
				String filename = extractFilename(new String(receivePacket.getData(), 0, receivePacket.getLength()));
				System.out.println(DESKTOP+ "\\"+ filename);
				try{
					out = new BufferedOutputStream(new FileOutputStream(DESKTOP+ "\\"+ filename));
					dataResponse = writeData(out, receivePacket, this.getReq());
				}catch(IOException e){
					e.printStackTrace();
				}
				
			}
			
			//Read Transfer
			else if (this.getCompletedRequestResponse() == true && this.getTransferType() == false){
				String filename = extractFilename(new String(receivePacket.getData(), 0, receivePacket.getLength()));
				System.out.println(DESKTOP+ "\\"+ filename);
				try{
					in = new BufferedInputStream(new FileInputStream(DESKTOP+ "\\"+ filename));
					dataResponse = readData(in, this.getReq());
				}catch(IOException e){
					e.printStackTrace();
				}
				
			}		
			
			//Create sendPacket to send Response
			sendPacket = new DatagramPacket(dataResponse, dataResponse.length);
			
			if (this.getReq() == Request.ERROR) { // it was invalid, close socket on port
										// 69 (so things work properly next
										// time) and quit
				receiveSocket.close();
				throw new Exception("Not yet implemented");
			}

			// Create a new client connection thread for each connection with
			// the client.
			Runnable newClient = new TFTPClientConnectionThread(this.getReq(), sendPacket, verboseMode);
			new Thread(newClient).start();
		} // end of loop
	}

	/**
	 * Extracts and verifies the first RRQ or WRQ sent by the client and creates an appropriate response
	 * @param dp - recieved data packet
	 * @return byte[] array of the response
	 */
	private byte[] extractAndVerifyRequest(DatagramPacket dp){
		byte[] data = dp.getData();
		int len = data.length;
		int j = 0, k = 0;
		
		// If it's a read, send back DATA (03) block 1
		// If it's a write, send back ACK (04) block 0
		// Otherwise, ignore it
		if (data[0] != 0)
			this.setReq(Request.ERROR); // bad
		else if (data[1] == 1)
			this.setReq(Request.READ); // could be read
		else if (data[1] == 2)
			this.setReq(Request.WRITE); // could be write
		else
			this.setReq(Request.ERROR); // bad

		if (this.getReq() != Request.ERROR) { // check for filename
			// search for next all 0 byte
			for (j = 2; j < len; j++) {
				if (data[j] == 0)
					break;
			}
			if (j == len)
				this.setReq(Request.ERROR); // didn't find a 0 byte
			if (j == 2)
				this.setReq(Request.ERROR); // filename is 0 bytes long
			// otherwise, extract filename
		}

		if (this.getReq() != Request.ERROR) { // check for mode
			// search for next all 0 byte
			for (k = j + 1; k < len; k++) {
				if (data[k] == 0)
					break;
			}
			if (k == len)
				this.setReq(Request.ERROR); // didn't find a 0 byte
			if (k == j + 1)
				this.setReq(Request.ERROR); // mode is 0 bytes long
		}

		if (k != len - 1)
			this.setReq(Request.ERROR); // other stuff at end of packet
		
		//Valid Read response as all the above checks have passed
		if(this.getReq() != Request.READ){
			//Read Transfer type
			this.setTransferType(true);
			//Completed Request Response
			this.setCompletedRequestResponse(true);
			return readResp;
		}
		
		//Valid Write response as all the above checks have passed
		else if(this.getReq() != Request.READ){
			//Write Transfer type
			this.setTransferType(false);
			//Completed Request Response
			this.setCompletedRequestResponse(false);
			return writeResp;
		}
		//Error Response
		else{
			return null;
		}
	}
	
	/**
	 * Writes Data to an outputstream, while waiting for data recieved from the client
	 * @param out - outputstream
	 * @param dp - recieved data packet
	 * @param rType - Type of request
	 * @return the byte [] array of Response to send to the client
	 */
	private byte[] writeData(BufferedOutputStream out, DatagramPacket dp, Request rType) {
		byte[] data = dp.getData();
		byte[] ackResp = new byte[4];
		ackResp[0] = 0;
		ackResp[1] = 4;

		// Not an Error Request
		if (rType != Request.ERROR) {
			try {
				while (data.length <= 516) {
					// Check if it is a data request
					if (!(data[0] == 0 && data[1] == 3)) {
						this.setReq(Request.ERROR);
					}
					
					//Check if the packet is the right size
					if(data.length < 4){
						this.setReq(Request.ERROR);
					}
					
					// Write data to the output file
					out.write(data, 4, data.length - 4);

					// Creation of Ack Response
					// Copy block number recieved as well
					System.arraycopy(data, 2, ackResp, 2, 2);

					System.out.println("Write: File Transfer has ended.");
					out.close();
				}

			} catch (IOException e) {
				this.setReq(Request.ERROR);
				e.printStackTrace();
				System.exit(1);
			}
		}

		return ackResp;
	}
	
	/**
	 * Reads data, waits for acknowledgements sent by the client
	 * @param in - input stream
	 * @param rType - Request type
	 * @return byte array of the response
	 */
	private byte[] readData(BufferedInputStream in, Request rType){
		int i;
		byte[] data = null;
		byte[] fileData = new byte[512];
		int blockNum = 1;
		
		// Not an Error Request
		if (rType != Request.ERROR) {
			try{
				//Check if it is finished reading the end of a file
				while((i = in.read(fileData)) != -1){
					// Check if it is a ack request
					if (!(data[0] == 0 && data[1] == 4)) {
						this.setReq(Request.ERROR);
					}
					
					//Check if the packet is the right size
					if(data.length != 4){
						this.setReq(Request.ERROR);
					}
					
					//Last packet to be sent
					if(i==-1){
						data = new byte[4];
						data[0] = 0;
						data[1] = 3;
						//Copy block numbers into bytes
						data[2] = blockNumBytes(blockNum)[0];
						data[3] = blockNumBytes(blockNum)[1];

					}
					else{
						data = new byte[i + 4];
						data[0] = 0;
						data[1] = 3;
						//Copy block numbers into bytes
						data[2] = blockNumBytes(blockNum)[0];
						data[3] = blockNumBytes(blockNum)[1];
						
						//Fill the rest of data buffer with bytes read from the stream
						for(int j = 0; j < i; j++){
							data[j + 4] = fileData[j];
						}
					}
					
					//Increment blockNum
					blockNum++;
					System.out.println("Read: File Transfer has ended.");
					in.close();
				}
			}catch (IOException e) {
				this.setReq(Request.ERROR);
				e.printStackTrace();
				System.exit(1);
			}


		}
		
		return data;
	}

	// returns the packet number as an integer
	private int retrieveBlockNumber(byte[] data) {
		int x = (int) data[2];
		int y = (int) data[3];
		if (x < 0) {
			x = 256 + x;
		}
		if (y < 0) {
			y = 256 + y;
		}

		return 256 * x + y;
	}
	
	//Converts the blocknumber as an int into a 2 byte array
	private byte[] blockNumBytes(int blockNum){
		byte[] blockNumArray = new byte[2];
		
		// create the corresponding block number in 2 bytes
		byte block1 = (byte) (blockNum / 256);
		byte block2 = (byte) (blockNum % 256);
		blockNumArray[0] = block1;
		blockNumArray[1] = block2;
		return blockNumArray;
		
	}
	
	//Extracts the filename
	private String extractFilename(String data) {
		return data.split("\0")[1].substring(1);
	}
		
	/**
	 * "Menu" for configuring the settings of client application
	 */
	public static void configServer() {
		Scanner sc = new Scanner(System.in); // scanner for getting user's input
		String input = " ";

		// option to toggle verbose or quiets mode
		while (!(input.equals("1") || input.equals(""))) { // loops until valid
															// input (1 or
															// nothing)
			System.out.println("\nEnter '1' to toggle between quiet and verbose mode ");
			System.out.print("or nothing to stay in " + (verboseMode ? "verbose" : "quiet") + " mode: ");
			input = sc.nextLine();
			// toggling verboseMode
			if (input.equals("1"))
				verboseMode = verboseMode ? false : true;
		}
		System.out.println("Running in " + (verboseMode ? "verbose" : "quiet") + " mode");

		input = "";
		System.out.println(
				"\nCurrent server directory is: " + (serverDirectory.equals("") ? "undefined" : serverDirectory));
		// option to set the file directory.
		// User must input file directory at the first launch.
		// Once an file directory has been set, the user can enter nothing to
		// keep it unchanged.
		while (input.equals("")) {
			System.out.println("Enter the server of directory or nothing to keep the directory unchanged: ");
			input = sc.nextLine();

			if (input.equals("")) {
				if (serverDirectory.equals(""))
					System.out.println("Server directory has not been entered yet!");
				else
					input = "entered"; // set input to arbitrary string to leave
										// loop
			} else {
				serverDirectory = input;
				System.out.println("Server directory is now: " + serverDirectory);
			}
		}

		System.out.println("\n------------------------------------------------------\nConfigerations are now set up.");
	}

	public static void main(String args[]) throws Exception {
		TFTPServer s = new TFTPServer();
		System.out.println("Welcome to the TFTP server application");
		configServer();
		s.receiveAndSendTFTP();
	}
}

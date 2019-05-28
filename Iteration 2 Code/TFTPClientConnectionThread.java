import java.io.*;
import java.net.*;
import java.util.Arrays;

//import TFTPClient.RequestType;

public class TFTPClientConnectionThread implements Runnable {

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private boolean verboseMode = false; // false for quiet and true for verbose

	private Request request;
	// responses for valid requests
	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };
	
	
	String fileName;

	// File path DESKTOP
	// TODO Must be changed based on system
	
	private boolean doneProcessingRequest = true;
	
	// List to keep track of whom the server is already talking to.
	// Used to handle duplicate packets from the same source.
	private ArrayList<String> establishedCommunications = new ArrayList<String>(); 
	
	private String serverDirectory;

	public TFTPClientConnectionThread(boolean verboseMode, String serverDirectory ) {
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

	public void run() {
		int len, j = 0, k = 0;
		
		while (true) { // loop forever

			System.out.print("");
			if (doneProcessingRequest) {
				byte[] data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);

				System.out.println("------------------------------------------------------");
				System.out.println("Type 'quit' to shutdown.");
				try {
					System.out
					.println("Server (" + InetAddress.getLocalHost().getHostAddress() + ") : Waiting for packet.");
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// Block until a datagram packet is received from receiveSocket.
				try {
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
					System.out.println("From host: " + receivePacket.getAddress());
					System.out.println("Host port: " + receivePacket.getPort());
					System.out.println("Length: " + len);
					System.out.println("Containing: ");
					// print the bytes
					for (j = 0; j < len; j++) {
						System.out.println("byte " + j + " " + data[j]);
					}
				} else {
					System.out.println("Server: Packet received.");
				}

				// Form a String from the byte array.
				String received = new String(data, 0, len);
				fileName = received.split("\0")[1].substring(1);
				System.out.println(fileName);

				// If it's a read, send back DATA (03) block 1
				// If it's a write, send back ACK (04) block 0
				// Otherwise, ignore it
				if (data[0] != 0)
					request = Request.ERROR; // bad
				else if (data[1] == 1)
					request = Request.READ; // could be read
				else if (data[1] == 2)
					request = Request.WRITE; // could be write
				else
					request = Request.ERROR; // bad

				if (request != Request.ERROR) { // check for filename
					// search for next all 0 byte
					for (j = 2; j < len; j++) {
						if (data[j] == 0)
							break;
					}
					if (j == len)
						request = Request.ERROR; // didn't find a 0 byte
					if (j == 2)
						request = Request.ERROR; // filename is 0 bytes long
					// otherwise, extract filename
				}

				if (request != Request.ERROR) { // check for mode
					// search for next all 0 byte
					for (k = j + 1; k < len; k++) {
						if (data[k] == 0)
							break;
					}
					if (k == len)
						request = Request.ERROR; // didn't find a 0 byte
					if (k == j + 1)
						request = Request.ERROR; // mode is 0 bytes long
				}

				if (k != len - 1)
					request = Request.ERROR; // other stuff at end of packet

				if (request == Request.ERROR) { // it was invalid, close socket on port 69 (so things work properly next
					// time) and quit
					receiveSocket.close();
					try {
						throw new Exception("Not yet implemented");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				establishedCommunications.add(receivePacket.getSocketAddress().toString());
				
				// Create a response.
				if (request == Request.READ) {
					// create new thread for sending data
					Runnable readReqThread = new TFTPsendThread(request, receivePacket, verboseMode);
					new Thread(readReqThread).start();

				} else if (request == Request.WRITE) {
					// create new thread for sending data
					Runnable writeReqThread = new TFTPsendThread(request, receivePacket, verboseMode);
					new Thread(writeReqThread).start();
				}
				/*
				 * if (request == Request.ERROR) { // it was invalid, close socket on port 69
				 * (so things work properly next time) and quit receiveSocket.close(); try {
				 * throw new Exception("Not yet implemented"); } catch (Exception e) { // TODO
				 * Auto-generated catch block e.printStackTrace(); } }
				 */
			}
		} // end of loop
	}

	public class TFTPsendThread implements Runnable {

		private DatagramSocket sendSocket;
		private DatagramPacket receivePacket;
		private DatagramPacket sendPacket;
		private boolean verboseMode = false; // false for quiet and true for verbose
		private static final int TIMEOUT = 1000;//Delay for timeout when waiting to receive file 

		private Request request;

		public TFTPsendThread(Request request, DatagramPacket receivePacket, boolean verboseMode) {
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(0);
			}

			this.receivePacket = receivePacket;
			this.request = request;
			this.verboseMode = verboseMode;

		}

		public void run() {
			byte[] response = new byte[4];
			// Create a response.
			if (request == Request.READ) { // for Read it's 0301
				//response = readResp;
				transferFiles(fileName, receivePacket.getPort(), receivePacket);
			} else if (request == Request.WRITE) { // for Write it's 0400
				response = writeResp;
				send(response);
			}

		}

		private void send(byte[] response) {
			sendPacket = new DatagramPacket(response, response.length, receivePacket.getAddress(),
					receivePacket.getPort());

			int len = sendPacket.getLength();
			if (verboseMode) {
				System.out.println("TFTPClientConnectionThread: Sending packet:");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				for (int j = 0; j < len; j++) {
					System.out.println("byte " + j + " " + response[j]);
				}
			} else {
				System.out.println("Server: Packet sent.");
			}

			// Send the datagram packet to the client via a new socket.
			try {
				// Construct a new datagram socket and bind it to any port
				// on the local host machine. This socket will be used to
				// send UDP Datagram packets.
				sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}

			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("TFTPClientConnectionThread: packet sent using port " + sendSocket.getLocalPort());
			System.out.println();

			/*if (request == Request.READ) {
				transferFiles(fileName, sendSocket.getLocalPort());
			} else */if (request == Request.WRITE) {
				receiveFiles(fileName, sendSocket.getLocalPort());
			}

			// We're finished with this socket, so close it.
			sendSocket.close();
		}

		// write to file
		public void receiveFiles(String fileName, int sendPort) {			
			
			try {
				receiveSocket.setSoTimeout(TIMEOUT);
				BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(serverDirectory + "\\" + fileName));
				while (true) {
					byte[] data = new byte[516];
					receivePacket = new DatagramPacket(data, data.length);
					int len = receivePacket.getLength();

					System.out.println("Server: Waiting for data packet.");

					try {
						// Block until a datagram is received via sendReceiveSocket, or until
						// idle for exceptional amount of time
						receiveSocket.setSoTimeout(300000);
						receiveSocket.receive(receivePacket);
						receiveSocket.setSoTimeout(TIMEOUT);
					} catch(InterruptedIOException ie) {
						//NOT IMPLEMENTED. Behind current version, might not work with changes
						System.out.println("Server idle timeout. Closing connection.");
						break;
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}

					// Check if the received packet is a duplicate read or write request from a socket and port that is being handled. If so, ignore the packet and continue waiting.
					if (establishedCommunications.contains(receivePacket.getSocketAddress().toString()) && (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 2)) {
						System.out.println("Server: Received a duplicate WRQ packet. Ignoring it.");
						continue;
						
					}
					// Process the received datagram.
					len = receivePacket.getLength();
					data = receivePacket.getData();
					System.out.println("Server: Data Packet received.");

					try {
						out.write(data, 4, data.length - 4);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
					
					byte[] ack = new byte[] { 0, 4, 0, 0 };
					
					sendPacket = new DatagramPacket(ack, ack.length, receivePacket.getAddress(),
							receivePacket.getPort());
					
					try {
						sendSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}

					if (verboseMode) {
						System.out.println("TFTPClientConnectionThread: Sending ACK packet:");
						System.out.println("To host: " + receivePacket.getAddress());
						System.out.println("Destination host port: " + receivePacket.getPort());
						System.out.println("Length: " + sendPacket.getLength());
						System.out.println("Containing: ");
						for (int j = 0; j < sendPacket.getLength(); j++) {
							System.out.println("byte " + j + " " + ack[j]);
						}
					} else {
						System.out.println("Server: ACK Packet sent.");
					}
					
					if (len < 516) {
						System.out.println("Received all data packets");
						doneProcessingRequest = true;
						establishedCommunications.remove(receivePacket.getSocketAddress().toString());
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

		// Converts the blocknumber as an int into a 2 byte array
		private byte[] blockNumBytes(int blockNum) {
			byte[] blockNumArray = new byte[2];

			// create the corresponding block number in 2 bytes
			byte block1 = (byte) (blockNum / 256);
			byte block2 = (byte) (blockNum % 256);
			blockNumArray[0] = block1;
			blockNumArray[1] = block2;
			return blockNumArray;

		}

		// read files
		public void transferFiles(String filename, int sendPort, DatagramPacket receivePacket) {
		   int blockNum = 0;
		   byte[] data = new byte[100];
	       receivePacket.setData(data, 0, data.length);;
		   byte[] dataBuffer = new byte[512];
		   try {
			   BufferedInputStream bis = new BufferedInputStream(new FileInputStream(serverDirectory + "\\" + filename));
				System.out.println(serverDirectory + "\\" + filename);

				System.out.println("Within Transfer files (for reading) server");
				int bytesRead = 0;
				while ((bytesRead = bis.read(dataBuffer, 0, 512)) != -1) {
					System.out.println(bis.read(dataBuffer));

					byte[] msg = new byte[bytesRead + 4];
					msg[0] = 0;
					msg[1] = 3;
					msg[2] = blockNumBytes(blockNum)[0];
					msg[3] = blockNumBytes(blockNum)[1];
					System.arraycopy(dataBuffer, 0, msg, 4, bytesRead);
					System.out.println("before sendPacket");
					
					sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), sendPort);
					try {
						sendSocket.send(sendPacket);
				    } catch (IOException e) {
				        e.printStackTrace();
				        System.exit(1);
				    }
					
					if (verboseMode) {
						System.out.println("Server: Packet sent:");
						System.out.println("From host: " + sendPacket.getAddress());
						System.out.println("Host port: " + sendPacket.getPort());
						System.out.println("Length: " + sendPacket.getLength());
						System.out.println("Containing: ");
						for (int j = 0; j < sendPacket.getLength(); j++) {
							System.out.println("byte " + j + " " + msg[j]);
						}
					} else {
						System.out.println("Server: Packet sent.");
					}

					System.out.println("Server: Waiting for packet.");

					try {
				           // Block until a datagram is received via sendReceiveSocket.
				           receiveSocket.receive(receivePacket);
				        } catch(InterruptedIOException ie) {
				        	System.out.println("Server timeout. Resending packet");
				        	continue;
						} catch(IOException e) {
				           e.printStackTrace();
				           System.exit(1);
				        }
					   int len = receivePacket.getLength();
					   
						if (verboseMode) {
							System.out.println("Client: Packet received:");
							System.out.println("From host: " + receivePacket.getAddress());
							System.out.println("Host port: " + receivePacket.getPort());
							System.out.println("Length: " + len);
							System.out.println("Containing: ");
							for (int j = 0; j < len; j++) {
								System.out.println("byte " + j + " " + data[j]);
							}
						} else {
							System.out.println("Server: Packet received.");
						}

						if (sendPacket.getLength() < 516) {
							System.out.println("Server: Last packet sent.");
						}
					blockNum++;
				}
				System.out.println("Finished Read");
				bis.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}
}

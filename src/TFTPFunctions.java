import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <h1>Transferring packets functionalities</h1> 
 * <p>Includes common methods needed by Client and Server for communicating</p>
 *
 * @since 2019-06-16
 */
public class TFTPFunctions {

	protected static final int TIMEOUT = 4000;
	protected static final int MAX_TIMEOUT = 5;
	private DatagramPacket receivePacket, sendPacket;
	public static final int ERROR_CODE_FILE_NOT_FOUND = 1;
	public static final int ERROR_CODE_ACCESS_VIOLATION = 2;
	public static final int ERROR_CODE_DISK_FULL = 3;
	public static final int ERROR_CODE_ILLEGAL_TFTP_OPERATION = 4;
	public static final int ERROR_CODE_UNKNOWN_TID = 5;

	/**
	 * Read and partition a file into an array of blocks(512 bytes) of data.
	 * 
	 * @param fileName
	 *            The name of the file to partition into an array
	 * @param socket
	 *            The host socket
	 * @param address
	 *            The address of host socket
	 * @param sendPort
	 *            The host socket port number
	 * @return ArrayList<byte[]> An array of 512 bytes of data
	 */
	ArrayList<byte[]> readFileIntoBlocks(String fileName, DatagramSocket socket, InetAddress address, int sendPort) {
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

			// If the last block has 512 bytes of data or the file is empty,
			// the file is a multiple of 512 data bytes and needs an empty data packet at the end
			if (msgList.size() == 0 || msgList.get(msgList.size() - 1).length == 516) {
				byte[] temp = new byte[4]; // temp byte array for last data packet
				temp[0] = 0;
				temp[1] = 3;
				temp[2] = 0;
				temp[3] = 0;
				msgList.add(temp);
			}
			bis.close();
		} catch (IOException e) {
	        // If the file cannot be find, send an error packet 1
			if (e instanceof FileNotFoundException) {
				System.out.println("File [" + fileName + "] not found.\n");
				DatagramPacket errorPacket = createErrorPacket(address, sendPort, ERROR_CODE_FILE_NOT_FOUND,
						"Unable to find the following file: " + fileName + ". It does not exist.");
				sendPacketFromSocket(socket, errorPacket);
			} else {
				e.printStackTrace();
			}
		}

		return msgList;
	}

	/**
	 * Creates an error packet
	 * 
	 * @param address
	 *            This is the IP address of the destination of the packet to be sent
	 * @param port
	 *            This is the port number of the destination of the packet to be sent
	 * @param errCode
	 *            This is the code representing the type of error
	 * @param errMsg
	 *            This is the detailed error message
	 * 
	 * @return DatagramPacket An error packet
	 */
	public DatagramPacket createErrorPacket(InetAddress address, int port, int errCode, String errMsg) {
		byte[] sbytes = errMsg.getBytes();
		byte[] buf = new byte[4 + sbytes.length + 1]; // 4 because 2 bytes for opcode, 2 bytes for block number.

		// Opcode for error codes
		buf[0] = 0;
		buf[1] = 5;

		try {
			byte[] blockNumberBytes = blockNumBytes(errCode);
			System.arraycopy(blockNumberBytes, 0, buf, 2, 2);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.arraycopy(sbytes, 0, buf, 4, sbytes.length);
		buf[4 + sbytes.length] = 0;

		return new DatagramPacket(buf, buf.length, address, port);
	}

	/**
	 * Converts a block number into a 2 byte array
	 * 
	 * @param blockNum
	 *            The block number to be converted
	 * @return byte[] The block number in a 2 byte array format
	 */
	byte[] blockNumBytes(int blockNum) {
		// create the corresponding block number in 2 bytes
		byte[] blockArray = { (byte) (blockNum / 256), (byte) (blockNum % 256) };
		return blockArray;
	}

	/**
	 * Prints to console information about a packet
	 * 
	 * @param inetAddress
	 *            The address of the destination socket
	 * @param port
	 *            The port number of the destination socket
	 * @param length
	 *            The length of the packet
	 * @param data
	 *            The contents of the packet
	 */
	void verboseMode(InetAddress inetAddress, int port, int length, byte[] data) {
		System.out.println("Destination Host: " + inetAddress);
		System.out.println("Destination Host Port: " + port);
		System.out.println("Length: " + length);
		System.out.println("Containing: ");
		for (int j = 0; j < length; j++) {
			System.out.println("byte " + j + " " + data[j]);
		}
	}

	/**
	 * Sends the Packet from a Socket
	 * 
	 * @param socket
	 *            The socket to send the packet from
	 * @param packet
	 *            The packet to be sent by the socket
	 */
	void sendPacketFromSocket(DatagramSocket socket, DatagramPacket packet) {
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Receives a Packet from a Socket
	 * 
	 * @param socket
	 *            The socket to receive the packet
	 * @param packet
	 *            The packet to be received by the socket
	 */
	void receivePacketFromSocket(DatagramSocket socket, DatagramPacket packet) {
		try {
			// Block until a datagram is received via sendReceiveSocket.
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Receive Data Packets during a Read or Write Operation
	 * 
	 * @param fileName
	 *            The name of the file
	 * @param sendPort
	 *            The destination port number
	 * @param host
	 *            The source's name - Client or Server
	 * @param socket
	 *            The socket to handle receiving data packets and sending ACK
	 *            packets
	 * @param testMode
	 *            If running in test mode
	 * @param requestResponse
	 *            If the packet to be received is a request
	 * @param verboseMode
	 *            If the verbose mode was selected
	 * @param connectionPort The destination port number
	 */
	void receiveFiles(String fileName, int sendPort, String host, DatagramSocket socket, boolean testMode,
			boolean requestResponse, boolean verboseMode, int connectionPort) {
		File file = new File(fileName);
		int fileSize = 0;
		// print message for overwriting existing file
		if (file.exists())
			System.out.println("\n-----------WARNING, ERROR CODE 6. File Already Exists. Overwriting file...----------");
		ArrayList<Integer> processedBlocks = new ArrayList<>();
		int expectedBlockNumber = 1;
		// modify sendPort to error sim port if in test mode
		if (testMode) sendPort = 23;
		try {
			socket.setSoTimeout(TIMEOUT);
			boolean fileOpen = false;
			BufferedOutputStream out = null;
			while (true) {
				byte[] data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				int len = receivePacket.getLength();
				System.out.println(host + ": Waiting for data packet.");
				try {
					// Block until a datagram is received via sendReceiveSocket.
					socket.setSoTimeout(300000);
					socket.receive(receivePacket);
					socket.setSoTimeout(TIMEOUT);

					if (requestResponse) {
						if (!testMode) sendPort = receivePacket.getPort();
						requestResponse = false;
						connectionPort = receivePacket.getPort();
					}
				} catch (InterruptedIOException io) {
					if (requestResponse) {
						System.out.println(host + " timed out. resending request.");
					} else {
						System.out.println(host + " has exceeded idle time. Cancelling transfer.");
						if (host.equals("Client")) {
							TFTPClient.finishedRequest = true;
							TFTPClient.changeMode = true;
						}
					}
					break;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				if (!fileOpen) {
					out = new BufferedOutputStream(new FileOutputStream(file));
					fileOpen = true;
				}
				
				// check to see if duplicate write request packet has been received
				if (host.equals("Server")
						&& TFTPClientConnectionThread.establishedCommunications
								.contains(receivePacket.getSocketAddress().toString())
						&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 2)) {
					System.out.println("Server: Received a duplicate WRQ packet. Ignoring it.");
					continue;
				}
				
				// check if valid packet, otherwise send error code 4 packet
				if (!isValidOperation(data)) {
					System.out.println("Received an invalid TFTP operation. Responding with ERROR code 4 and stoping.");
					byte[] err = new byte[] { 0, 5, 0, 4 };
					if (host.equals("Client")) {
						sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), sendPort);
					} else {
						sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
								receivePacket.getPort());
					}
					sendPacketFromSocket(socket, sendPacket);
					break;
				}

				// Check if packet came from correct source. Send back ERROR packet code 5 if not
				if (connectionPort != receivePacket.getPort()) {
					System.out.println(
							"Received Packet from unknown source. Responding with ERROR CODE 5 and continuing.");
					byte[] err = new byte[] { 0, 5, 0, 5 };
					DatagramPacket sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
							receivePacket.getPort());
					sendPacketFromSocket(socket, sendPacket);
					continue;
				}

				// Process the received datagram.
				len = receivePacket.getLength();
				data = receivePacket.getData();

				// Check for error 4 "Invalid TFTP Operation" or error code 4
				if (data[1] == 5 && (data[3] == 4)) {
					System.out.println(
							"\nERROR - " + new String(Arrays.copyOfRange(data, 4, data.length), "UTF-8") + "\n");
					if (host.equals("Client")) {
						TFTPClient.finishedRequest = true;
						TFTPClient.changeMode = true;
					} else if (host.equals("Server")) {
						TFTPClientConnectionThread.doneProcessingRequest = true;
						TFTPClientConnectionThread.establishedCommunications
								.remove(receivePacket.getSocketAddress().toString());
					}
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Delete the file that has been created at the beginning of the method.
					new File(fileName).delete();
					break;
				}

				// Check for error code 1 for invalid file name or error code 2 for access violation
				if (data[1] == 5 && (data[3] == 1 || data[3] == 2)) {
					System.out.println("\nERROR - " + new String(Arrays.copyOfRange(data, 4, data.length), "UTF-8") + "\n");
					if (host.equals("Client")) {
						TFTPClient.finishedRequest = true;
						TFTPClient.changeMode = true;
					} else if (host.equals("Server")) {
						TFTPClientConnectionThread.doneProcessingRequest = true;
						TFTPClientConnectionThread.establishedCommunications.remove(receivePacket.getSocketAddress().toString());
					}
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Delete the file that has been created at the beginning of the method.
					new File(fileName).delete();
					break;
				}

				// Check if an error packet was received
				if (data[1] == 5 && data[3] == 5) {
					System.out.println("ERROR code 5: ACK Packet sent to wrong port. Waiting for proper DATA.");
					continue;
				}
				// Check if the next data block is next in sequence.
				if ((data[2] * 256 + data[3]) > expectedBlockNumber) {
					// Create an error packet 4 and terminate connection.
					System.out.println("Received an invalid data packet block number sending error packet and stoping.");
					byte[] err = new byte[] { 0, 5, 0, 4 };
					if (host.equals("Client")) {
						sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), sendPort);
					} else {
						sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
								receivePacket.getPort());
					}
					sendPacketFromSocket(socket, sendPacket);
					break;
				}
				// Check if it's a duplicate packet. If it is, send an ACK but do not rewrite to the file.
				if (!processedBlocks.contains(data[2] * 256 + data[3])) {
					// This block number has not been processed. Write it to the file.
					try {
						out.write(data, 4, len - 4);
						fileSize += len - 4;
					} catch (IOException e) {
						if (file.getFreeSpace() < len - 4) {
							System.out.println("No space on disk. Terminating tranfer.");
							// Send Error code 3 packet
							byte[] err = new byte[] { 0, 5, 0, 3 };
							sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), sendPort);
							sendPacketFromSocket(socket, sendPacket);
							// Deleting the incomplete file
							file.delete();

							if (host.equals("Client")) {
								TFTPClient.finishedRequest = true;
								TFTPClient.changeMode = true;
							} else if (host.equals("Server")) {
								TFTPClientConnectionThread.doneProcessingRequest = true;
								TFTPClientConnectionThread.establishedCommunications
										.remove(receivePacket.getSocketAddress().toString());
							}
							try {
								out.close();
							} catch (IOException ie) {
								// TODO Auto-generated catch block
								ie.printStackTrace();
							}
							break;
						}
						e.printStackTrace();
					}
					processedBlocks.add(data[2] * 256 + data[3]);
					expectedBlockNumber++;
				} else if (verboseMode) {
						System.out.println(host + ": Duplicate data packet received. Ignoring it by not writing it again.");
				}

				if (verboseMode) {
					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
				}

				byte[] ack = new byte[] { 0, 4, data[2], data[3] };
				if (host.equals("Client")) {
					sendPacket = new DatagramPacket(ack, ack.length, receivePacket.getAddress(), sendPort);
				} else {
					sendPacket = new DatagramPacket(ack, ack.length, receivePacket.getAddress(),
							receivePacket.getPort());
				}
				sendPacketFromSocket(socket, sendPacket);

				if (verboseMode) {
					System.out.println(host + ": Sending ACK packet:");
					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), sendPacket.getLength(), ack);
				}

				if (len < 516) {
					System.out.println("Received all data packets");
					try {
						out.close();
					} catch (IOException e) {
						if (file.getFreeSpace() < fileSize) {
							System.out.println("No space on disk. Terminating tranfer.");
							// Sending ERROR packet for Error code 3
							byte[] err = new byte[] { 0, 5, 0, 3 };
							sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), sendPort);
							sendPacketFromSocket(socket, sendPacket);
							// Deleting the incomplete file
							file.delete();
						}
					}
					break;
				}
			}
		} catch (FileNotFoundException e2) {
			System.out.println(sendPort);
			System.out.println("ERROR 2: Access Violation. Sending Error Packet 2.");
			// Sending ERROR packet for Error code 2 if access violation
			byte[] err = new byte[] { 0, 5, 0, 2 };
			sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), receivePacket.getPort());
			sendPacketFromSocket(socket, sendPacket);
			if (verboseMode) {
				verboseMode(receivePacket.getAddress(), receivePacket.getPort(), err.length, err);
			}
			if (host.equals("Client")) {
				TFTPClient.finishedRequest = true;
				TFTPClient.changeMode = true;
			} else if (host.equals("Server")) {
				TFTPClientConnectionThread.doneProcessingRequest = true;
				TFTPClientConnectionThread.establishedCommunications
						.remove(receivePacket.getSocketAddress().toString());
			}
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		if (host.equals("Client")) {
			TFTPClient.finishedRequest = true;
			TFTPClient.changeMode = true;
		} else if (host.equals("Server")) {
			TFTPClientConnectionThread.doneProcessingRequest = true;
			TFTPClientConnectionThread.establishedCommunications.remove(receivePacket.getSocketAddress().toString());
		}
	}

	/**
	 * Send Data Packets during a Read or Write Operation
	 *
	 * @param socket
	 *            The socket to handle sending data packets and receiving ACK
	 *            packets
	 * @param receivePacket
	 *            The packet received from the destination socket
	 * @param host
	 *            The source's name - Client or Server
	 * @param fileName
	 *            The name of the file
	 * @param sendPort
	 *            The destination port number
	 * @param processedACKBlocks
	 *            The array of ACK blocks that have been received
	 * @param verboseMode
	 *            If the verbose mode was selected
	 */
	void transferFiles(DatagramSocket socket, DatagramPacket receivePacket, String host, String fileName, int sendPort,
			ArrayList<Integer> processedACKBlocks, boolean verboseMode) {
		int timeoutCount = 0;
		int blockNum = 1; // Start at data block one when reading from a server.
		byte[] data = new byte[100];
		if (host.equals("Server"))
			receivePacket.setData(data, 0, data.length);
		else
			receivePacket = new DatagramPacket(data, data.length);

		InetAddress sendAddress = null;
		try {
			sendAddress = host.equals("Client") ? InetAddress.getByName(TFTPClient.ipAddress)
					: receivePacket.getAddress();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
			return; // Return since we will not be able to send a packet without a valid IP address.
		}

		ArrayList<byte[]> msgBuffer = readFileIntoBlocks(fileName, socket, sendAddress, sendPort);

		try {
			socket.setSoTimeout(TIMEOUT);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		for (int i = 0; i < msgBuffer.size(); i++) {
			byte[] msg = msgBuffer.get(i);
			byte[] blockNumsInBytes = blockNumBytes(blockNum);
			msg[0] = 0;
			msg[1] = 3;
			msg[2] = blockNumsInBytes[0];
			msg[3] = blockNumsInBytes[1];

			sendPacket = new DatagramPacket(msg, msg.length, sendAddress, sendPort);

			sendPacketFromSocket(socket, sendPacket);

			if (verboseMode) {
				System.out.println(host + ": Packet sent:");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), sendPacket.getLength(), msg);
			}

			try {
				// Block until a datagram is received via sendReceiveSocket.
				socket.receive(receivePacket);
			} catch (InterruptedIOException ie) {
				// increment timeout count until max timeout has been reached in which case, terminate the transfer
				timeoutCount++;
				if (timeoutCount == MAX_TIMEOUT) {
					System.out.println("Maximum timeouts occurred without response. Terminating transfer.");
					if (host.equals("Client")) {
						TFTPClient.finishedRequest = true;
						TFTPClient.changeMode = true;
					}
					break;
				}
				System.out.println(host + " Timed out. Resending packet.");
				i--;
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// reset timeout counter
			timeoutCount = 0;

			// Check if the received packet is a duplicate ACK. If it is, then
			// re-send the Nth data packet for the ACK. Sorcerer's Apprentice Bug.
			if (!processedACKBlocks.contains(data[2] * 256 + data[3])) {
				processedACKBlocks.add(data[2] * 256 + data[3]);
			} else {
				if (verboseMode) {
					System.out.println(host
							+ ": Duplicate ACK data packet received. Ignoring it by not re-sending data block number ["
							+ data[2] * 256 + data[3] + "] and waiting for the next datablock.");
				}
			}

			// Check if the received packet is a duplicate read request from a socket and
			// port that is being handled. If so, ignore the packet and continue waiting.
			if (host.equals("Server")) {
				if (TFTPClientConnectionThread.establishedCommunications
						.contains(receivePacket.getSocketAddress().toString())
						&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 1)) {
					System.out.println("Server: Received a duplicate RRQ packet. Ignoring it.");
					i--;
					continue;
				}
			}

			// Check if packet came from correct source. Send back ERROR packet code 5 if not
			if ((host.equals("Server") && sendPort != receivePacket.getPort())
					|| (host.equals("Client") && TFTPClient.connectionPort != receivePacket.getPort())) {
				System.out.println("Received Packet from unknown source. Responding with ERROR code 5 and continuing.");
				byte[] err = new byte[] { 0, 5, 0, 5 };
				sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), receivePacket.getPort());
				sendPacketFromSocket(socket, sendPacket);
				i--;
				continue;
			}

			// Check for error 4 "Invalid TFTP Operation".
			if (!isValidOperation(data)) {
				System.out.println("Received an invalid TFTP operation. Responding with ERROR code 4 and stoping.");
				byte[] err = new byte[] { 0, 5, 0, 4 };
				sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), receivePacket.getPort());
				sendPacketFromSocket(socket, sendPacket);
				i--;
				break;
			}

			if ((data[2] * 256 + data[3]) > blockNum) {
				// Create an error packet 4 and terminate connection.
				System.out.println("Received an invalid data packet block number sending error packet and stoping.");
				byte[] err = new byte[] { 0, 5, 0, 4 };
				sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), receivePacket.getPort());
				sendPacketFromSocket(socket, sendPacket);
				i++;
				break;
			}

			// Check if packet received is an ERROR Packet
			if (receivePacket.getData()[1] == 5) {
				// Check for error 1 "Invalid file name".
				if (data[1] == 5 && data[3] == 1) {
					try {
						System.out.println("\nERROR - " + new String(Arrays.copyOfRange(data, 4, data.length), "UTF-8") + "\n");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					continue;
				}
				if (receivePacket.getData()[3] == 2) {
					System.out.println("ERROR code 2: Access Denied.");
				}
				if (receivePacket.getData()[3] == 3) {
					System.out.println("ERROR code 3: no space on disk or in allocation. Terminating transfer.");
				}
				if (receivePacket.getData()[3] == 4) {
					System.out.println("ERROR code 4: Invalid TFTP operation. Terminating transfer.");
				}
				if (receivePacket.getData()[3] == 5) {
					System.out.println("ERROR code 5: DATA Packet sent to wrong port. Resending last DATA packet.");
					i--;
					continue;
				}
				TFTPClient.finishedRequest = true;
				TFTPClient.changeMode = true;
				break;
			}

			int len = receivePacket.getLength();

			if (verboseMode) {
				System.out.println(host + ": Packet received:");
				System.out.println("Block number: " + receivePacket.getData()[2] + receivePacket.getData()[3]);
				verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
			}
			if (sendPacket.getLength() < 516) {
				System.out.println(host + ": Last packet sent.");
				if (host.equals("Client")) {
					TFTPClient.finishedRequest = true;
					TFTPClient.changeMode = true;
				}
			}
			blockNum++;
		}
		System.out.println("Finished Read");
		if (host.equals("Server")) {
			TFTPClientConnectionThread.doneProcessingRequest = true;
			TFTPClientConnectionThread.establishedCommunications.remove(receivePacket.getSocketAddress().toString());
		}
	}

	/**
	 * Check if the opcode is valid or not
	 *
	 * @param data
	 *            A byte array of the data in a packet
	 * @return boolean The operation is valid or not
	 */
	private boolean isValidOperation(byte[] data) {
		// if not a data packet or an ACK packet, operation is invalid
		if (data[0] != 0 || data[1] != 3 && data[1] != 4) {
			return false;
		}
		return true;
	}

}
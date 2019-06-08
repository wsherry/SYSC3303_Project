import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class TFTPFunctions {

	private static final int TIMEOUT = 4000;
	private DatagramPacket receivePacket, sendPacket;

	ArrayList<byte[]> readFileIntoBlocks(String fileName) {
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

	/**
	 * Converts the blocknumber as an int into a 2 byte array
	 * 
	 * @param blockNum
	 * @return
	 */
	byte[] blockNumBytes(int blockNum) {
		// create the corresponding block number in 2 bytes
		byte[] blockArray = { (byte) (blockNum / 256), (byte) (blockNum % 256) };
		return blockArray;
	}

	void verboseMode(InetAddress inetAddress, int port, int length, byte[] data) {
		System.out.println("Destination Host: " + inetAddress);
		System.out.println("Destination Host Port: " + port);
		System.out.println("Length: " + length);
		System.out.println("Containing: ");
		for (int j = 0; j < length; j++) {
			System.out.println("byte " + j + " " + data[j]);
		}
	}

	void sendPacketFromSocket(DatagramSocket socket, DatagramPacket packet) {
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	void receivePacketFromSocket(DatagramSocket socket, DatagramPacket packet) {
		try {
			// Block until a datagram is received via sendReceiveSocket.
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// client: sendport = -1 or 23 if test mode
	void receiveFiles(String fileName, int sendPort, String host, DatagramSocket socket, boolean testMode,
			boolean requestResponse, boolean verboseMode, int connectionPort) {
		ArrayList<Integer> processedBlocks = new ArrayList<>();
		if (testMode)
			sendPort = 23;
		try {
			if (host == "Server")
				socket.setSoTimeout(TIMEOUT);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

			while (true) {
				byte[] data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				int len = receivePacket.getLength();
				System.out.println(host + ": Waiting for data packet.");
				if (requestResponse) {
					try {
						// Block until a datagram is received via sendReceiveSocket.
						socket.receive(receivePacket);
						if (!testMode)
							sendPort = receivePacket.getPort();
						requestResponse = false;
						connectionPort = receivePacket.getPort();
					} catch (InterruptedIOException io) {
						System.out.println(host + " timed out. resending request.");
						break;
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				} else {
					// clientserverthread goes here first
					try {
						// Block until a datagram is received via sendReceiveSocket.
						socket.setSoTimeout(300000);
						socket.receive(receivePacket);
						socket.setSoTimeout(TIMEOUT);
					} catch (InterruptedIOException io) {
						System.out.println(host + " has exceeded idle time. Cancelling transfer.");
						if (host == "Client") {
							TFTPClient.finishedRequest = true;
							TFTPClient.changeMode = true;
						}
						break;
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
				if (host == "Server") {
					if (TFTPClientConnectionThread.establishedCommunications
							.contains(receivePacket.getSocketAddress().toString())
							&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 2)) {
						System.out.println("Server: Received a duplicate WRQ packet. Ignoring it.");
						continue;
					}
				}

				// Check if packet came from correct source. Send back ERROR packet code 5 if
				// not.
				if (connectionPort != receivePacket.getPort()) {
					System.out.println(
							"Received Packet from unknown source. Responding with ERROR CODE 5 and continuing.");
					byte[] err = new byte[] { 0, 5, 0, 5 };
					DatagramPacket sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(),
							receivePacket.getPort());
					try {
						socket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					continue;
				}

				// Process the received datagram.
				len = receivePacket.getLength();
				data = receivePacket.getData();

				// Check if ERROR packet was received
				if (data[1] == 5 && data[3] == 5) {
					System.out.println("ERROR code 5: ACK Packet sent to wrong port. Waiting for proper DATA.");
					continue;
				}

				System.out.println(host + ": Data Packet received.");

				// Check if it's a duplicate packet. If it is, we still want to send an ACK but
				// not rewrite to the file.
				if (!processedBlocks.contains(data[2] * 10 + data[3])) {
					// This block number has not been processed. Write it to the file.
					try {
						out.write(data, 4, len - 4);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					processedBlocks.add(data[2] * 10 + data[3]);
				} else {
					if (verboseMode) {
						System.out.println(
								host + ": Duplicate data packet received. Ignoring it by not writing it again.");
						// TODO We should send an Nth ACK for the Nth duplicate data packet that was
						// received.
					}
				}

				if (verboseMode) {
					verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
				}

				byte[] ack = new byte[] { 0, 4, data[2], data[3] };
				if (host == "Client") {
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
					if (host == "Client") {
						TFTPClient.finishedRequest = true;
						TFTPClient.changeMode = true;
					} else if (host == "Server") {
						TFTPClientConnectionThread.doneProcessingRequest = true;
						TFTPClientConnectionThread.establishedCommunications
								.remove(receivePacket.getSocketAddress().toString());
					}
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
	 * read files
	 * 
	 * @param filename
	 * @param sendPort
	 * @param receivePacket
	 * @throws UnknownHostException
	 */
	void transferFiles(DatagramSocket socket, DatagramPacket receivePacket, String host, String fileName,
			int sendPort, ArrayList<Integer> processedACKBlocks, boolean verboseMode) {

		int blockNum = 1; // You start at data block one when reading from a server.
		byte[] data = new byte[100];
		if (host == "Server")
			receivePacket.setData(data, 0, data.length);
		else
			receivePacket = new DatagramPacket(data, data.length);

		ArrayList<byte[]> msgBuffer = readFileIntoBlocks(fileName);

		if (host == "Server") {
			try {
				socket.setSoTimeout(TIMEOUT);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
		}

		for (int i = 0; i < msgBuffer.size(); i++) {
			byte[] msg = msgBuffer.get(i);
			byte[] blockNumsInBytes = blockNumBytes(blockNum);
			msg[0] = 0;
			msg[1] = 3;
			msg[2] = blockNumsInBytes[0];
			msg[3] = blockNumsInBytes[1];
			// InetAddress from client class
			if (host == "Client") {
				try {
					sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(TFTPClient.ipAddress),
							sendPort);
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else
				sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), sendPort);

			sendPacketFromSocket(socket, sendPacket);

			if (verboseMode) {
				System.out.println(host + ": Packet sent:");
				verboseMode(sendPacket.getAddress(), sendPacket.getPort(), sendPacket.getLength(), msg);
			}

			System.out.println(host + ": Waiting for packet.");

			try {
				// Block until a datagram is received via sendReceiveSocket.
				socket.receive(receivePacket);
			} catch (InterruptedIOException ie) {
				System.out.println(host + " Timed out. Resending packet.");
				i--;
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// Check if the received packet is a duplicate ACK. If it is, then we should not
			// be re-sending the Nth data packet for the ACK. Sorcerer's Apprentice Bug.
			if (!processedACKBlocks.contains(data[2] * 10 + data[3])) {
				processedACKBlocks.add(data[2] * 10 + data[3]);
			} else {
				if (verboseMode) {
					System.out.println(host
							+ ": Duplicate ACK data packet received. Ignoring it by not re-sending data block number ["
							+ data[2] * 10 + data[3] + "] and waiting for the next datablock.");
				}
			}

			// Check if the received packet is a duplicate read request from a socket and
			// port that is being handled. If so, ignore the packet and continue waiting.
			if (host == "Server") {
				if (TFTPClientConnectionThread.establishedCommunications
						.contains(receivePacket.getSocketAddress().toString())
						&& (receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 1)) {
					System.out.println("Server: Received a duplicate RRQ packet. Ignoring it.");
					i--;
					continue;
				}
			}

			// Check if packet came from correct source. Send back ERROR packet code 5 if
			// not.
			if ((host == "Server" && sendPort != receivePacket.getPort())
					|| (host == "Client" && TFTPClient.connectionPort != receivePacket.getPort())) {
				System.out.println("Received Packet from unknown source. Responding with ERROR code 5 and continuing.");
				byte[] err = new byte[] { 0, 5, 0, 5 };
				sendPacket = new DatagramPacket(err, err.length, receivePacket.getAddress(), receivePacket.getPort());
				sendPacketFromSocket(socket, sendPacket);
				i--;
				continue;
			}

			// Check if packet received is an ERROR Packet
			if (receivePacket.getData()[1] == 5) {
				if (receivePacket.getData()[3] == 5) {
					System.out.println("ERROR code 5: DATA Packet sent to wrong port. Resending last DATA packet.");
					i--;
					continue;
				}
			}

			int len = receivePacket.getLength();

			if (verboseMode) {
				System.out.println(host + ": Packet received:");
				System.out.println("Block number: " + receivePacket.getData()[2] + receivePacket.getData()[3]);
				verboseMode(receivePacket.getAddress(), receivePacket.getPort(), len, data);
			}
			if (sendPacket.getLength() < 516) {
				System.out.println(host + ": Last packet sent.");
				if (host == "Client") {
					TFTPClient.finishedRequest = true;
					TFTPClient.changeMode = true;
				}
			}
			blockNum++;
		}
		System.out.println("Finished Read");
		if (host == "Server") {
			TFTPClientConnectionThread.doneProcessingRequest = true;
			TFTPClientConnectionThread.establishedCommunications.remove(receivePacket.getSocketAddress().toString());
		}
	}

}

import java.io.*;
import java.net.*;

public class TFTPClientConnectionThread implements Runnable {

	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket;

	private DatagramPacket sendPacket;
	private boolean verboseMode = false; //false for quiet and true for verbose

	private Request request;
	
	// Response byte array
	public byte[] response;
	
	String fileNameToWrite;

	public TFTPClientConnectionThread(Request request, DatagramPacket receivePacket, boolean verboseMode) {
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
	
	/**
	 * @return the receivePacket
	 */
	public synchronized DatagramPacket getReceivePacket() {
		return receivePacket;
	}

	/**
	 * @param receivePacket the receivePacket to set
	 */
	public synchronized void setReceivePacket(DatagramPacket receivePacket) {
		this.receivePacket = receivePacket;
	}

	public void run() {
	    byte[] response = this.getReceivePacket().getData();
		send(response);
	}

	private void send(byte[] response) {
        sendPacket = new DatagramPacket(response, response.length,
                              receivePacket.getAddress(), receivePacket.getPort());
        
        int len = sendPacket.getLength();
        if (verboseMode) {
        	System.out.println("TFTPClientConnectionThread: Sending packet:");
        	System.out.println("To host: " + sendPacket.getAddress());
        	System.out.println("Destination host port: " + sendPacket.getPort());
        	System.out.println("Length: " + len);
        	System.out.println("Containing: ");
        	for (int j=0; j<len; j++) {
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

        // We're finished with this socket, so close it.
        sendSocket.close();		
	}
}

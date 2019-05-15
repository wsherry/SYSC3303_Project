import java.io.*;
import java.net.*;

public class TFTPClientConnectionThread implements Runnable {

	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	
	private Request request;
	// responses for valid requests
	public static final byte[] readResp = {0, 3, 0, 1};
	public static final byte[] writeResp = {0, 4, 0, 0};
	
	String fileNameToWrite;

	public TFTPClientConnectionThread(Request request, DatagramPacket receivePacket) {
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}

		this.receivePacket = receivePacket;
		this.request = request;
	}

	public void run() {
	    byte[] response = new byte[4];
		// Create a response.
        if (request == Request.READ) { // for Read it's 0301
           response = readResp;
        } else if (request == Request.WRITE) { // for Write it's 0400
           response = writeResp;
        }

		send(response);
	}

	private void send(byte[] response) {
        sendPacket = new DatagramPacket(response, response.length,
                              receivePacket.getAddress(), receivePacket.getPort());

        System.out.println("TFTPClientConnectionThread: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.println("Containing: ");
        for (int j=0; j<len; j++) {
           System.out.println("byte " + j + " " + response[j]);
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

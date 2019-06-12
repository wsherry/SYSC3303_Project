//package Iteration1;

// TFTPServer.java
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.util.Scanner;

public class TFTPServer {
	// UDP datagram packets and sockets used to send / receive
	private static boolean verboseMode = false; // false for quiet and true for verbose
	private static String serverDirectory = "C:\\Users\\Sherry Wang\\Desktop";

	/*
	 * public TFTPServer() { try { // Construct a datagram socket and bind it to
	 * port 69 // on the local host machine. This socket will be used to // receive
	 * UDP Datagram packets. receiveSocket = new DatagramSocket(69); } catch
	 * (SocketException se) { se.printStackTrace(); System.exit(1); } }
	 */

	/**
	 * Launches the thread for receiving data and continuously runs the server's
	 * GUI.
	 * 
	 */
	public void receiveAndSendTFTP() {
		Scanner sc = new Scanner(System.in);
		String input;

		// create ne thread for receiving
		TFTPClientConnectionThread recieveThread = new TFTPClientConnectionThread(verboseMode, serverDirectory);
		new Thread(recieveThread).start();

		while (true) { // loop forever
			input = "";

			// Server exits loop only when server receives 'quit' input
			if (sc.nextLine().toUpperCase().equals("QUIT")) {
				System.out.println("Server is shutting down.");
				recieveThread.closeSocket();
				break;
			}
			// After a request has been completed the user gets prompted
			// to enter the configuration "menu" again
			// TO BE IMPLEMENTED AFTER FILE TRANSFER IMPLEMENTATION IS COMPLETED
			/*
			 * if (finishedRequest) { //finishedRequest should be true after a file has been
			 * fully read or written System.out.
			 * println("Enter 1 to change configurations or nothing to leave configs unchanged: "
			 * ); while (!(input.equals("1") || input.equals(""))){ input = sc.nextLine();
			 * if (input.equals("1")) configServer(); } finishedRequest = false; //set
			 * finishedRequest to false so the user only gets prompt after a request
			 * finishes }
			 */
		}
	}

	/**
	 * "Menu" for configuring the settings of client application
	 */
	public static void configServer() {
		Scanner sc = new Scanner(System.in); // scanner for getting user's input
		String input = " ";

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

		input = "";
		System.out.println(
				"\nCurrent server directory is: " + (serverDirectory.equals("") ? "undefined" : serverDirectory));
		// option to set the file directory.
		// User must input file directory at the first launch.
		// Once an file directory has been set, the user can enter nothing to keep it
		// unchanged.
		while (input.equals("")) {
			System.out.println("Enter the server of directory or nothing to keep the directory unchanged: ");
			input = sc.nextLine();

			if (input.equals("")) {
				if (serverDirectory.equals(""))
					System.out.println("Server directory has not been entered yet!");
				else
					input = "entered"; // set input to arbitrary string to leave loop
			} else {
				serverDirectory = input;
				System.out.println("Server directory is now: " + serverDirectory);
			}
		}
		System.out.println("\n------------------------------------------------------\nConfigurations are now set up.");
	}

	public static void main(String args[]) throws Exception {
		TFTPServer s = new TFTPServer();
		System.out.println("Welcome to the TFTP server application");
		configServer();
		s.receiveAndSendTFTP();
	}
}
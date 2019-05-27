// TFTPSim.java
// This class is the beginnings of an error simulator for a simple TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  Upon receiving a response, it passes it on to the 
// client.
// One socket (23) is used to receive from the client, and another to send/receive
// from the server.  A new socket is used for each communication back to the client.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim {
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
   private static long delayTime=0;
   private static int packetNumber=0;
   private static boolean verboseMode = true; //false for quiet and true for verbose
   private static Type packetType;
   
   public static enum Type {
	   RRQ, WRQ, REQ, DATA, ACK
   };
   public static enum Mode {
	   NORMAL, LOSS, DELAY, DUPLICATE
   };
   
   private static Mode mode = Mode.NORMAL;
   
   public TFTPSim()
   {
      try {
         // Construct a datagram socket and bind it to port 23
         // on the local host machine. This socket will be used to
         // receive UDP Datagram packets from clients.
         receiveSocket = new DatagramSocket(23);
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets from the server.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void passOnTFTP()
   {
      byte[] data;
      
      int clientPort, j=0, len, packetCount=0;
      InetAddress clientAdress;

      for(;;) { // loop forever
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).
         
         data = new byte[516];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Simulator: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         len = receivePacket.getLength();
         clientPort = receivePacket.getPort();
         if (verboseMode) {
        	 System.out.println("Simulator: Packet received:");
        	 System.out.println("From host: " + receivePacket.getAddress());
        	 System.out.println("Host port: " + clientPort);
        	 System.out.println("Length: " + len);
        	 System.out.println("Containing: " );
        	 // print the bytes
        	 for (j=0;j<len;j++) {
        		 System.out.println("byte " + j + " " + data[j]);
        	 }
         } else {
             System.out.println("Simulator: Packet received.");
         }
         
         clientAdress = receivePacket.getAddress();
         // Form a String from the byte array, and print the string.
         //String received = new String(data,0,len);
         //System.out.println(received);
         
         Type receivedType = null;
         //Save the type of the received packet
         if (data[1]==1) receivedType = Type.RRQ;
         if (data[1]==2) receivedType = Type.WRQ;
         if (data[1]==3) receivedType = Type.DATA;
         if (data[1]==4) receivedType = Type.ACK;
         
         if (packetType == receivedType) packetCount++;
         
         //Perform delayed packet if selected
         //the packet type matches the one chose in configurations
         if (mode == Mode.DELAY && packetCount == packetNumber && receivedType == packetType) {
        	 try {
            	 System.out.println("------------------------------------------------------\nDelaying " + packetType + " packet number " + packetNumber + "...");
        		 System.out.println("------------------------------------------------------");
        		 Thread.sleep(delayTime*1000); //put this thread to sleep to delay the transfer to the server
        	 } catch (InterruptedException e) {
        		 // TODO Auto-generated catch packetCount
        		 e.printStackTrace();
        	 }
         } else if (mode == Mode.DUPLICATE) {
         	//TO BE COMPLETED
         }
         
         if (mode == Mode.LOSS && packetCount == packetNumber && receivedType == packetType) {
        	 System.out.println("------------------------------------------------------\nLosing " + packetType + " packet number " + packetNumber + "...");
    		 System.out.println("------------------------------------------------------");
         } else {	         // Now pass it on to the server (to port 69)
	         // Construct a datagram packet that is to be sent to a specified port
	         // on a specified host.
	         try {
				sendPacket = new DatagramPacket(data, len,
						 	InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	         len = sendPacket.getLength();
	         if (verboseMode) {
	        	 System.out.println("\nSimulator: sending packet.");
	        	 System.out.println("To host: " + sendPacket.getAddress());
	        	 System.out.println("Destination host port: " + sendPacket.getPort());
	        	 System.out.println("Length: " + len);
	        	 System.out.println("Containing: ");
	        	 for (j=0;j<len;j++) {
	        		 System.out.println("byte " + j + " " + data[j]);
	        	 }
	         } else {
	             System.out.println("Simulator: sending packet.");
	         }
	
	         // Send the datagram packet to the server via the send/receive socket.
	         try {
	            sendReceiveSocket.send(sendPacket);
	         } catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	         }
	         
	         if (data[1]==3) { //checking if last data packet
	        	 if (len < 516) {
					System.out.println("Received all data packets");
					configSim();
					continue;
	        	 }
	        	 receivedType = Type.DATA;
	         }
         }
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).

         data = new byte[516];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Simulator: Waiting for packet.");
         try {
            // Block until a datagram is received via sendReceiveSocket.
            sendReceiveSocket.receive(receivePacket);
         } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

    	 len = receivePacket.getLength();
         if (verboseMode) {
        	 // Process the received datagram.
        	 System.out.println("Simulator: Packet received:");
        	 System.out.println("From host: " + receivePacket.getAddress());
        	 System.out.println("Host port: " + receivePacket.getPort());
        	 System.out.println("Length: " + len);
        	 System.out.println("Containing: ");
        	 for (j=0;j<len;j++) {
        		 System.out.println("byte " + j + " " + data[j]);
        	 }
         } else {
             System.out.println("Simulator: Packet received.");        	 
         }
         //Save the type of the received packet
         if (data[1]==1) receivedType = Type.RRQ;
         if (data[1]==2) receivedType = Type.WRQ;
         if (data[1]==3) receivedType = Type.DATA;
         if (data[1]==4) receivedType = Type.ACK;
         
         if (packetType == receivedType) packetCount++;
         
         //Perform delayed packet if selected
         //the packet type matches the one chose in configurations
         if (mode == Mode.DELAY && packetCount == packetNumber && receivedType == packetType) {
        	 try {
        		 System.out.println("------------------------------------------------------\nDelaying " + packetType + " packet number " + packetNumber + "...");
        		 System.out.println("------------------------------------------------------");
        		 Thread.sleep(delayTime*1000); //put this thread to sleep to delay the transfer to the server
        	 } catch (InterruptedException e) {
        		 // TODO Auto-generated catch packetCount
        		 e.printStackTrace();
        	 }
         } else if (mode == Mode.DUPLICATE) {
         	//TO BE COMPLETED
         }        
         
         //Lose the packet if selected
         if (mode == Mode.LOSS && packetCount == packetNumber && receivedType == packetType) {
        	 System.out.println("------------------------------------------------------\nLosing " + packetType + " packet number " + packetNumber + "...");
    		 System.out.println("------------------------------------------------------");
         } else {
    		 // Construct a datagram packet that is to be sent to a specified port
	         // on a specified host.
	         sendPacket = new DatagramPacket(data, receivePacket.getLength(),
	        		 clientAdress, clientPort);
	
	         len = sendPacket.getLength();
	         if (verboseMode) {
	        	 System.out.println( "Simulator: Sending packet:");
	        	 System.out.println("To host: " + sendPacket.getAddress());
	        	 System.out.println("Destination host port: " + sendPacket.getPort());
	        	 System.out.println("Length: " + len);
	        	 System.out.println("Containing: ");
	        	 for (j=0;j<len;j++) {
	        		 System.out.println("byte " + j + " " + data[j]);
	        	 }        	 
	         } else {
	             System.out.println("Simulator: Sending packet.");
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
	
	         if (data[1]==3) { //checking if last data packet
	        	 if (len < 516) {
					System.out.println("Received all data packets");
					configSim();
					continue;
	        	 }
	        	 receivedType = Type.DATA;
	         }
	         
	         System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
	         System.out.println();
         }
         // We're finished with this socket, so close it.
         sendSocket.close();
      } // end of loop
   }

	/**
	 * "Menu" for configuring the settings of client application
	 */
	public static void configSim() {
		Scanner sc = new Scanner(System.in); // scanner for getting user's input
		String input = " ";

		// option to set which test mode
		// loops until valid input (0, 1, 2, 3 or nothing)
		while (!(input.equals("0") || input.equals("1") || input.equals("2") || input.equals("3") || input.equals(""))) { 
			System.out.print("\nEnter '0' to run in normal mode, '1' for simulated loss packet");
			System.out.print(", '2' for simulated delayed packet,\n '3' for simulated duplicate packet ");
			System.out.print("or nothing to stay in " + mode + " mode: ");
			input = sc.nextLine();
			// setting the mode accordingly
			if (input.equals("0")) mode = Mode.NORMAL;
			if (input.equals("1")) mode = Mode.LOSS;
			if (input.equals("2")) mode = Mode.DELAY;
			if (input.equals("3")) mode = Mode.DUPLICATE;			
		}
		System.out.println("Running in " + mode + " mode");
		
		if (mode != Mode.NORMAL) {
			input = "";
	    	//User inputs to specify which type of packet to lose/delay/duplicate
			// loops until valid input (0, 1, or 2)
			while (!(input.equals("0") || input.equals("1") || input.equals("2") || input.equals("3"))) { 
				System.out.print("\nEnter '0' for " + mode + " RRQ packets, '1' for " + mode + " WRQ packets");
				System.out.print(", '2' for DATA packets or '3' for " + mode + " ACK packets: ");
				input = sc.nextLine();
				// setting the mode accordingly
				if (input.equals("0")) packetType = Type.RRQ;
				if (input.equals("1")) packetType = Type.WRQ;
				if (input.equals("2")) packetType = Type.DATA;
				if (input.equals("3")) packetType = Type.ACK;
			}
			
	    	boolean invalid=true; //used to break while loop when the entered input is a valid number

	    	//User inputs to specify which packet number to lose/delay/duplicate
			while (invalid) {
				System.out.print("\nWhich " + packetType + " packet to " + mode + " (" + packetType + " packet number): ");
				input = sc.nextLine();
	   		   	try {
	       		   	packetNumber = Integer.valueOf(input);
	       		   	invalid = false;
	   		   	} catch (NumberFormatException e) {
	   		   		System.out.println("Invalid value entered! Please enter a NUMBER.");
	   		   	}		
			}
			
			System.out.println(packetType + " number " + packetNumber + " will be delayed");
			
	        if (mode == Mode.DELAY) {
	        	invalid=true; //used to break while loop when the entered input is a valid number
	        	while (invalid) {
	        		System.out.println("\nEnter the amount of time to delay packet (seconds): ");
	       		   	input = sc.nextLine();
	       		   	try {
	           		   	delayTime = Long.parseLong(input);
	           		   	invalid = false;
	       		   	} catch (NumberFormatException e) {
	       		   		System.out.println("Invalid value entered! Please enter a NUMBER.");
	       		   	}
	       		}
	       		System.out.println(packetType + " number " + packetNumber + " packet will be delayed by " + delayTime + " seconds.");
	        }
        }

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

		System.out.println("\n------------------------------------------------------\nConfigerations are now set up.");
		System.out.println("------------------------------------------------------");
	}

   public static void main( String args[] )
   {
	   TFTPSim sim = new TFTPSim();
	   System.out.println("Welcome to the TFTP Error Simulation application");
	   configSim();
	   sim.passOnTFTP();
   }
}
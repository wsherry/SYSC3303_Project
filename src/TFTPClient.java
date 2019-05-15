// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;


public class TFTPClient {

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket sendReceiveSocket;
   private static boolean normalMode = true; //true for normal and false for test
   private static boolean verboseMode = false; //false for quiet and true for verbose
   private static String ipAddress = "";
   private static String clientDirectory = "";
   private static String serverDirectory = "";
   private static int count = 0;
   
   // we can run in normal (send directly to server) or test
   // (send to simulator) mode
   public static enum Mode { NORMAL, TEST};

   public TFTPClient()
   {
      try {
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void sendAndReceive()
   {
	   Scanner sc = new Scanner (System.in);
	   //user toggle verbose or quiets mode
	   String input = "";
	   byte readWrite;
	   
	   if (count > 0) {
		   System.out.println("Enter 1 to change configerations or nothing to leave configs unchanged: ");
		   while (!(input.equals("1") || input.equals(""))){
			   if (input.equals("1")) {
				   enterDetails();
			   }
		   }
	   }
		   
	   while (!(input.equals("1") || input.equals("2"))){
		   System.out.println("\nEnter '1' for read or '2' write request: ");
		   input = sc.nextLine();

		   if (input.equals("1")) {
			   readWrite = (byte) 1;
		   } else if (input.equals("2")) {
			   readWrite = (byte) 2;
		   } else {
			   System.out.print(input + " is not 1 or 2.\n");
		   }
	   }
	   
	   input = " ";
	   //while (found){ //keep asking for file until valid file is found in directory
		   System.out.println("\nEnter the name of the file: ");
		   input = sc.nextLine();

		   //CHECK IF FILE EXISTS
	   //}
	   
	   byte[] msg = new byte[100], // message we send
             fn, // filename as an array of bytes
             md, // mode as an array of bytes
             data; // reply as array of bytes
	   String filename, mode; // filename and mode as Strings
	   int j, len, sendPort;
      
      // In the assignment, students are told to send to 23, so just:
      // sendPort = 23; 
      // is needed.
      // However, in the project, the following will be useful, except
      // that test vs. normal will be entered by the user.
      Mode run = Mode.TEST; // change to NORMAL to send directly to server
      
      if (run==Mode.NORMAL) 
         sendPort = 69;
      else
         sendPort = 23;
      System.out.println("Client: creating packet.");
         
      // Prepare a DatagramPacket and send it via sendReceiveSocket
         // to sendPort on the destination host (also on this machine).

         // if i even (2,4,6,8,10), it's a read; otherwise a write
         // (1,3,5,7,9) opcode for read is 01, and for write 02
         // And #11 is invalid (opcode 07 here -- could be anything)

        msg[0] = 0;
        msg[1]=1;
        msg[1]=2;

        // next we have a file name -- let's just pick one
        filename = "test.txt";
        // convert to bytes
        fn = filename.getBytes();
        
        // and copy into the msg
        System.arraycopy(fn,0,msg,2,fn.length);
        // format is: source array, source index, dest array,
        // dest index, # array elements to copy
        // i.e. copy fn from 0 to fn.length to msg, starting at
        // index 2
        
        // now add a 0 byte
        msg[fn.length+2] = 0;

        // now add "octet" (or "netascii")
        mode = "octet";
        // convert to bytes
        md = mode.getBytes();
        
        // and copy into the msg
        System.arraycopy(md,0,msg,fn.length+3,md.length);
        
        len = fn.length+md.length+4; // length of the message
        // length of filename + length of mode + opcode (2) + two 0s (2)
        // second 0 to be added next:

        // end with another 0 byte 
        msg[len-1] = 0;

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host.
        // The arguments are:
        //  msg - the message contained in the packet (the byte array)
        //  the length we care about - k+1
        //  InetAddress.getLocalHost() - the Internet address of the
        //     destination host.
        //     In this example, we want the destination to be the same as
        //     the source (i.e., we want to run the client and server on the
        //     same computer). InetAddress.getLocalHost() returns the Internet
        //     address of the local host.
        //  69 - the destination port number on the destination host.
        try {
           sendPacket = new DatagramPacket(msg, len,
                               InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
           e.printStackTrace();
           System.exit(1);
        }

        if (verboseMode) {
        	System.out.println("Client: sending packet.");
        	System.out.println("To host: " + sendPacket.getAddress());
        	System.out.println("Destination host port: " + sendPacket.getPort());
        	len = sendPacket.getLength();
        	System.out.println("Length: " + len);
        	System.out.println("Containing: ");
        	for (j=0;j<len;j++) {
        		System.out.println("byte " + j + " " + msg[j]);
        	}
        } else {
        	System.out.println("Client: sending packet.");
        }
        
        // Form a String from the byte array, and print the string.
        String sending = new String(msg,0,len);
        System.out.println(sending);

        // Send the datagram packet to the server via the send/receive socket.

        try {
           sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: Packet sent.");

        // Construct a DatagramPacket for receiving packets up
        // to 100 bytes long (the length of the byte array).

        data = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);

        System.out.println("Client: Waiting for packet.");
        try {
           // Block until a datagram is received via sendReceiveSocket.
           sendReceiveSocket.receive(receivePacket);
        } catch(IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        // Process the received datagram.
        if (verboseMode) {
        	System.out.println("Client: Packet received:");
        	System.out.println("From host: " + receivePacket.getAddress());
        	System.out.println("Host port: " + receivePacket.getPort());
        	len = receivePacket.getLength();
        	System.out.println("Length: " + len);
        	System.out.println("Containing: ");
        	for (j=0;j<len;j++) {
        		System.out.println("byte " + j + " " + data[j]);
        	}
        } else {
            System.out.println("Client: Packet received:");
        }
        
        System.out.println();      

      // We're finished, so close the socket.
      sendReceiveSocket.close();
   }
   
   /**
    * 
    */
   public static void enterDetails () {
	   Scanner sc = new Scanner (System.in);
	   //user toggle verbose or quiets mode
	   String input = " ";
	   
	   while (!(input.equals("1") || input.equals("2") || input.equals(""))){
		   System.out.println("\nEnter '1' to run in normal mode or '2' for test mode ");
		   System.out.print("or nothing to stay in " + (normalMode ? "normal" : "test") + " mode: ");
		   input = sc.nextLine();

		   if (input.equals("1")) normalMode = true;
		   if (input.equals("2")) normalMode = false;
	   }
	   
	   input = " ";
	   while (!(input.equals("1") || input.equals(""))){
		   System.out.println("\nEnter '1' to toggle between quiet and verbose mode ");
		   System.out.print("or nothing to stay in " + (verboseMode ? "verbose" : "quiet") + " mode: ");
		   input = sc.nextLine();

		   if (input.equals("1")) {
			   verboseMode = verboseMode ? false : true;
		   }
	   }
	   
	   input = "";
	   while (input.equals("")){
		   System.out.println("\nEnter the IP address of server or nothing to keep IP address unchanged: ");
		   input = sc.nextLine();
		   
		   if (input.equals("")) {
			   if (ipAddress.equals("")) {
				   System.out.print("IP has not been entered yet!");
			   }else {
				   input="entered";
			   }
		   } else {
			   ipAddress = input;
		   }
	   }
	   
	   input = "";
	   while (input.equals("")){
		   System.out.println("\nEnter the client of directory or nothing to keep the directory unchanged: ");
		   input = sc.nextLine();
		   
		   if (input.equals("")) {
			   if (clientDirectory.equals("")) {
				   System.out.print("Client directory has not been entered yet!");
			   } else {
				   input="entered";
			   }
		   } else {
			   clientDirectory = input;
		   }
	   }
	   
	   input = "";
	   while (input.equals("")){
		   System.out.println("\nEnter the server of directory or nothing to keep the directory unchanged: ");
		   input = sc.nextLine();
		   
		   if (input.equals("")) {
			   if (serverDirectory.equals("")) {
				   System.out.print("Server directory has not been entered yet!");
			   } else {
				   input="entered";
			   }
		   } else {
			   serverDirectory = input;
		   }
	   }
	   System.out.println("Configerations are now set up.");
   }

   public static void main(String args[])
   {
      TFTPClient c = new TFTPClient();
      System.out.println("Welcome to the TFTP client application");
      enterDetails ();
      c.sendAndReceive();
   }
}



// TFTPServer.java
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.io.*;
import java.net.*;


public class TFTPServer {
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket receivePacket;
   private DatagramSocket receiveSocket;
   
   public TFTPServer()
   {
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

   public void receiveAndSendTFTP() throws Exception
   {
      byte[] data = new byte[4];     
      Request req; // READ, WRITE or ERROR
      
      int len, j=0, k=0;

      for(;;) { // loop forever
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).
         
         data = new byte[100];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Server: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         System.out.println("Server: Packet received:");
         System.out.println("From host: " + receivePacket.getAddress());
         System.out.println("Host port: " + receivePacket.getPort());
         len = receivePacket.getLength();
         System.out.println("Length: " + len);
         System.out.println("Containing: " );
         
         // print the bytes
         for (j=0;j<len;j++) {
            System.out.println("byte " + j + " " + data[j]);
         }

         // Form a String from the byte array.
         String received = new String(data,0,len);
         System.out.println(received);

         // If it's a read, send back DATA (03) block 1
         // If it's a write, send back ACK (04) block 0
         // Otherwise, ignore it
         if (data[0]!=0) req = Request.ERROR; // bad
         else if (data[1]==1) req = Request.READ; // could be read
         else if (data[1]==2) req = Request.WRITE; // could be write
         else req = Request.ERROR; // bad

         if (req!=Request.ERROR) { // check for filename
             // search for next all 0 byte
             for(j=2;j<len;j++) {
                 if (data[j] == 0) break;
            }
            if (j==len) req=Request.ERROR; // didn't find a 0 byte
            if (j==2) req=Request.ERROR; // filename is 0 bytes long
            // otherwise, extract filename
         }
 
         if(req!=Request.ERROR) { // check for mode
             // search for next all 0 byte
             for(k=j+1;k<len;k++) { 
                 if (data[k] == 0) break;
            }
            if (k==len) req=Request.ERROR; // didn't find a 0 byte
            if (k==j+1) req=Request.ERROR; // mode is 0 bytes long
         }
         
         if(k!=len-1) req=Request.ERROR; // other stuff at end of packet        
         
         if (req == Request.ERROR) { // it was invalid, close socket on port 69 (so things work properly next time) and quit
            receiveSocket.close();
            throw new Exception("Not yet implemented");
         }

      	 // Create a new client connection thread for each connection with the client.
         Runnable newClient = new TFTPClientConnectionThread(req, receivePacket);
         new Thread(newClient).start();
      } // end of loop
   }

   public static void main( String args[] ) throws Exception
   {
      TFTPServer s = new TFTPServer();
      s.receiveAndSendTFTP();
   }
}



import java.io.*;
import java.net.*;
import java.util.Arrays;

//import TFTPClient.RequestType;


public class TFTPClientConnectionThread implements Runnable {

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private boolean verboseMode = false; //false for quiet and true for verbose
	
	private Request request;
	// responses for valid requests
	public static final byte[] readResp = {0, 3, 0, 1};
	public static final byte[] writeResp = {0, 4, 0, 0};
	
	String fileName;
	
	//File path DESKTOP
	//TODO Must be changed based on system
	private static final String SERVERDIRECTORY = "C:\\Users\\Sherry Wang\\Desktop";


	public TFTPClientConnectionThread(boolean verboseMode) {
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
	}
	
	/**
	 * Closes the receive socket
	 */
	void closeSocket() {
		receiveSocket.close();
	}

	public void run() {
		int len, j=0, k=0;

		while(true) { // loop forever
			byte[] data = new byte[100];
	        receivePacket = new DatagramPacket(data, data.length);

	        System.out.println("------------------------------------------------------");
	        System.out.println("Type 'quit' to shutdown.");
	        try {
				System.out.println("Server (" + InetAddress.getLocalHost().getHostAddress() + ") : Waiting for packet.");
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        
	        // Block until a datagram packet is received from receiveSocket.
	        try {
	        	receiveSocket.receive(receivePacket);
	        } catch (SocketException exception) { //the socket has been closed for server shut down
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
	        	System.out.println("Containing: " );
	        	// print the bytes
	        	for (j=0;j<len;j++) {
	        		System.out.println("byte " + j + " " + data[j]);
	        	} 
	        } else {
	            System.out.println("Server: Packet received.");
	        }
	         
	         // Form a String from the byte array.
	         String received = new String(data,0,len);
	         fileName = new String(receivePacket.getData(), 0, receivePacket.getLength());
			 fileName = fileName.split("\0")[1].substring(1);
	         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!");
	         
	         System.out.println(fileName);
	         System.out.println(received);

	         // If it's a read, send back DATA (03) block 1
	         // If it's a write, send back ACK (04) block 0
	         // Otherwise, ignore it
	         if (data[0]!=0) request = Request.ERROR; // bad
	         else if (data[1]==1) request = Request.READ; // could be read
	         else if (data[1]==2) request = Request.WRITE; // could be write
	         else request = Request.ERROR; // bad

	         if (request!=Request.ERROR) { // check for filename
	             // search for next all 0 byte
	             for(j=2;j<len;j++) {
	                 if (data[j] == 0) break;
	            }
	            if (j==len) request=Request.ERROR; // didn't find a 0 byte
	            if (j==2) request=Request.ERROR; // filename is 0 bytes long
	            // otherwise, extract filename
	         }
	 
	         if(request!=Request.ERROR) { // check for mode
	             // search for next all 0 byte
	             for(k=j+1;k<len;k++) { 
	                 if (data[k] == 0) break;
	            }
	            if (k==len) request=Request.ERROR; // didn't find a 0 byte
	            if (k==j+1) request=Request.ERROR; // mode is 0 bytes long
	         }
	         
	         if(k!=len-1) request=Request.ERROR; // other stuff at end of packet        

	         if (request == Request.ERROR) { // it was invalid, close socket on port 69 (so things work properly next time) and quit
		            receiveSocket.close();
		            try {
						throw new Exception("Not yet implemented");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		         }
	         // Create a response.
	         if (request == Request.READ) { 
	        	 //create new thread for sending data
	        	 Runnable readReqThread = new TFTPsendThread(request, receivePacket, verboseMode);
	             new Thread(readReqThread).start();
	             
	         } else if (request == Request.WRITE) {
	        	 //create new thread for sending data
	        	 Runnable writeReqThread = new TFTPsendThread(request, receivePacket, verboseMode);
	             new Thread(writeReqThread).start();
	         }
	         
/*	         if (request == Request.ERROR) { // it was invalid, close socket on port 69 (so things work properly next time) and quit
	            receiveSocket.close();
	            try {
					throw new Exception("Not yet implemented");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	         }*/
		} // end of loop
	}
	
	public class TFTPsendThread implements Runnable {

		private DatagramSocket sendSocket;
		private DatagramPacket receivePacket;
		private DatagramPacket sendPacket;
		private boolean verboseMode = false; //false for quiet and true for verbose


		private Request request;
		// responses for valid requests
		String fileNameToWrite;

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
				response = readResp;
			} else if (request == Request.WRITE) { // for Write it's 0400
				response = writeResp;
			}

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
			
			if (request == Request.READ) {
				transferFiles(fileName, sendSocket.getLocalPort());
			} else if (request == Request.WRITE){
				receiveFiles(fileNameToWrite, sendSocket.getLocalPort());
			}

			// We're finished with this socket, so close it.
			sendSocket.close();		
		}
		
		
		//write to file
		public void receiveFiles(String fileName, int sendPort) {

			byte[] data = new byte[516];
			int len = 516;
			receivePacket = new DatagramPacket(data, data.length);
			
			BufferedOutputStream out;
			try {
				out = new BufferedOutputStream(new FileOutputStream(SERVERDIRECTORY + "\\"+ fileName));
				while (true) {
					System.out.println("Server: Waiting for data packet.");

						try {
							// Block until a datagram is received via sendReceiveSocket.
							receiveSocket.receive(receivePacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}

						// Process the received datagram.
						len = receivePacket.getLength();
						data = receivePacket.getData();

						System.out.println("Server: Data Packet received.");

						if (verboseMode) {
							System.out.println("From host: " + receivePacket.getAddress());
							System.out.println("Host port: " + receivePacket.getPort());
							System.out.println("Length: " + len);
							System.out.println("Containing: ");
							for (int j = 0; j < len; j++) {
								System.out.println("byte " + j + " " + data[j]);
							}
						}
						
						try {
							out.write(data, 4, data.length - 4);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						
						if (len < 516) System.out.println("Received all data packets");
					}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			
			}

		public void transferFiles(String filename, int sendPort) {
		   int block_num = 0;
		   byte[] msg = new byte[516];
		   byte[] data = new byte[100];
	       receivePacket = new DatagramPacket(data, data.length);
		   msg[0] = 0;
		   msg[1] = 3;
		   msg[2] = (byte) block_num;
		   byte[] dataBuffer = new byte[516];
		   try {
			   BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));   

			   while(bis.read(dataBuffer) != -1) {
				   sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
				   try {
			           sendSocket.send(sendPacket);
			        } catch (IOException e) {
			           e.printStackTrace();
			           System.exit(1);
			        }
				   try {
			           // Block until a datagram is received via sendReceiveSocket.
			           receiveSocket.receive(receivePacket);
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
					
		           //byte[] ack = new byte[] {0,4,0,0};
		           //boolean verified = Arrays.equals(ack, data);
			   }
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	   }
		
		
		

	}
}


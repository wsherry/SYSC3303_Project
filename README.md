# SYSC3303_Project
Group Members: 
Pragya Singh 100974148
Sherry Wang 101172387
Ben Bozec 101034327
Alan Lin 101036660
Alexei Tchekansky 100324946

Contributions: 

Iteration 1:

Alexei Tchekansky:
-Mulithreading between the client and the server
-UML Class Diagrams
-Testing

Ben Bozec:
-Implemented the "shut down" functioanlity that shut down a server

Alan Lin:
-Command line setup in order to encapsulate a simple userinterface

Pragya Singh:
-Steady state file tranfers (server side)
-State diagrams
-Read me file

Sherry Wang:
-Steady state file tranfers (client side) 
-Use case diagrams
-Testing

Iteration 2: 

Sherry Wang:
-File transfer Testing( mainly large files)
-File tranfer testing for multiple files
-Changed user inputted IP address
-Changing client and server connection formating and function issues 

Alexei Tchekansky:
-Fixing the server from iteration 1 so that it continues running and waiting for requests
-UML Class diagrams 
-Duplicate packet functionality and handling duplicates in the client and server.

Pragya Singh: 
-Sequence diagrams 
-State diagrams 
-Read me 

Alan Lin:
-Added an UI to the error simulator (TFTPSim.java) that allows the user to configure the options for the error simulation.
-Added the following modes normal, loss and delayed.

Ben Bozec:
-Timeout functionality (can be seen in the features) 

Iteration 3: 

Sherry Wang:
-Update the following diagrams 
	-UML diagrame, 
	-Use Cases
	-Sequence diagrams
	-State machine diagram	
	
Alexei Tchekansky:
-Created the simulation for error code 5 in Error simulator

Ben Bozec:
-Error code 5 handling for the client and server

Alan Lin:
-Update Error Simulation to have a seperate drop down for error 4 simulations
-Update Error Simulation to have a seperate drop down for error 5 simulations
-Bug fixes, including issues with send and receiving data packets and changing the client connection to use the same port for sending and receiving data

Pragya Singh:
-Read me 
-Created the error simulator for the four cases for error code 4 (Opcode, Mode , Block number and filename validity)

Iteration 4: 

Sherry Wang: 
-Refactored the code
-Implemented error code 2

Alexei Tchekansky:
-Error handling for error code 1 
-Fixed the simulation ACK bug from iteration 3.

Ben Bozec:
-Error handling for Error code 3 (server and client)

Alan Lin:
-Handling of error code 6
-Fixed the bug with transferring files of 512 byte multiple
-Error code 4 bugs

Pragya Singh:
- UML Class diagram 
- Sequence Diagram 
- State Machine Diagrams 
- Use Case Diagrams 
- Read me 

Iteration 5: 
Alexei Tchekansky:
Implemented the code to handle missing parts of error code 4.

Ben Bozec:
Implemented the code to handle missing parts of error code 4.

Alan Lin:
Fixed a bug that was not allowing duplicate packets from being handled properly. 7=6yWrote out detailed setup instructions

Pragya Singh:
Wrote, edited and formatted the report. Redo some diagrams as they were incorrect and or unclear.Added in class diagram for Iteration 5.

Sherry Wang: 
Fixed a bug that was not allowing duplicate packets from being handled properly. 

Added Feature since Iteration 4:
- Can handle multiple files being send of 512 bytes

Added Features since Iteration 3:
- Deal with I/O errors that can occur 
- Preparing transmitte, recieve and handeling TFTP Error packets 
- The packets error codes we are dealing with is 1,2,3,6
		-Error code 1: File not Found 
		-Error code 2: Access Violation 
		-Error code 3: Disk Full 
		-Error code 6: File already exists 

Added Features Since Iteration 2:
-Sim starts a new thread, with a different TID and sends the packet to either client or server
- User is able to select error code 5
- User is able to select between four different scnerios for error code 4 
	-Opcode Validity
	-Block Number Validity
	-Checking for Valid Modes
	-Empty or Null File names
	
Added Features Since iteration 1:
-File tranfer of larger and multiple files 
-Changed inputter IP address for the user 
-Changed formatting issues to make the code clener 
-Fixed the server so that it continues to run after waiting for a request.
-Duplicate packed functionalitu 
-UI to the error simualator that allows the user to configure between differnt options 
-When set time has passed (currently 1 second), Client will resend read/write 
request if no response is received.
-During transfer, if the side that sends the data packets does not receive an ACK packet 
within 1 second, the previous data packet is resent 
-During transfer, if the server does not receive a data packet within
a reasonably long period of time (currently 5 minutes) the server ends the transfer and 
closes the connection (Idle timeout not implemented for Client yet)


Project Overview for Iteration 0 and 1:
The programs allows clients to establish WRQ connections and RRQ connections with the server. This is done due to the implementation of steady-state file transfer between the client and the server. Ever RRQ the server will respond with specific data block of 0-1 bytes in order to see if a file was written or read from properly respond witha ACK block 0. For every WRQ request ther sever will The error simulator will just pass on packets (client to server, and server to client).The server has a new thread that represents a new client and this connection thread is used to connect with the client. The error simulator in this iteration only passes a package between the client and the server, in this iteration no error simulation is done. 

Project Overview for Iteration 2:
The server and client are able to now handle network errors ( lost, delayed, duplicaiton). While having the side sending the data files also be able to have a timeout and retransmit.The UI will also allow the user to pick particular modes they would like to configure the program to run in. 

Project Overview for Iteration 3: 
Implementing errors that can occur in aTFTP packets that is received.   TFTP
ERROR packets  should be able to prepared, transmitted,received, and handled (Error Code 4, 5) must be prepared, transmitted,
received, and handled. There will be an assumption that no input file errors, in this iteration. 

Project Overview for Iteration 4:
Add support for ERROR packets both the client and server code
from Iteration #3. Must catch exceptions thrown by your Java read/write
code and translate the exceptions to the appropriate TFTP error codes. Direct testing should be functional for all the errors withouth having to update the error simulator.

Project Overview for Iteration 5:
Handle a scenario where the client and the server can reside on different computers.

Errors Iteration 2:
TFTPClientConnection currently doesn’t use port 69 for receiving data

Errors Iteration 5: 
Extra boundary cases eith error code 4 that werent considered ( missed in the presentation demo)

Overview of each class:

Request.java: A basic enum class that is used to identify the services a client needs the server to provide. the three enum that are currently being used as identifications of the tasks are read, write and error.

Server.java :
The server receives a read or write packet from a client and sends back the appropriate response without any actual file transfer

Client.java :
Implements the shut down method that forces the server to finsh tranfering all current files that are currnetly in progress with continuing to create new connections with other clients. All has a simple user interface that can be seen in the command line.

FTPClientConnectionThread.java
Creates connections between different clients and servers to process requests such as read and write and errors

TFTPSim.java
The simulator receives a read or write packet from a client and passes it on to the server.  Upon receiving a response, it passes it on to the client.


Installation/running instructions:

Iteration 1: 

1) Run server.java as java application and follow the command line prompts. Copy the displayed address after configuration is complete.
2) Run TFTPSim.java as java application. *Not necessary if only running in normal mode.
3) Run client.java as java application and follow the command line prompts. When prompted for the IP adress, paste the IP adress that was copied from the server.
** run in the above order

Iteration 2: 
Installation/setup instructions:

1. Run server.java as java application and follow the command line prompts. Copy/note the displayed address after configuration is complete.
2. Run TFTPSim.java as java application. *Not necessary if only running in normal mode.
	2.1 If running TFTPSim.java, follow the command line prompts. If you a mode that's not NORMAL mode, you will be prompted for more simulation configurations.
3. Run client.java as java application and follow the command line prompts. When prompted for the IP address, paste the IP address that was copied/noted from the server.
** run in the above order**

Iteration 3:
1. Run server.java as java application and follow the command line prompts. This incluldes running in quiet or verbose mode and setting the directory of the server (i.e. "C:\\Users\\alanlin\\Desktop"). Copy/note the displayed address after configuration is complete.
2. Run TFTPSim.java as java application. *Not necessary if only running in normal mode.
	2.1 If running TFTPSim.java, follow the command line prompts. If you a mode that's not NORMAL mode, you will be prompted for more simulation configurations.
		2.2 If simulating an error (not normal mode) the command line will prompt for which packet type, and number to perform the error simulation on.
		2.2.1 If simulating erro code 4, a sub menu will be printed with options for error 4 cases.
3. Run client.java as java application and follow the command line prompts. This incluldes running in normal or test mode, quiet or verbose mode, setting the IP address of the destination (server's IP)
and setting the directory of the client (i.e. "M:\SYSC3303_Project"). When prompted for the IP address, paste the IP address that was copied/noted from the server.
** run in the above order**

Iteration 4:
**ignore the .class files**
1. In this directory, there's a folder called "Iteration4_Diagrams", which contains the associated diagrams for this program.
and a zip file called "Team8_Project_IT4" which contains all the Java code. Unzip this file.
2. After unzipping the "Team8_Project_IT4", in Eclipse, click "Open Project from file system" and select the directory with the unzipped files.
3. Once all the files are loaded, first run server.java as java application and follow the command line prompts. These prompts are explained in 3.1.x.
	3.1.1 Option to run in quiet or verbose mode. Verbose mode will print out the packets that are being recieved and sent, quiet does not.
	3.1.2 Option setting the IP address of the destination (server's IP address). If running with error sim, you will not be prompted for this.This incluldes running in quiet or verbose mode and setting the directory of the server (i.e. "C:\\Users\\alanlin\\Desktop"). Copy/note the displayed address after configuration is complete.
4. If running in test mode only, run TFTPSim.java as java application. Otherwise, this is not necessary if only running in normal mode.
	4.1 If running TFTPSim.java, follow the command line prompts. If running in a mode that's not NORMAL mode, you will be prompted for more simulation configurations.
		4.2 If simulating an error (not normal mode) the command line will prompt for which packet type and number to perform the error simulation on. The modes are explained in 4.2.x.
			4.2.1 LOSS packet mode: the error sim will not forward the specified packet to the recipient.
			4.2.2 DELAYED packet mode: the error sim will delay the specified packet that's being sent to the recipient.
			4.2.3 DUPLICATE packet mode: the error sim will forward the specified packet twice.
			4.2.4 ILLEGAL TFTP OPERATION mode: the error sim will create packets that will generate an error code 4 (modes listed in 4.2.4.x) in either client or server.
				4.2.4.1 Error in OPCODE: the error sim will change the opcode of the specified packet to "99".
				4.2.4.2 Error in BLOCK NUMBER: the error sim will add 3 to the block number of the specified packet.
				4.2.4.3 Error in FILE NAME: the error sim will remove the name from the specified packet (request packet).
				4.2.4.4 Error in MODE: the error sim will change the mode to "pctect" (valid is "octect") of the specified packet.
			4.2.5 UNKOWN TRANSFER ID: the error sim will send the specified packet from another socket (different from the socket for estiablishing connection).
		4.3.1 If simulating error code 4, a sub menu will be printed with options for error 4 cases.
5. Run client.java as java application and follow the command line prompts. These prompts are explained in 5.1.x.
	5.1.1 Option to run in normal or test mode. Normal mode is only for running without the error sim.
	5.1.2 Option to run in quiet or verbose mode. Verbose mode will print out the packets that are being recieved and sent, quiet does not.
	5.1.3 Option setting the IP address of the destination (server's IP address). If running with error sim, you will not be prompted for this.
	5.1.4 Setting the directory of the client (i.e. "M:\SYSC3303_Project").
** run in the above order**

Iteration 5 Instructions:
1.	In this directory, there's a folder called "Iteration4_Diagrams", which contains the associated diagrams for this program. and a zip file called "Team8_Project_IT4" which contains all the Java code. Unzip this file.
2.	After unzipping the "Team8_Project_IT4", in Eclipse, click "Open Project from file system" and select the directory with the unzipped files.
3.	Once all the files are loaded, first run server.java as java application and follow the command line prompts. These prompts are explained in 3.1.x. 
	3.1.1 Option to run in quiet or verbose mode. Verbose mode will print out the packets that are being received and sent, quiet does not. 
	3.1.2 Option setting the IP address of the destination (server's IP address). If running with error sim, you will not be prompted for this. This includes running in quiet or verbose mode and setting the directory of the server (i.e. "C:\Users\alanlin\Desktop"). Copy/note the displayed address after configuration is complete.
4.	If running in test mode only, run TFTPSim.java as java application. Otherwise, this is not necessary if only running in normal mode. 
	4.1 If running TFTPSim.java, follow the command line prompts. If running in a mode that's not NORMAL mode, you will be prompted for more simulation configurations. 
	4.2 If simulating an error (not normal mode) the command line will prompt for which packet type and number to perform the error simulation on. The modes are explained in 4.2.x.
 		4.2.1 LOSS packet mode: the error sim will not forward the specified packet to the recipient. 
		4.2.2 DELAYED packet mode: the error sim will delay the specified packet that's being sent to the recipient.
		4.2.3 DUPLICATE packet mode: the error sim will forward the specified packet twice. 
		4.2.4 ILLEGAL TFTP OPERATION mode: the error sim will create packets that will generate an error code 4 (modes listed in 4.2.4.x) in either client or server. 
		4.2.4.1 Error in OPCODE: the error sim will change the opcode of the specified packet to "99". 
		4.2.4.2 Error in BLOCK NUMBER: the error sim will add 3 to the block number of the specified packet. 
		4.2.4.3 Error in FILE NAME: the error sim will remove the name from the specified packet (request packet). 
		4.2.4.4 Error in MODE: the error sim will change the mode to "octect" (valid is "octect") of the specified packet. 
		4.2.5 UNKOWN TRANSFER ID: the error sim will send the specified packet from another socket (different from the socket for establishing connection). 
	4.3.1 If simulating error code 4, a sub menu will be printed with options for error 4 cases.

5.	Run client.java as java application and follow the command line prompts. These prompts are explained in 5.1.x. 
	5.1.1 Option to run in normal or test mode. Normal mode is only for running without the error sim. 
	5.1.2 Option to run in quiet or verbose mode. Verbose mode will print out the packets that are being received and sent, quiet does not. 
	5.1.3 Option setting the IP address of the destination (server's IP address). If running with error sim, you will not be prompted for this. 
	5.1.4 Setting the directory of the client (i.e. "M:\SYSC3303_Project"). ** run in the above order**


KEY NOTE: From Iteration 1-4 we had encapsulated the code to be able to handle a scenario where the client and the server can reside on different computers.

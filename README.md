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

Errors Iteration 2:
TFTPClientConnection currently doesn’t use port 69 for receiving data


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


Installation/setup instructions:

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
3. Run client.java as java application and follow the command line prompts. When prompted for the IP adress, paste the IP adress that was copied/noted from the server.
** run in the above order**

Iteration 3:
Follow the screen promt

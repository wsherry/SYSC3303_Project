# SYSC3303_Project
Group Members: 
Pragya Singh 100974148
Sherry Wang 101172387
Ben Bozec 101034327
Alan Lin 101036660
Lexa Minsky 100324946

Contributions: 

Lexa Minsky:
-Mulithreading between the client and the server
-UML Class Diagrams

Ben Bozec:
-Implemented the "shut down" functioanlity that shut down a server

Alan Lin:
-Command line setup in order to encapsulate a simple userinterface

Pragya Singh:
-Steady state file tranfers 
-State diagrams
-Read me file

Sherry Wang:
-Steady state file tranfers 
-Use case diagrams

Project Overview for Iteration 0 and 1:
The programs allows clients to establish WRQ connections and RRQ connections with the server. This is done due to the implementation of steady-state file transfer between the client and the server. Ever RRQ the server will respond with specific data block of 0-1 bytes in order to see if a file was written or read from properly respond witha ACK block 0. For every WRQ request ther sever will The error simulator will just pass on packets (client to server, and server to client).


Overview of each class:

Server.java 

Client.java 

intermdiateHost.java


Installation/setup instructions:
1) run server.java as java application
2) run intermediateHost.java as java application
3) run client.java as java application
** run in the above order

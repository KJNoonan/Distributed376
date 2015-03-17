# Distributed376
Socket File Storage System
This program is a server client file application that sends Client Files to a Server and then can request files to pull from
the server.
This program was meant for running on the command line. Simply recompile with javac and run with the usage statement.

USAGE:
Client: java driver -c [IP Address] port [-v]
Server: java driver -s port [-v]
Note: you may need to run the server like this:
java -Xms1024M -Xms1024M driver -s port [-v]
to avoid the server running out of memory

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class driver {
	public final static int CONNECTIONS = 5;//allows the max number of connections
	/*****************************
	 * Runs the main script for the Distributed system
	 * */
	public static void main(String args[]) throws IOException {
		int a = args.length;
		InetAddress ip = null;
		int port = 0;
		boolean verbose;

		if ((a < 2) || (a > 4)) {
			printUsage();
			System.exit(0);
		}
		if (args[a - 1].equals("-v")) {
			verbose = true;
		} else {
			verbose = false;
		}
		if (args[0].equals("-c")) {//runs the client
			if (a == 2) {
				ip = InetAddress.getLoopbackAddress();
				port = Integer.parseInt(args[1]);
			} else if (verbose && a == 3) {
				ip = InetAddress.getLoopbackAddress();
				port = Integer.parseInt(args[1]);
			} else if (!verbose && a == 3) {
				ip = InetAddress.getByName(args[1]);
				port = Integer.parseInt(args[2]);
			} else if (verbose && a == 4) {
				ip = InetAddress.getByName(args[1]);
				port = Integer.parseInt(args[2]);
			} else {
				printUsage();
			}
			Client c = new Client(ip, port, verbose);
			Thread t = new Thread(c);
			t.start();
		} else if (args[0].equals("-s")) {//run a server
			port = Integer.parseInt(args[1]);

			ServerSocket sSocket = new ServerSocket(port);
			System.out.println("Listening on port " + port + "...");
			LinkedList<Thread> threads = new LinkedList<Thread>();
			while (true) {
				Socket client = sSocket.accept();
				Server s = new Server(client, verbose);
				Thread t = new Thread(s);
				t.start();
				if(threads.size() >= CONNECTIONS){
					try {
						threads.pop().join();
					} catch (InterruptedException e) {
						//move on if interrupted
					}
				}
			}
		} else {
			printUsage();
		}

	}
	/*******************************
	 * Prints out a statement telling the user how to run the commands
	 * */
	public static void printUsage() {
		System.out.println("Usage Statement");
		System.out.println("Client: java driver -c [IP Address] port [-v]");
		System.out.println("Server: java driver -s port [-v]");
		System.out.println("Note: you may need to run the server like this:");
		System.out.println("java -Xms1024M -Xms1024M driver -s port [-v]");
		System.out.println("to avoid the server running out of memory");
	}
}

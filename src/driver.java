import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class driver {
	public static void main(String args[]) throws IOException {
		int a = args.length;
		InetAddress ip = null;
		int port = 0;
		boolean verbose;
		
		if((a < 2)||(a > 4)){
			printUsage();
			System.exit(0);
		}
		if(args[a-1].equals("-v")){
			verbose = true;
		}else{
			verbose = false;
		}
		if(args[0].equals("-c")){
			if(a == 2){
				ip = InetAddress.getLoopbackAddress();
				port = Integer.parseInt(args[1]);
			}else if(verbose && a == 3){
				ip = InetAddress.getLoopbackAddress();
				port = Integer.parseInt(args[1]);
			}else if(!verbose && a == 3){
				ip = InetAddress.getByName(args[1]);
				port = Integer.parseInt(args[2]);
			}else if(verbose && a == 4){
				ip = InetAddress.getByName(args[1]);
				port = Integer.parseInt(args[2]);			
			}else{
				printUsage();
			}
			Client c = new Client(ip, port, verbose);
			Thread t = new Thread(c);
			t.start();
		}else if( args[0].equals("-s")){
			port = Integer.parseInt(args[1]);
			Server s = new Server(port, verbose);
			Thread t = new Thread(s);
			t.start();
		}else{
			printUsage();
		}
		
	}
	public static void printUsage(){
		System.out.println("Usage Statement");
		System.out.println("Client: java driver -c [IP Address] port [-v]");
		System.out.println("Server: java driver -s port [-v]");
	}
}
			
			
		
		
	


	

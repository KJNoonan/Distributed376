import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.io.DataOutputStream;
import java.io.DataInputStream;

public class Client implements Runnable {
	private Hashtable<String, Long> requestList;
	private Socket cSocket;
	private DataInputStream in;
	private DataOutputStream out;
	// files to send to client
	private HashMap<String, Long> serverFiles;
	private String folder;
	// files to retrieve from client
	private HashMap<String, Long> clientFiles;
	private LinkedList<String> requestedFiles;
	BufferedReader br;
	String separator;
	private boolean verbose;

	// make a client object that'll drive the server
	public Client(InetAddress ip, int port, boolean v) throws IOException {
		verbose = v;
		String os = System.getProperty("os.name");
		if (os.equals("Linux")) {
			separator = "/";
		} else {
			separator = "\\";
		}
		folder = separator + "clientFiles";
		folder = System.getProperty("user.dir") + folder;

		FileScan f = new FileScan(folder);
		f.scan();
		clientFiles = f.returnMap();
		// create a Socket with server and open streams
		Socket client = new Socket(ip, port);
		in = new DataInputStream(client.getInputStream());
		out = new DataOutputStream(client.getOutputStream());
		br = new BufferedReader(new InputStreamReader(System.in));
	}

	/**************************************************************************
	 * Gets username and password from user and sends to server to authenticate
	 * 
	 * @return 0 for not signed in 1 for signed in
	 * */
	private int signIn() throws IOException {
		System.out
				.print("Please enter your Username(Enter '~' to create a new user): ");
		String usr = br.readLine();
		out.writeUTF(usr);
		if (usr.equals("~")) {
			int test = createNewUser();
			while (test != 1) {
				System.out.println("UserName taken");
				test = createNewUser();
			}
			if (verbose) {
				System.out.println("Account created!");
			}
			return 0;
		}

		if (in.readUTF().equals("notok")) {
			System.out.println("Username not found");
			return 0;
		}

		System.out.println("Please enter your password: ");
		String pass = br.readLine();
		out.writeUTF(pass);
		String conf = in.readUTF();
		if (conf.equals("confirmed")) {
			System.out.println("Logged in!");
			return 1;
		} else {
			System.out.println("Password Failed");
			return 0;
		}
	}

	/***************************************************************************
	 * gets username and password from user and sends to server to create the
	 * account
	 * 
	 * @throws IOException
	 *
	 * */
	private int createNewUser() throws IOException {
		String resp;
		System.out.print("Please enter new Username: ");
		String user = br.readLine();
		out.writeUTF(user);
		resp = in.readUTF();
		if (resp.equals("notok")) {
			System.out.println("Username taken");
			return 0;
		} else {
			String pass, cpass;
			System.out.println("Please enter a password: ");
			pass = br.readLine();
			System.out.println("Please confirm your password: ");
			cpass = br.readLine();
			while (!pass.equals(cpass)) {
				System.out.println("Passwords must match!");
				System.out.println("Please enter a password: ");
				pass = br.readLine();
				System.out.println("Please confirm your password: ");
				cpass = br.readLine();
			}
			out.writeUTF(pass);
			System.out.println("User: " + user + "Pass: " + pass);
			return 1;
		}
	}

	// given a hashtable the client sends a long(number of bytes)
	// and then sends the corresponding file over
	private void sendRequests(LinkedList<String> lst) throws IOException {
		if (verbose) {
			System.out.println("Sending List");
		}
		out.writeInt(lst.size());
		for (String e : lst) {
			out.writeUTF(e);
		}
	}

	private LinkedList<String> recieveRequests() throws IOException, ClassNotFoundException {
		if (verbose) {
			System.out.println("Recieving List");
		}
		int i = in.readInt();
		LinkedList<String> temp = new LinkedList<String>();
		if (i != 0) {
			for (int y = 0; y < i; y++) {
				temp.push(in.readUTF());
			}
		}
		return temp;
	}

	private void sendHashMap(HashMap<String, Long> map) throws IOException {
		if (verbose) {
			System.out.println("Sending Map");
		}
		out.writeInt(map.size());
		for (Entry<String, Long> e : map.entrySet()) {
			out.writeUTF(e.getKey());
			out.writeLong(e.getValue());
		}

	}

	private HashMap<String, Long> retriveHashMap() throws IOException {
		if (verbose) {
			System.out.println("Recieving Map");
		}
		int i = in.readInt();
		HashMap<String, Long> temp = new HashMap<String, Long>();
		if (i != 0) {
			for (int y = 0; y < i; y++) {
				temp.put(in.readUTF(), in.readLong());
			}
		}
		return temp;
	}

	@Override
	public void run() {
		try {
			int status = signIn();
			while (status != 1) {
				status = signIn();
			}
			sendHashMap(clientFiles);
			serverFiles = retriveHashMap();
			requestedFiles = recieveRequests();
			LinkedList<Thread> threads = new LinkedList<Thread>();
			while (!requestedFiles.isEmpty()) {
				Uploader u = new Uploader(folder + separator + requestedFiles.pop(), out);
				Thread t1 = new Thread(u);
				t1.start();
				threads.add(t1);
				if(verbose){
					System.out.println("Adding new Thread: "+t1.getId());
				}
			}
			// wait on files to finish uploading
			while (!threads.isEmpty()) {
				threads.pop().join();
			}
			// recieve hashmap from server
			String cmd;
			String answer;
			do {
				serverFiles = retriveHashMap();
				printHashMap(serverFiles);
				System.out
						.print("Please enter file name to recieve file(enter 'exit' to quit): ");
				cmd = br.readLine();
				out.writeUTF(cmd);
				if (cmd.toLowerCase().equals("exit")) {
					continue;
				}
				answer = in.readUTF();
				if (answer.equals("Sending...")) {
					Downloader d = new Downloader(folder + cmd, in);
					Thread t1 = new Thread(d);
					t1.start();
					t1.join();
					System.out.println("Completed!");
				} else {
					System.out.println(answer);
				}
			} while (!cmd.toLowerCase().equals("exit"));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void printHashMap(HashMap<String, Long> hm) {
		int count = 1;
		System.out.print("Files" + "\t" + "Date\n");
		for (Entry<String, Long> e : hm.entrySet()) {
			// may need to multiply the long by 1000
			Date d = new Date(e.getValue());
			String[] i = e.getKey().split(separator);
			System.out.println(count + ". " + i[i.length-1] + "\t" + d);
			count++;
		}
	}
}

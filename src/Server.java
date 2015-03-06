import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Server implements Runnable {
	private HashMap<String, Long> serverHashMap;
	private HashMap<String, Long> clientHashMap;
	private HashMap<String, String> users;
	private String userFile;
	private LinkedList<String> requestedFiles;
	private LinkedList<String> sendFiles;
	private Socket rSocket;
	private String folder;
	private ServerSocket sSocket;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean verbose;
	private String separator;

	public Server(int port, boolean v) throws IOException {
		verbose = v;
		if(System.getProperty("os.name").equals("Linux")){
			separator = "/";
		}else{
			separator = "\\";
		}
		String working = System.getProperty("user.dir");
			folder = working + separator + "serverFiles";
			userFile = working + separator + "users.txt";
		getUsers();
		sSocket = new ServerSocket(port);
		System.out.println("Listening on port " + port + "...");
		rSocket = sSocket.accept();
		in = new DataInputStream(rSocket.getInputStream());
		out = new DataOutputStream(rSocket.getOutputStream());

	}

	private int authenticate() throws IOException {
		String usr = in.readUTF();
		if (usr.equals("~")) {
			int test = createNewUser();
			while (test != 1) {
				test = createNewUser();
			}
			if(verbose){
				System.out.println("Account Created!");
			}
			return 0;// start over
		}
		
		if (verbose) {
			System.out.println("Checking...");
		}
		if (!users.containsKey(usr)) {
			out.writeUTF("notok");
			return 0;
		}else{
			out.writeUTF("ok");
		}
		String pass = in.readUTF();
		if (verbose) {
			System.out.println("Checking user: " + usr + "pass:" + pass);
		}
		String tPass = users.get(usr);
		if (tPass.equals(pass)) {
			if (verbose) {
				System.out.println("password confirmed");
			}
			out.writeUTF("confirmed");
			folder = folder + separator + usr;
			return 1;
		} else {
			if (verbose) {
				System.out.println("password failed");
			}
			out.writeUTF("unconfirmed");
			return 0;
		}

	}

	private int createNewUser() throws IOException {
		String user = in.readUTF();
		if (verbose) {
			System.out.println("Attempting to create new User");
			System.out.println("User: " + user);
		}
		if (users.containsKey(user)) {
			if (verbose) {
				System.out.println("Username taken");
			}
			out.writeUTF("notok");
			return 0;
		} else {
			out.writeUTF("ok");
			System.out.println("waiting on password..");
			String pass = in.readUTF();// get the password
			if (verbose) {
				System.out.println(user + ":" + pass);
			}
			PrintWriter pw = new PrintWriter(userFile);
			pw.write(user + ":" + pass);
			pw.close();
			users.put(user, pass);
			new File(folder + separator + user).mkdir();
			return 1;
		}

	}

	private void getUsers() throws IOException {
		String path;
		String working = System.getProperty("user.dir");
		if (System.getProperty("os.name").equals("Linux")) {
			path = working + separator + "users.txt";
		} else {
			path = working + separator + "users.txt";
		}
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		HashMap<String, String> accounts = new HashMap();
		while ((line = br.readLine()) != null) {
			String[] account = line.split(":");
			accounts.put(account[0], account[1]);
		}
		users = accounts;
	}

	private void compareMaps() {
		if(verbose){
			System.out.println("comparing maps");
		}
		requestedFiles = new LinkedList<String>();
		for (Entry<String, Long> e : clientHashMap.entrySet()) {
			if (serverHashMap.containsKey(e.getKey())) {
				Long dm = serverHashMap.get(e.getKey());
				if (e.getValue().compareTo(dm) < 0) {
					requestedFiles.add(e.getKey());
				}
			} else {
				requestedFiles.add(e.getKey());
			}
		}
	}

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
			int status = authenticate();
			while (status != 1) {
				status = authenticate();
			}
			FileScan f = new FileScan(folder);
			f.scan();
			serverHashMap = f.returnMap();
			clientHashMap = retriveHashMap();
			sendHashMap(serverHashMap);
			compareMaps();
			sendRequests(requestedFiles);
			LinkedList<Thread> threads = new LinkedList<Thread>();
			while (!requestedFiles.isEmpty()) {
				Downloader d = new Downloader(folder + separator + requestedFiles.pop(), in);
				Thread t1 = new Thread(d);
				t1.start();
				threads.add(t1);
				if(verbose){
					System.out.println("Adding new Thread: "+t1.getId());
				}
			}
			// wait for the children
			while (!threads.isEmpty()) {
				threads.pop().join();
			}
			// update server hashmap and send to client
			String cmd;
			do {
				f.scan();
				serverHashMap = f.returnMap();
				sendHashMap(serverHashMap);
				cmd = in.readUTF();
				if (cmd.toLowerCase().equals("exit")) {
					continue;
				}
				if (serverHashMap.containsKey(cmd)) {
					out.writeUTF("Sending...");
					Uploader u = new Uploader(folder + separator + cmd, out);
					Thread t1 = new Thread(u);
					t1.start();
					t1.join();
				} else {
					out.writeUTF("Could not find " + cmd);
				}
			} while (!cmd.toLowerCase().equals("exit"));

		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
	private ServerLogHelper sLog;

	/***********************************************************
	 * Given a remote socket, the Constructor sets up:
	 * 1. verbose option
	 * 2. Server files folder
	 * 3. Users files
	 * 4. Server logging helper
	 * 5. DataStreams
	 * ************************************************************/
	public Server(Socket client, boolean v) throws IOException {
		verbose = v;
		String working = System.getProperty("user.dir");
		if (System.getProperty("os.name").equals("Linux")) {
			separator = "/";
			working = working.substring(0, working.length()-4);
		} else {
			separator = "\\";
			working = working.substring(0, working.length()-5);
		}
		sLog = new ServerLogHelper(client.getInetAddress(), separator, working + separator + "ExternalFiles");
		folder = working + separator +"ExternalFiles"+ separator +"serverFiles";
		userFile = working + separator +"ExternalFiles"+ separator + "users.txt";
		getUsers();
		rSocket = client;
		in = new DataInputStream(rSocket.getInputStream());
		out = new DataOutputStream(rSocket.getOutputStream());

	}
	/*******************************************************************************
	 * Handles signing in the Client or possibly creating a new account all together
	 * @returns 1 for sucessful sign in, or 0 for unsucessful signin
	 * */
	private int authenticate() throws IOException {
		String usr = in.readUTF();
		if (usr.equals("~")) {
			int test = createNewUser();
			while (test != 1) {
				test = createNewUser();
			}
			if (verbose) {
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
		} else {
			out.writeUTF("ok");
		}
		String pass = in.readUTF();
		if (verbose) {
			System.out.println("Checking user: " + usr + "pass:" + pass);
		}
		String tPass = users.get(usr);
		if (tPass.equals(pass)) {
			sLog.clientWrite("Logged in. User: " + usr + " password: " + pass);
			if (verbose) {
				System.out.println("password confirmed");
			}
			out.writeUTF("confirmed");
			folder = folder + separator + usr;
			return 1;
		} else {
			sLog.clientWrite("Failed to log into account. Credentials: User: "
					+ usr + " password: " + pass);
			if (verbose) {
				System.out.println("password failed");
			}
			out.writeUTF("unconfirmed");
			return 0;
		}

	}
	/**********************************************************
	 * Creates a new user and makes sure there isn't a user with
	 * that name
	 * @returns 1 for successful creation, 0 for unsuccessful creation(username already existed)
	 * */
	private int createNewUser() throws IOException {
		String user = in.readUTF();
		sLog.clientWrite("Client attempting to create a new user");
		if (verbose) {
			System.out.println("Attempting to create new User");
			System.out.println("User: " + user);
		}
		if (users.containsKey(user)) {
			sLog.clientWrite("Client failed to create a new user. Username: "
					+ user + " already existed");
			if (verbose) {
				System.out.println("Username taken");
			}
			out.writeUTF("notok");
			return 0;
		} else {
			out.writeUTF("ok");
			String pass = in.readUTF();// get the password
			if (verbose) {
				System.out.println(user + ":" + pass);
			}
			FileWriter fileWritter = new FileWriter(userFile, true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			bufferWritter.write("\n" + user + ":" + pass);
			bufferWritter.close();
			users.put(user, pass);
			new File(folder + separator + user).mkdir();
			sLog.clientWrite("Client sucessfully created a new Account");
			return 1;
		}

	}
	/*******************************************************************
	 * Scans through the users.txt and puts the usernames and passwords into 
	 * the users hashmap
	 * *****************************************************************/
	private void getUsers() throws IOException {
		File f = new File(userFile);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		HashMap<String, String> accounts = new HashMap<String, String>();
		while ((line = br.readLine()) != null) {
			String[] account = line.split(":");
			accounts.put(account[0], account[1]);
		}
		br.close();
		users = accounts;
	}

	/**********************************************************************
	 * compares ClientHashMap's and ServerHashMap's files
	 * if the client has a file that the server doesn't it appends the filename to linkedList
	 * if one hashmap has a more current file it appends the filename to the linkedList
	 **********************************************************************/
	private void compareMaps() {
		if (verbose) {
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
	/**********************************************************************
	 * Sends a Linkedlist to the client
	 * */
	private void sendRequests(LinkedList<String> lst) throws IOException {
		if (verbose) {
			System.out.println("Sending List");
		}
		out.writeInt(lst.size());
		for (String e : lst) {
			out.writeUTF(e);
		}
		sLog.serverWrite("sent requestList " + lst.toString());
	}
	
	/***********************************************************************
	 * Recieves a LinkedList from the client
	 * */
	private LinkedList<String> recieveRequests() throws IOException {
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
	/***********************************************************************
	 * Sends a hashmap to the client
	 * */
	private void sendHashMap(HashMap<String, Long> map) throws IOException {
		if (verbose) {
			System.out.println("Sending Map");
		}
		out.writeInt(map.size());
		for (Entry<String, Long> e : map.entrySet()) {
			out.writeUTF(e.getKey());
			out.writeLong(e.getValue());
		}
		sLog.serverWrite("sent HashMap" + map.keySet().toString());
	}

	/**********************************************************************
	 * Retrieves a hashmap from the client
	 * @returns HashMap<String, Long> of the files on the client
	 * */
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
		sLog.serverWrite("recieved Hashmap: " + temp.keySet().toString());
		return temp;
	}

	@Override
	public void run() {
		try {
			System.out.println("Client: "+rSocket.getInetAddress()+" just connected");
			int status = authenticate();
			//loop until a valid authentication happens
			while (status != 1) {
				status = authenticate();
			}
			//scan the files serverside and make a hashmap
			FileScan f = new FileScan(folder);
			f.scan();
			serverHashMap = f.returnMap();
			//get client's files and send server's
			clientHashMap = retriveHashMap();
			sendHashMap(serverHashMap);
			compareMaps();
			sendRequests(requestedFiles);
			LinkedList<Thread> threads = new LinkedList<Thread>();
			while (!requestedFiles.isEmpty()) {
				String fileRequestedName = requestedFiles.pop();
				Downloader d = new Downloader(folder + separator
						+ fileRequestedName, in);
				Thread t1 = new Thread(d);
				t1.start();
				sLog.serverWrite("recieving " + fileRequestedName);
				threads.add(t1);
				if (verbose) {
					System.out.println("Adding new Thread: " + t1.getId());
				}
			}
			// wait for the children
			while (!threads.isEmpty()) {
				threads.pop().join();
			}
			sLog.serverWrite("All Files Downloaded");
			// update server hashmap and send to client
			String cmd;
			do {
				f.scan();
				serverHashMap = f.returnMap();
				sendHashMap(serverHashMap);
				cmd = in.readUTF();
				sLog.clientWrite("requested file: " + cmd);
				if (cmd.toLowerCase().equals("exit")) {
					continue;
				}
				if (serverHashMap.containsKey(cmd)) {
					sLog.serverWrite("sending file: " + cmd);
					out.writeUTF("Sending...");
					Uploader u = new Uploader(folder + separator + cmd, out);
					Thread t1 = new Thread(u);
					t1.start();
					t1.join();
				} else {
					sLog.serverWrite("couldn't find " + cmd);
					if (verbose) {
						out.writeUTF("Could not find " + cmd);
					}
				}
			} while (!cmd.toLowerCase().equals("exit"));
			sLog.clientWrite("disconnected");
			sLog.closeLog();
			System.out.println("Client: "+rSocket.getInetAddress()+" just closed");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

}

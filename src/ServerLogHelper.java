import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogHelper {
	private InetAddress ip;
	private Date date;
	private BufferedWriter log_writer;
	private DateFormat date_format;
	
	//Public Constructor - Takes int Port argument
	
	public ServerLogHelper(InetAddress i, String separator, String directory) throws IOException{
		ip = i;
		
		//Create Date and Date Format Objects
		date = new Date();
		DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		
		//Create the log file and add to it the time and date so that every construction of a log file will create a
		//brand new one instead of overwriting the old one.
			
	    	
	    	String fileLocation =  directory + separator + "serverLogFile";
	    	File log_file = new File(fileLocation + ".txt");
	    	if(!log_file.exists()){
	    		log_file.createNewFile();
	    		System.out.println("creating log file");
	    	}
			log_writer = new BufferedWriter(new FileWriter(log_file, true));
			
		    log_writer.write("Client: " + ip + " connected on:" + date_format.format(date)+"\n");
	    
	    
	}
	//Method for client writting to the log file, simply create the log file object and then call .writeToLog(string)
	public void clientWrite(String t) throws IOException{
		log_writer.write(ip.toString()+": "+t.toString()+"\n");
	}

	public void serverWrite(String t)throws IOException{
		log_writer.write("Server to "+ip.toString()+": "+t.toString()+"\n");
	}
	//Method for closing the log file at the end of the connection
	//***VERY IMPORTANT TO CLOSE THE LOG FILE!!! WILL NOT WORK WITHOUT THIS METHOD!!!***
	//
	public void closeLog() throws IOException{
		log_writer.flush();
		log_writer.close();
	}
	
	
}
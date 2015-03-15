import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Downloader implements Runnable {
	private DataInputStream in;
	private String requested;
	
	/*****************************
	 * constructor 
	 * */
	public Downloader(String r, DataInputStream i){
		in = i;
		requested = r;
	}
	/****************************
	 * recieves bytes from server and recreates the file from them 
	 * */
	public void download(int bytes) throws FileNotFoundException, IOException{
		if(bytes != 0){
			byte[] picArray = new byte[bytes];
			int c = bytes;
			while(c > 0){
				picArray[bytes-c] = in.readByte();
				c--;
			}
			FileOutputStream outFile = new FileOutputStream(requested);
			outFile.write(picArray);
			outFile.close();
		}else{
			System.out.println("File not Found: " + requested);
		}
	}

	@Override
	public void run() {
		try {
			download((int)in.readLong());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

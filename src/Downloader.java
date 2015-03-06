import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Downloader implements Runnable {
	private DataInputStream in;
	private String requested;
	
	public Downloader(String r, DataInputStream i){
		in = i;
		requested = r;
	}
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
			System.out.println("File not Found, Sorry...");
			System.exit(0);
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

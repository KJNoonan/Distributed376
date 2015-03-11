import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Uploader implements Runnable{
		private DataOutputStream out;
		private String sendingFile;
		
		public Uploader(String f, DataOutputStream o){
			out = o;
			sendingFile = f;
		}
		public void upload() throws FileNotFoundException, IOException{
			File file = new File(sendingFile);
			long bts = file.length();
			out.writeLong(bts);
			
			FileInputStream fs = new FileInputStream(sendingFile);
			int b;
			while((b = fs.read()) != -1){
				out.writeByte(b);
			}
			fs.close();
		}
		@Override
		public void run() {
			try {
				upload();
			} catch (IOException e) {
				System.out.println("File not found: " + sendingFile);
			}
			
		}

	}



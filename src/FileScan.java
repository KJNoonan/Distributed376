import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;


public class FileScan {
	
	private HashMap<String, Long> fileList;
	private File folder;
	
	public FileScan(String path) {
		folder = new File(path);
	}
	
	/************************************************
	 * Scans the files and puts them into a hashmap
	 * */
	public void scan(){
		fileList = new HashMap<String, Long>();
		if(folder.isDirectory()){
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            scan();
	        } else {
	        	// changed the key to be the path
	        	// makes it easier to make file transfer to its specific directory
	            fileList.put(fileEntry.getName(), fileEntry.lastModified());
	        }
	    }
		}else{
			System.out.println("Not a directory! Making File:" + folder);
			System.out.println("Please Rerun");
			folder.mkdir();
			System.exit(1);
		}
	}
	public void printTable(){
		scan();
		for (Entry<String, Long> e:fileList.entrySet()){
			//System.out.println("File: " + e.getKey() + " |Date Modified: "+ e.getValue());
			System.out.println(e.getKey()+"   "+e.getValue());
		}
	}
	/***************************************************
	 * scan and return the hashmap
	 * */
	public HashMap<String, Long> returnMap(){
		scan();
		return fileList;
		
	}
	public static void main(String[] args){
		FileScan f = new FileScan("c:\\Users\\jesus\\Desktop\\New folder");
		f.printTable();
		
		
	}

	
}









































/*import java.io.File;
import java.util.Hashtable;
import java.util.Map.Entry;


public class FileScan {
	
	private Hashtable<String, Long> fileList;
	
	
	public FileScan(String path) {
		fileList = new Hashtable<String, Long>();
		File folder = new File(path);
		scan(folder);
	}
	
	private void scan(File folder){
		
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            scan(fileEntry);
	        } else {
	            fileList.put(fileEntry.getPath(), fileEntry.lastModified());
	        }
	    }
	}
	public void printTable(){
		for (Entry<String, Long> e:fileList.entrySet()){
			System.out.println("File: " + e.getKey() + " |Date Modified: "+ e.getValue());
		}
	}
	public Hashtable<String, Long> returnTable(){
		return fileList;
		
	}
	public static void main(String[] args){
		FileScan f = new FileScan("c:\\Users\\jesus\\Desktop\\New folder");
		f.printTable();
		
		
	}
}
*/
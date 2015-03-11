
public class test {
	public static void main(String args[]){
		String working = System.getProperty("user.dir");
		System.out.println(working);
		System.out.println(working.replace("/src/", ""));
	}
}

package test;

public class hw32 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String flag;
		 
		flag="fail";
		
		int i ;
		int n;
		n=Integer.parseInt(args[0]);
		for(i=2;i<n;i++){
			if(n % i==0){
			flag="pass";
		}
			}
		
		
		System.out.println(""+flag);		
		
	}

}

package test;

public class hw31 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub	
		int c;
		c=Integer.parseInt(args[0]);
		int total=(int) (c*0.06);
		int alltoatl=(int) (total+((c-300000)*0.13));
		if(c<=300000){
			System.out.println("µ|ª÷"+ total);
		}else{
			System.out.println("µ|ª÷"+ alltoatl);
		}
	}

}

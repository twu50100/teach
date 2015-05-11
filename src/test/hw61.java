package test;

public class hw61 {
	public static void main(String[] args){
		judge(1);
	}

	private static void judge(int answer) {
		// TODO Auto-generated method stub
	guess newanswer=new guess();
	if (newanswer.getAnswer()>answer){
	System.out.println("太小了");	
	}
	else if (newanswer.getAnswer()<answer){
		System.out.println("太大了");	
	}else{
		System.out.println("答對了");	
	}
	}
	
}

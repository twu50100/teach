package test;

public class hw62 {

	public static void main(String[] args){
		  setData("twu",true,20,"A123456789");
		 }
		private String name;
		private boolean gender;
		private int age;
		private String id;

		// TODO Auto-generated method stub
		
			public void setData(String name,boolean gender,int age,String id) {
				this.name=name;
				this.gender=gender;
				this.age=age;
				this.id=id;
		    personal newpersonal=new personal();   
			
				newpersonal.setDates("twu",true,20,"A123456789");
				
				personal newGender = new personal();
				personal newAge = new personal();
				personal newID = new personal();
				
				
						System.out.println("�ʧO�G"+newGender.getGender());
						System.out.println("�~���G"+newAge.getAge());
						System.out.println("�����Ҧr���G"+newID.getID());
			}
}

package test;

public class personal {
	private String name;
	private boolean gender;
	private int age;
	private String id;


	void Personal(String inputname){
		setName(inputname);
	}
	
	void Personal(String inputname,boolean inputgender,int inputage , String inputid){
		setName(inputname);
		gender=inputgender;
		age=inputage;
		id=inputid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setData(String name, boolean gender, int age, String id){
		this.name = name;
		this.gender=gender;
		this.age=age;
		this.id=id;
	}
	
	public boolean getGender(){
		return gender;
	}
	
	public int getAge(){
		return age;
	}
	
	public String getID(){
		return id;
	}

	public void setDates(String string, boolean b, int i, String string2) {
		// TODO Auto-generated method stub
		
	}

}

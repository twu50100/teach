package test;

public class pt61 {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	testObj Obj1= new testObj(1);
	testObj Obj2= new testObj((int) 1.0);
	testObj Obj3= new testObj("�r��");
	}

}

class testObj{

	public void testObj(int n){
		System.out.println("��ƫ��غc�l");
	}
	public testObj(double n){
		System.out.println("�B�I��");
	}
	public testObj(String string) {
		// TODO Auto-generated constructor stub
	}
	public void testObj(String n){
		System.out.println("�r��");
	}
	}
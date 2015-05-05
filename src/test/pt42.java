package test;

public class pt42 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int i;
		int[][] score={
				{1,7,15},
				{9,3,4},
				{6,8,13},
				{11,5,2},
				{10,7,9}
		};
		for(int[] firstArray:score){
			for(int element:firstArray){
				System.out.print(element+"\t");
			}
			System.out.println();
		}
	}

}

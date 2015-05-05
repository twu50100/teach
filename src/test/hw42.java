package test;

public class hw42 {
	public static void main(String[] args) {

	int i;
	int[][] score={
			{60,70,50},
			{91,54,80},
			{60,57,68},
			{61,80,72},
			{50,67,79}
	};
	for(int[] firstArray:score){
		for(int element:firstArray){
			System.out.print(element+"\t");
		}
		System.out.println();
	}
}

}
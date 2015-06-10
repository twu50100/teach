package test;

public class sort {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int [] list = new int[args.length];;
		for(int l =0;l<args.length;l++){
			list[l]=Integer.parseInt(args[l]);
		}
		int temp=0;		
		for(int i =list.length-1; i>0; i--){
			for(int j=0;j<i;j++){
				if(list[j]>list[j+1]){
					temp=list[j];
					list[j]=list[j+1];
					list[j+1]=temp;
				}
			}
		}
		for(int k =0 ;k<list.length;k++){
			System.out.print(list[k]+" ");
		}
	}

}

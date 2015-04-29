package test;

/**
 * @author michelle
 *
 */
public class hw23 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	int d;
	d=Integer.parseInt(args[0]);
	double c;
	c=(d<=1000)? 80:((d-1000)/300)*10 +80;
	System.out.print("里程數:" +d);
	System.out.print("計程車費:" +c);
	}
	}

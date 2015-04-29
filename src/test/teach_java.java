package test;

import javax.swing.JOptionPane;
public class teach_java {

	public static void main(String[] args) {
		String input1=JOptionPane.showInputDialog(null,"檔名開頭:","輸入對話框",JOptionPane.QUESTION_MESSAGE);
		String input2=JOptionPane.showInputDialog(null,"會計科目(以a,b,c,d,e代表):","輸入對話框",JOptionPane.QUESTION_MESSAGE);
		if(input1.equals("F")){
			if(!input2.equals("a")||!input2.equals("b")||!input2.equals("c")||!input2.equals("d")||!input2.equals("e")){
				JOptionPane.showMessageDialog(null,"會計科目不能為"+input2+"你確定!?");
			}else{
				JOptionPane.showMessageDialog(null,"檔案轉入成功");
			}
		}else{
			JOptionPane.showMessageDialog(null,"檔案轉入成功");
		}
		
	
		

	}

}

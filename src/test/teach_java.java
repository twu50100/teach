package test;

import javax.swing.JOptionPane;
public class teach_java {

	public static void main(String[] args) {
		String input1=JOptionPane.showInputDialog(null,"�ɦW�}�Y:","��J��ܮ�",JOptionPane.QUESTION_MESSAGE);
		String input2=JOptionPane.showInputDialog(null,"�|�p���(�Ha,b,c,d,e�N��):","��J��ܮ�",JOptionPane.QUESTION_MESSAGE);
		if(input1.equals("F")){
			if(!input2.equals("a")||!input2.equals("b")||!input2.equals("c")||!input2.equals("d")||!input2.equals("e")){
				JOptionPane.showMessageDialog(null,"�|�p��ؤ��ର"+input2+"�A�T�w!?");
			}else{
				JOptionPane.showMessageDialog(null,"�ɮ���J���\");
			}
		}else{
			JOptionPane.showMessageDialog(null,"�ɮ���J���\");
		}
		
	
		

	}

}

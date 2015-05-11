package test;

public class guess {
	private int answer;
void Guess(){
	answer=(int)(Math.floor(Math.random()*100)+1);
}
public int getAnswer() {
	return answer;
}
public void setAnswer(int answer) {
	this.answer = answer;
}


}

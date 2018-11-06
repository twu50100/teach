import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

abstract class HuffmanTree implements Comparable<HuffmanTree> {
    public final int frequency; // the frequency of this tree
    public HuffmanTree(int freq) { frequency = freq; }
 
    // compares on the frequency
    public int compareTo(HuffmanTree tree) {
        return frequency - tree.frequency;
    }
}
 
class HuffmanLeaf extends HuffmanTree {
    public final char value; 
 
    public HuffmanLeaf(int freq, char val) {
        super(freq);
        value = val;
    }
}
 
class HuffmanNode extends HuffmanTree {
    public final HuffmanTree left, right; 
 
    public HuffmanNode(HuffmanTree l, HuffmanTree r) {
        super(l.frequency + r.frequency);
        left = l;
        right = r;
    }
}
 
public class HuffmanCode {
	
	private static int count = 0;

    public static HuffmanTree buildTree(int[] charFreqs) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();

        for (int i = 0; i < charFreqs.length; i++)
            if (charFreqs[i] > 0)
                trees.offer(new HuffmanLeaf(charFreqs[i], (char)i));
 
        assert trees.size() > 0;

        while (trees.size() > 1) {

            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();
 
            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }
 
    public static void printResult(HuffmanTree tree, StringBuffer prefix) {
        assert tree != null;
        if (tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf)tree;
            
            // 32=空白 , 13=換行             
            if((int)leaf.value==32){
            	System.out.println("空白\t" + leaf.frequency + "/"+count+"\t" + prefix);
            }else if((int)leaf.value==13) {
            	System.out.println("換行\t" + leaf.frequency + "/"+count+"\t" + prefix);
            }else{
            	System.out.println(leaf.value + "\t" + leaf.frequency + "/"+count+"\t" + prefix);
            }
            
 
        } else if (tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode)tree;
            //Recursion
            // left node
            prefix.append('0');
            printResult(node.left, prefix);
            prefix.deleteCharAt(prefix.length()-1);
 
            // right node
            prefix.append('1');
            printResult(node.right, prefix);
            prefix.deleteCharAt(prefix.length()-1);
        }
    }
 
    public static void main(String[] args) throws IOException {
        
    	//read file from D:\\santaclaus.txt
        FileReader fr = new FileReader("D:\\santaclaus.txt");

        BufferedReader br = new BufferedReader(fr);

        int[] charFreqs = new int[256];
                
        while (br.ready()) {
        	int n =br.read();
        	//換行=13 10 保留一個即可
        	if(n!=10){
                charFreqs[n]++;
                count++;
        	}
        }

        fr.close();
 
        // build tree
        HuffmanTree tree = buildTree(charFreqs);
 
        // results
        System.out.println("Symbol\tProp\tHuffman Code");
        printResult(tree, new StringBuffer());
    }
}
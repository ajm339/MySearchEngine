import java.util.HashMap;


public class Node {
	String name;
	HashMap<String, Double> atc_values;
	
	public Node(String name, HashMap<String,Double> atc_values){
		this.name = name;
		this.atc_values = atc_values;
	}

}

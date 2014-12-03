
public class DocumentVector implements Comparable<DocumentVector> {
	String key;
	double value;
	
	public DocumentVector(String key, double value){
		this.key = key;
		this. value = value;
	}
	
	public int compareTo(DocumentVector v){
		if (this.value < v.value){
			return -1;
		} else if (this.value > v.value){
			return 1;
		} else {
			return 0;
		}
	}
	
	public boolean equals(Object d) {
		if (d instanceof DocumentVector) {
			return ((this.value == (((DocumentVector) d).value) && this.key == ((DocumentVector) d).key));
		} else { 
			return false; 
		}
	}

}

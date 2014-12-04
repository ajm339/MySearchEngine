import java.util.ArrayList;


public class Cluster {
	ArrayList<String>node_list = new ArrayList<String>();
	Node node1;
	Node node2;
	Cluster cluster;
	
	public Cluster(Node node1, Node node2){
		this.node1 = node1;
		this.node2 = node2;
	}
	
	public Cluster (Cluster cluster, Node node1){
		this.cluster = cluster;
		this.node1 = node1;
	}
	
	public Cluster getCluster(){
		if(this.cluster != null){
			return this.cluster;
		} else {
			return null;
		}
	}
}

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;


public class CompleteClustering {
	HashMap<String, HashMap<String,Double>> docs_atc_scores = NewSearchEngine.db_document_tfidf_normalized;
	static HashMap<String, HashMap<String, Integer>> db_document_vectors = NewSearchEngine.db;
	
	public static ArrayList<String>analyze(int K, ArrayList<String> results){
		
		ArrayList<String> sub_results = new ArrayList<String>();
		for(int i =0; i<30; i++){
			sub_results.add(results.get(i));
		}
		ArrayList<ArrayList<String>> clustering_results = CompleteClustering.calculateCluster(K, sub_results);
		ArrayList<String> highest_clustering_results = getHighestRanked(sub_results, clustering_results);
		
		return highest_clustering_results;
	}
	
	/*
	 * Calculate the average rankings of clusters
	 */
	public static ArrayList<String> sortAverage(ArrayList<ArrayList<String>> cluster) {
		ArrayList<String> new_cluster_list = new ArrayList<String>(); 
		for (ArrayList<String> list: cluster) {
			new_cluster_list.addAll(list);
		}
		Collections.sort(new_cluster_list);
		Collections.reverse(new_cluster_list);
		
		return new_cluster_list;
	}
	
	
	/*
	 * Method to reorder results based on clustering and results list
	 */
	public static ArrayList<String> getHighestRanked(ArrayList<String>results, ArrayList<ArrayList<String>>cluster_list){
		ArrayList<String> new_results = new ArrayList<String>();
			
			//loop through the atc.atc results until reordered by clusters
			for(String document:results){
				
				//move through to next document if current one has already been added.
				if(new_results.contains(document)){
					continue;
				}
				
				//iterate through each set of lists(clusters)
				int n = 0;
				while (n < cluster_list.size()){
					boolean isFound = false;
					//iterate through a single list(cluster)
					int length = cluster_list.get(n).size();
					for(int i=0; i<length; i++){
						ArrayList<String> list = cluster_list.get(n);
						//if the document from results is found, add all the documents to the cluster and break the loop 
						if(document.equals(list.get(i))){
							new_results.addAll(list);
							isFound = true;
							break;
						}
					}
					//break while loop if document is already found to move to next document to be searched
					if(isFound){
						break;
					}
					n++;
				} //end while
				
				if(new_results.size()>=30){
					break;
				}
				
			}
		
		return new_results;
	}
	
	
	
	/*
	 * Calculates the vector distance for 2 documents
	 */
	private static double distance(String doc1, String doc2){
		double distance = 0.0;
		
		HashMap<String,Integer> vector1 = db_document_vectors.get(doc1);  //word vectors of document 1
		HashMap<String,Integer> vector2 = db_document_vectors.get(doc2);  //word vectors of document 2
		
		HashSet<String> vector_keys = new HashSet<String>();  //vector keys of document 1 and 2 for needed calculating distances

		//loop to add vector1 keys to vector_keys
		for(String itr: vector1.keySet()){
			vector_keys.add(itr);
		}
		//loop to add vector2 keys to vector_keys
		for(String itr: vector2.keySet()){
			vector_keys.add(itr);
		}
		
		double denominator = 0.0; //under the square root of distance formula
		for( String itr: vector_keys){
			int v1 = (vector1.containsKey(itr) ? vector1.get(itr) : 0); //frequency of word in vector 1
			int v2 = (vector2.containsKey(itr) ? vector2.get(itr) : 0); //frequency of word in vector 2
			
			double inner_denominator = v1-v2;
			denominator += Math.pow(inner_denominator, 2);
		}
		
		distance = 1/Math.sqrt(denominator);
		
		return distance;
	}
	
	/*
	 * Takes in two lists (clusters of nodes, and finds the max linkage distance between the two lists and returns it to iterateClusters
	 */
	private static double compareLists(ArrayList<String> list1, ArrayList<String> list2){
		double max_distance = 0.0;
		for(int i=0; i<list1.size(); i++){
			for(int j=0; j<list2.size(); j++){
				double distance = distance(list1.get(i), list2.get(j));
				max_distance = Math.max(max_distance, distance);
			}
		}
		
		return max_distance;
	}
	
	/*
	 * Iterates through the current cluster in memory to find the two new clusters to be merged
	 */
	private static ArrayList<ArrayList<String>> iterateCluster(ArrayList<ArrayList<String>> cluster){
		int list1_index = 0;
		int list2_index = 0;
		double min_distance = 0.0;
		
		//loop through each list in the cluster in relation to the other lists
		//find the minimum distance between lists, merge the two lists into one element in cluster
		for (int i=0; i<cluster.size(); i++){
			for (int j=1; j<cluster.size()-1; j++){
				
				double calculated_distance = compareLists(cluster.get(i), cluster.get(j));

				//if the calculated distance between clusters is less than the current min distance, remember the distance and clusters
				//first pass sets min_distance to calculated_distance
				if((calculated_distance<min_distance)||(i==0)){
					min_distance = calculated_distance;
					list1_index = i;
					list2_index = j;
				}
			}
		}
		
		ArrayList<String> new_joint_list = new ArrayList<String>();  //initialize the new joint list of two list with the minimum distance
		
		new_joint_list.addAll(cluster.get(list1_index)); //add first list
		new_joint_list.addAll(cluster.get(list2_index)); //add second list
		
		cluster.set(list1_index, new_joint_list); //replace the spot where the first list is located with the new list of the two merged lists
		cluster.remove(list2_index); //remove the second list since it has been merged with the first
		
		return cluster;
	}
	
	/*
	 * Takes in the list of the top 30 documents from atc.atc ranking, and merges them until there are K clusters
	 */
	public static ArrayList<ArrayList<String>> calculateCluster(int K, ArrayList<String> top30){
		int n = 30;
		
		ArrayList<ArrayList<String>> cluster = new ArrayList<ArrayList<String>>(); //list that holds the list of docs.  [list1, list2, list3]  each list starts initially with 1 doc, and as clusters form, the lists merge to hold the docs of a cluster
		
		//loop to initialize the cluster list
		for(int i=0; i<30; i++){
			String doc_name = top30.get(i);
			ArrayList<String>document = new ArrayList<String>();
			document.add(doc_name);
			cluster.add(document);
		}
		
		while(n>K){
			cluster = iterateCluster(cluster);
			n-=1;
		}
		
		return cluster;
	}

}

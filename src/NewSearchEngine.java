import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;


import java.util.function.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

public class NewSearchEngine{
	static String data = "";

	static CharArraySet stopwords;

	
	HashMap<String,Integer>docs_by_rel = new HashMap<String,Integer>(); //term to # rel_docs
	HashMap<String,Integer>docs_with_term = new HashMap<String,Integer>(); //term to # total_docs
	HashMap<String,Integer>relevant_doc_num = new HashMap<String,Integer>();
	HashMap<String,HashMap <String,Integer>> term_freq_in_doc = new HashMap<String,HashMap <String,Integer>> ();
	HashMap<String,Integer>term_freq_in_query = new HashMap<String,Integer>();
	static HashMap<String, HashMap<String,Double>> db_document_tfidf_normalized = new HashMap<String, HashMap<String,Double>>();
	static HashMap<String, HashMap<String,Integer>> db;
	static HashMap<String,Double> query_tokens;

	////////////////////////////////////INDEXING//////////////////////////////////////

	public static String Rocchio (double alpha, double beta,HashMap<String,Integer> query_tokens_map, ArrayList<String> relevant_docs_array){
		query_tokens = new HashMap<String,Double>();
		for(String k : query_tokens_map.keySet()){
			query_tokens.put(k, query_tokens_map.get(k).doubleValue());
		}
		ArrayList<HashMap<String,Integer>> relevant_docs = new ArrayList<HashMap<String,Integer>>();
		ArrayList<HashMap<String,Integer>> nonrelevant_docs = new ArrayList<HashMap<String,Integer>>();
		for(String doc : db.keySet()){
			if(relevant_docs_array.contains(doc)){
				relevant_docs.add(db.get(doc));
			}else{
				nonrelevant_docs.add(db.get(doc));
			}
		}
		double true_alpha = alpha/relevant_docs.size();
		double true_beta = beta/nonrelevant_docs.size();
		for(int x = 0; x < relevant_docs.size(); x++){
			for(String key : relevant_docs.get(x).keySet()){
				if(query_tokens.get(key) == null){
					query_tokens.put(key, true_alpha);
				}else{
					query_tokens.put(key, query_tokens.get(key) + true_alpha);
				}
			}			
		}		
		for(int x = 0; x < nonrelevant_docs.size(); x++){
			for(String key : nonrelevant_docs.get(x).keySet()){
				if(query_tokens.get(key) == null){
					query_tokens.put(key, -1.0*true_beta);
				}else{
					query_tokens.put(key, query_tokens.get(key) - true_beta);
				}
			}			
		}	
		List<String> query_token_words = new ArrayList<String>();
		query_token_words.addAll(query_tokens.keySet());
		Collections.sort(query_token_words, new Comparator<String>(){
            public int compare(String str1, String str2) {
            	return query_tokens.get(str2).compareTo(query_tokens.get(str1));
            }
        });
		for(String token : query_tokens_map.keySet()){
			query_token_words.remove(token);
		}
		return query_token_words.subList(0,7).toString();
	}
	
	public static void buildIndex(String indexDir, String docsPath, CharArraySet stops) {

		data = "";
		// Check whether docsPath is valid
		if (docsPath == null || docsPath.isEmpty()) {
			System.err.println("Document directory cannot be null");
			System.exit(1);
		}
		
		// Check whether the directory is readable
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		stopwords = stops;
		indexDocs(docDir);
		createdb(data,docsPath.split("/")[1]);
		
	}
	
	static void indexDocs(File file) {
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(new File(file, files[i]));
					}
				}
			} else {
				FileInputStream fis = null;
				
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
				}

				try {
					data += encode_db_line(AnalyzeThings(new BufferedReader(new InputStreamReader(fis))),file);

				} catch (IOException e) {
					System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
				} finally {
					try {
						fis.close();
					} catch(IOException e) {
						System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
					}
				}
			}
		}
	}	
	
	////////////////////////////////////SEARCH//////////////////////////////////////
	
	public static ArrayList<String> runQuery(String search_term, int num_results, String docsPath, HashSet<String> answers){
		try {
			HashMap<String, Integer> tokenized = tokenizeString(search_term);
			db = reassemble(docsPath.split("/")[1]);
			ArrayList<String> arrlist = evaluate_db(db,tokenized);
//			System.out.print("BM25: ");
//			System.out.println(bm25(arrlist, answers, db, tokenized));
//			System.out.print("ATCATC: ");
//			System.out.print("ATNATN: ");
//			System.out.println(atnatn(arrlist, answers, db, tokenized));
//			System.out.print("ANNBPN: ");
//			System.out.println(annbpn(arrlist, answers, db, tokenized));
//			System.out.print("THE ALEXV6 (SUPER AWESOME) WEIGHTING ALGORITHM: ");
//			System.out.println(my_weighting(arrlist, answers, db, tokenized));
			return atcatc2(db, tokenized, num_results);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}
	
	
	public static ArrayList<String> evaluate_db(HashMap<String, HashMap<String,Integer>> db, HashMap<String,Integer> tokenized){
		HashMap<String,Integer> temp_map = new HashMap<String,Integer>();
		ArrayList<String> k;

		for(String x : db.keySet()){
			int j = 0;
			for(String h : tokenized.keySet()){
				if(db.get(x).get(h) != null){
					j += db.get(x).get(h);
				}
			}	
			temp_map.put(x,j);
		}

		k = new ArrayList<String>(temp_map.keySet());
		Collections.sort(k, new Comparator<String>() {
	        public int compare(String str1, String str2) {
	            return temp_map.get(str2) - temp_map.get(str1);
	        }
	    });
		ArrayList<String> arnold = new ArrayList<String>(k.subList(0, 100));
		return arnold;
		
	}
	
	
	//"""""""""""""""""""""""""""HELPERS""""""""""""""""""""""""""""""""""""""""
	
	
	public static double bm25(ArrayList<String> actual, HashSet<String> answers, HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		double total_sum = 0.0;
		HashMap<String,Integer>term_freq = new HashMap<String,Integer>();
		for(String term : tokenized){
			for(String str : db.keySet()){
				if(db.get(str).containsKey(term)){
					if(term_freq.containsKey(str)){
						term_freq.put(str,term_freq.get(str) + db.get(str).get(term));
					}else{
						term_freq.put(term, db.get(str).get(term));
					}	
				}
			}
			if(!term_freq.containsKey(term)){
				term_freq.put(term, 0);
			}
		}
		for(String term : tokenized){
			double ri = 0.0;
			double ni = 0.0;
			double n = (double)db.size();
			double r = (double)answers.size();
			double avdl = 0.0;
				
			for(String str : answers){
				if(db.get(str).get(term) != null){
					ri += 1.0;
				}
			}
			for(String str : db.keySet()){
				if(db.get(str).get(term) != null){
					ni += 1.0;
				}
				avdl += (double) db.get(str).size();
			}
			avdl /= n;
			double b = 0.75;
			double k1 = 1.2;
			double k2 = 100.0;
			
			double sum = 0.0;
			
			double a = Math.log(((ri+0.5)*(n-ni-r+ri+0.5))/((ni-ri+0.5)*(r-ri+0.5)));
			for(String str : db.keySet()){
				double k = 1.2*(0.25 + 0.75*((double)db.get(str).size())/avdl);
				Integer fi = (db.get(str).get(term) == null) ? 0 : db.get(str).get(term);
				double b2 = (k1+1.0)*((double)fi)/(k+(double)fi);
				double c = (k2 +1.0)*((double)term_freq.get(term))/(k+((double)term_freq.get(term)));
				sum += a*b2*c;
			}
			total_sum += sum/db.keySet().size();
		}
		return total_sum/((double)tokenized.size());
		
	}
	//b = 0.75 k1 = 1.2 k2 = 100.0
	
	
	public static HashMap<String, Double> calculate_idf(HashMap<String, HashMap<String,Integer>> db, HashMap<String, Integer> tokenized){
		double N = (double)db.size(); //total number of docs needed for idf (t)	
		
		HashMap<String, Integer> term_doc_freq = new HashMap<String, Integer>(); //number of documents of each term occurs in
		HashMap<String, Double> idf = new HashMap<String, Double>();
		/*loop counts how many docs a term occurs 
		 * in and stores in term_doc_freq HashMap.  
		 * calculate idf (t)
		 */
		
			int n = 0; //total number of docs a term occurs in
			for(String str : db.keySet()){
				for(String term: db.get(str).keySet()){
					if(term_doc_freq.containsKey(term)){
						term_doc_freq.put(term, term_doc_freq.get(term)+1);
					} else {
						term_doc_freq.put(term, 1);
					}
				}
			}
			
			for(String itr : term_doc_freq.keySet()){
				idf.put(itr, Math.log(N/term_doc_freq.get(itr))); //idf
			}
		
		return idf;
	}
	
	public static HashMap<String, Double> calculate_doc_tf(HashMap<String,Integer> doc_tf){
		/*loop cycles through each document, 
		 * and counts the number of times a term in the query 
		 * occurs, and stores it in the HashMap term_freq.  also calculates the maximum 
		 * occurring term in each docment needed for tf (a)
		 */
		HashMap<String, Double> document_tf_vector = new HashMap<String, Double>();
		
			
			int max_occuring_term = 0;
			//get max occuring term frequency
			for(String str : doc_tf.keySet()){
				max_occuring_term = Math.max(max_occuring_term, doc_tf.get(str));
			}
			
			for(String str : doc_tf.keySet()){
				int value = doc_tf.get(str);
				double final_value = 0.5 + (0.5)*(value/max_occuring_term);
				document_tf_vector.put(str, final_value);
			}

		return document_tf_vector;
	}
	
	public static HashMap<String, Double> calculate_query_tf(HashMap<String,Integer> query_tf){
		int max_occuring_term = 0;
		
		for(String term : query_tf.keySet()){
			max_occuring_term = Math.max(max_occuring_term, query_tf.get(term));
		}
		
		HashMap<String,Double> calculated_query_tf = new HashMap<String,Double>();
		
		for(String term : query_tf.keySet()){
			int value = query_tf.get(term);
			double final_value = 0.5 + (0.5)*(value/max_occuring_term);
			calculated_query_tf.put(term, final_value);
		}
		return calculated_query_tf;
	}
	
	public static HashMap normalized(HashMap<String, Double> document_tfidf){
		/*
		 * loop to create normalization factor c
		 * */
		double c = 0.0; //normalization factor
		double denom_before_sqrt = 0.0;
		for(String itr : document_tfidf.keySet()){
			denom_before_sqrt += Math.pow(document_tfidf.get(itr), 2);
		}

		c = 1.0/Math.pow(denom_before_sqrt, 0.5);
		
		HashMap<String, Double> normalized_vector = new HashMap<String, Double>();
		
		for(String itr : document_tfidf.keySet()){
			normalized_vector.put(itr, document_tfidf.get(itr)*c);
		}
		return document_tfidf;
	}
	
	public static ArrayList<String> getTop(HashMap<String, Double>rankings, int n){
		PriorityQueue<DocumentVector> topDocs = new PriorityQueue<DocumentVector>(n);

		// for each key
		for(String document: rankings.keySet()){
			DocumentVector v = new DocumentVector(document, rankings.get(document));
			if(topDocs.size()<n){
				topDocs.add(v);
			} else if (topDocs.peek().compareTo(v) == -1){
				topDocs.poll();
				topDocs.add(v);
			}
			
		}
		
		Object[] results = topDocs.toArray();
		Arrays.sort(results, Collections.reverseOrder());
		
		ArrayList<String> final_results = new ArrayList<String>();
		
		for(int i=0; i<results.length; i++){
			DocumentVector v = (DocumentVector) results[i];
//			System.out.println("Document: " + v.key + " ==== Value: " + v.value);
			final_results.add(v.key);
		}
		return final_results;
	}
	
	public static ArrayList<String> atcatc2(HashMap<String, HashMap<String,Integer>> db, HashMap<String, Integer> tokenized, int num_results){
		double total_sum = 0.0;
		
		/*
		 * Calculate idf of all terms in docs
		 * Then calculate tf vector for all terms in all docs
		 * Then multiple tf*idf for all terms in all docs
		 * Then normalize the tf*idf values of all terms in all docs into db_document_tifidf
		 */
		HashMap<String, Double> idf = calculate_idf(db, tokenized);
		
		HashMap<String, HashMap<String,Double>> db_document_tfidf_normalized = new HashMap<String, HashMap<String,Double>>();
		
		for(String document : db.keySet()){
			HashMap<String, Double> document_tf_vector = calculate_doc_tf(db.get(document));
			
			HashMap<String,Double> document_tfidf = new HashMap<String,Double>();
			
			for(String term: document_tf_vector.keySet()){
				double value = document_tf_vector.get(term) * idf.get(term);
				document_tfidf.put(term, value);
			}
			
			db_document_tfidf_normalized.put(document, normalized(document_tfidf));
		}
		
		/* Use previously calculated idf of all terms in docs
		 * Then calculate tf vector for all terms in query (tokenized variable which already has counts)
		 * Then multiple tf*idf for all terms in query
		 * Then normalize the tf*idf values of all terms in the query into query_tfidf
		 */
		HashMap<String, Double> query_tf_vector = calculate_query_tf(tokenized);
		HashMap<String, Double> query_tfidf = new HashMap<String, Double>();
		for (String term : tokenized.keySet()){
			if(idf.containsKey(term)){
				double query_value = query_tf_vector.get(term);
				double idf_value = idf.get(term);
				query_tfidf.put(term, query_value*idf_value);
			} else {
				query_tfidf.put(term, 0.0);
			}
		}
		
		HashMap<String, Double> query_tfidf_normalized = normalized(query_tfidf);
		
		/*
		 * Take the dot product of db_document_tfidf_normalized and query_tfidf_normalized
		 */
		HashMap<String, Double> dot_product_docs = new HashMap<String, Double>();
		for(String document : db_document_tfidf_normalized.keySet()){	
			HashMap<String, Double> current_doc = db_document_tfidf_normalized.get(document);
			double running_total = 0.0;
			for (String term : query_tfidf_normalized.keySet()){
				double value = query_tfidf_normalized.get(term) * (current_doc.containsKey(term) ? current_doc.get(term) : 0.0); 
				running_total += value;		
			}
			dot_product_docs.put(document, running_total);
		}
		
		//Collections.sort(dot_product_docs);
		
		return getTop(dot_product_docs, num_results);
	}
	
	
	public static double atcatc(ArrayList<String> actual, HashSet<String> answers, HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		double total_sum = 0.0;
		HashMap<String,Integer>term_freq = new HashMap<String,Integer>();
		HashMap<String,Integer>max_term_freq = new HashMap<String,Integer>();
		for(String term : tokenized){
			max_term_freq.put(term,0);
		}
		for(String term : tokenized){
			for(String str : db.keySet()){
				if(db.get(str).containsKey(term)){
					if(term_freq.containsKey(str)){
						term_freq.put(str,term_freq.get(str) + db.get(str).get(term));
					}else{
						term_freq.put(term, db.get(str).get(term));
					}
					max_term_freq.replace(term, Math.max(max_term_freq.get(term), db.get(str).get(term)));
				}
			}
			if(!term_freq.containsKey(term)){
				term_freq.put(term, 0);
			}
		}
		
		
		for(String term: tokenized){
			
			double ri = 0.0;
			double ni = 0.0;
			double n = (double)db.size();
			double r = (double)answers.size();
			double avdl = 0.0;
				
			for(String str : answers){
				if(db.get(str).get(term) != null){
					ri += 1.0;
				}
			}
			for(String str : db.keySet()){
				if(db.get(str).get(term) != null){
					ni += 1.0;
				}
				avdl += (double) db.get(str).size();
			}
			
			
			double a = 0.0;
			double t = 0.0;
			double c = 0.0;
			if(term_freq.containsKey(term) && term_freq.get(term) != 0 && max_term_freq.get(term) != 0 && ni != 0){
				a = 0.5 + (term_freq.get(term)/(max_term_freq.get(term)));
				System.out.println("a: " + a);
				t = Math.log(n/ni);
				c = Math.pow(term_freq.get(term),2);
			}
			if(c != 0.0){
				c = 1/Math.pow(c, 0.5);
			}
			total_sum += a*t*c;
		}
		return total_sum/actual.size();
	}
	
	public static double atnatn(ArrayList<String> actual, HashSet<String> answers, HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		double total_sum = 0.0;
		HashMap<String,Integer>term_freq = new HashMap<String,Integer>();
		HashMap<String,Integer>max_term_freq = new HashMap<String,Integer>();
		for(String term : tokenized){
			max_term_freq.put(term,0);
		}
		for(String term : tokenized){
			for(String str : db.keySet()){
				if(db.get(str).containsKey(term)){
					if(term_freq.containsKey(str)){
						term_freq.put(str,term_freq.get(str) + db.get(str).get(term));
					}else{
						term_freq.put(term, db.get(str).get(term));
					}
					max_term_freq.replace(term, Math.max(max_term_freq.get(term), db.get(str).get(term)));
				}
			}
			if(!term_freq.containsKey(term)){
				term_freq.put(term, 0);
			}
		}
		
		
		for(String term: tokenized){
			
			double ri = 0.0;
			double ni = 0.0;
			double n = (double)db.size();
			double r = (double)answers.size();
			double avdl = 0.0;
				
			for(String str : answers){
				if(db.get(str).get(term) != null){
					ri += 1.0;
				}
			}
			for(String str : db.keySet()){
				if(db.get(str).get(term) != null){
					ni += 1.0;
				}
				avdl += (double) db.get(str).size();
			}
			
			
			double a = 0.0;
			double t = 0.0;
			if(term_freq.containsKey(term) && term_freq.get(term) != 0){
				a = 0.5 + ((0.5*term_freq.get(term))/(max_term_freq.get(term)));
				t = Math.log(n/ni);
			}
	
			total_sum += a*t;
		}
/*		*/
		return total_sum/actual.size();

	}
	public static double annbpn(ArrayList<String> actual, HashSet<String> answers, HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		double total_sum = 0.0;
		HashMap<String,Integer>term_freq = new HashMap<String,Integer>();
		HashMap<String,Integer>max_term_freq = new HashMap<String,Integer>();
		for(String term : tokenized){
			max_term_freq.put(term,0);
		}
		for(String term : tokenized){
			for(String str : db.keySet()){
				if(db.get(str).containsKey(term)){
					if(term_freq.containsKey(str)){
						term_freq.put(str,term_freq.get(str) + db.get(str).get(term));
					}else{
						term_freq.put(term, db.get(str).get(term));
					}
					max_term_freq.replace(term, Math.max(max_term_freq.get(term), db.get(str).get(term)));
				}
			}
			if(!term_freq.containsKey(term)){
				term_freq.put(term, 0);
			}
		}
		
		
		for(String term: tokenized){
			
			double ri = 0.0;
			double ni = 0.0;
			double n = (double)db.size();
			double r = (double)answers.size();
			double avdl = 0.0;
				
			for(String str : answers){
				if(db.get(str).get(term) != null){
					ri += 1.0;
				}
			}
			for(String str : db.keySet()){
				if(db.get(str).get(term) != null){
					ni += 1.0;
				}
				avdl += (double) db.get(str).size();
			}
			
			
			double a = 0.0;
			double p = 0.0;
			if(term_freq.containsKey(term) && term_freq.get(term) != 0){
				a = (0.5 + ((0.5*term_freq.get(term))/(max_term_freq.get(term))));
				p = Math.max(0, Math.log(Math.abs(n-ni))/ni);
			}
	
			total_sum += a*p;
		}
/*		*/
		return total_sum/actual.size();
	}
	
	public static double my_weighting(ArrayList<String> actual, HashSet<String> answers, HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		//There is something to be said for simplicity. Also, I haven't
		//slept in 48 hours, I've had 8 hours in the last week, and I will not get to sleep for another 15-20
		//after writing this, so try not to take it too badly. Thanks.
		//-Connor
		return 2.0;
	}
	
	
	
	public static HashMap<String, Integer> AnalyzeThings(BufferedReader buff) throws IOException{
		StringBuffer stringBuffer = new StringBuffer();
		String line = null;
		while((line = buff.readLine())!=null){
		   stringBuffer.append(line).append("\n");
		}
				
		HashMap<String, Integer> result = tokenizeString(stringBuffer.toString());
		return result;
	}

	public static void createdb(String thingy,String associated){
		String database_name = "data/"+ associated + "database.txt";
		try{
			File database = new File(database_name);
			if (!database.exists()) {
				database.createNewFile();
			}
			database.setWritable(true);
			FileWriter filewriter = new FileWriter(database.getAbsoluteFile());
			BufferedWriter writer = new BufferedWriter(filewriter);
			writer.write(thingy);
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static String encode_db_line(HashMap<String,Integer> tokens, File file){
		HashMap <String, Integer> freq = new HashMap<String,Integer>();
		for(String g : tokens.keySet()){
			if(freq.containsKey(g)){
				freq.replace(g, freq.get(g) + 1);
			}else{
				freq.put(g, 1);
			}
		}
		String res = file.getName().split("\\.")[0] + "|";
		for(String r : freq.keySet()){
			res += r + "," + freq.get(r).toString() + "|";
		}
		return res+"\n";
	}
	
	public static HashMap<String, HashMap<String,Integer>> reassemble(String associated){
		String database_name = "data/"+ associated + "database.txt";
		HashMap<String, HashMap<String,Integer>> results = new HashMap<String, HashMap<String,Integer>>();
		File database = new File(database_name);
		if (database.canRead()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(database);
			} catch (FileNotFoundException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
				String line;
				while ((line = reader.readLine()) != null){
					String[] temp_array = line.split("\\|");
					HashMap<String,Integer> temp_hash = new HashMap<String,Integer>();
					for(int j = 1; j < temp_array.length; j++){
						String[] g = temp_array[j].split("\\,");
						if(g.length <= 2){
							temp_hash.put(g[0], Integer.parseInt(g[1]));
						}else{
							String o = "";
							for(int h = 0; h < g.length - 1; h++){
								o += g[h];
							}
							temp_hash.put(o, Integer.parseInt(g[g.length - 1]));
						}
						
					}
					results.put(temp_array[0], temp_hash);
				}
			} catch (IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			} finally {
				try {
					fis.close();
				} catch(IOException e) {
					System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
				}
			}
		}
		return results;
	}
	
	public static HashMap<String, Integer> tokenizeString(String str) throws IOException{
		Analyzer analyzer = new MyAnalyzer(stopwords);		
		HashMap<String, Integer> result = new HashMap<String, Integer>();
        TokenStream stream  = analyzer.tokenStream(null, new StringReader(str));
		stream.reset();
        try {
            while(stream.incrementToken()) {
            	String term = stream.getAttribute(CharTermAttribute.class).toString();
            	if (result.containsKey(term)){
            		result.put(term, result.get(term) + 1);
            	} else {
            		result.put(term,  1);
            	}
            }
        }
        catch(IOException e) {
            // not thrown b/c we're using a string reader...
        }
        stream.close();
		return result;
	}
	
//	public static void update_crap(List<String> tokens){
//		total_docs;
//		docs_by_rel = new HashMap<String,Integer>(); //term to # rel_docs
//		docs_with_term = new HashMap<String,Integer>(); //term to # total_docs
//		relevant_doc_num = new HashMap<String,Integer>();
//		HashMap<String,HashMap <String,Integer>> term_freq_in_doc = new HashMap<String,HashMap <String,Integer>> ();
//		HashMap<String,Integer>term_freq_in_query = new HashMap<String,Integer>();
//	}

	
}

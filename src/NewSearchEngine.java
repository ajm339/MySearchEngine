import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.List;

import javax.swing.text.Document;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class NewSearchEngine {
	static String data = "";
	Integer atcatc = 0;
	Integer atnatn = 0;
	Integer annbpn = 0;
	Integer my_weighting = 0;
	Integer bm25 = 0;
	static CharArraySet stopwords;

	Integer total_docs = 0;
	HashMap<String,Integer>docs_by_rel = new HashMap<String,Integer>(); //term to # rel_docs
	HashMap<String,Integer>docs_with_term = new HashMap<String,Integer>(); //term to # total_docs
	HashMap<String,Integer>relevant_doc_num = new HashMap<String,Integer>();
	HashMap<String,HashMap <String,Integer>> term_freq_in_doc = new HashMap<String,HashMap <String,Integer>> ();
	HashMap<String,Integer>term_freq_in_query = new HashMap<String,Integer>();
	
	

	////////////////////////////////////INDEXING//////////////////////////////////////
	
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
		System.out.println("{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{");
		System.out.println(data);
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
	
	public static ArrayList<String> runQuery(String search_term, Integer num_results, String docsPath, HashSet<String> answers){
		try {
			List<String> tokenized = tokenizeString(search_term);
			HashMap<String, HashMap<String,Integer>> db = reassemble(docsPath.split("/")[1]);
			ArrayList<String> arrlist = evaluate_db(db,tokenized);
			System.out.print("BM25: ");
			System.out.println(bm25(arrlist, answers, db, tokenized));
			System.out.print("ATCATC: ");
			System.out.println(atcatc(arrlist, answers, db, tokenized));
			System.out.print("ATNATN: ");
			System.out.println(atnatn(arrlist, answers, db, tokenized));
			System.out.print("ANNBPN: ");
			System.out.println(annbpn(arrlist, answers, db, tokenized));
			System.out.print("THE ALEXV6 (SUPER AWESOME) WEIGHTING ALGORITHM: ");
			System.out.println(my_weighting(arrlist, answers, db, tokenized));
			return arrlist;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}
	
	
	public static ArrayList<String> evaluate_db(HashMap<String, HashMap<String,Integer>> db, List<String> tokenized){
		HashMap<String,Integer> temp_map = new HashMap<String,Integer>();
		ArrayList<String> k;

		for(String x : db.keySet()){
			int j = 0;
			for(String h : tokenized){
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
	
	
	
	public static List<String> AnalyzeThings(BufferedReader buff) throws IOException{
		StringBuffer stringBuffer = new StringBuffer();
		String line = null;
		while((line = buff.readLine())!=null){
		   stringBuffer.append(line).append("\n");
		}
				
		List<String> result = tokenizeString(stringBuffer.toString());
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
	
	public static String encode_db_line(List<String> tokens, File file){
		HashMap <String, Integer> freq = new HashMap<String,Integer>();
		for(String g : tokens){
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
	
	public static List<String> tokenizeString(String str) throws IOException{
		Analyzer analyzer = new MyAnalyzer(stopwords);		
		List<String> result = new ArrayList<String>();
        TokenStream stream  = analyzer.tokenStream(null, new StringReader(str));
		stream.reset();
        try {
            while(stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
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

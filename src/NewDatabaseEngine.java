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


public class NewDatabaseEngine {
	public static String data = "";
	public static CharArraySet stopwords;
	public File database;
	public static String database_name;
	public static float atcatc = 0;
	public static float atnatn = 0;
	public static float annbpn = 0;
	public static float my_weighting = 0;
	public static float bm25 = 0;
	public static HashSet<String> rel_docs;

	public static HashMap<String,Integer> docs_by_rel; //term to # rel_docs
	public static HashMap<String,Integer> docs_with_term; //term to # total_docs
	public static int total_docs = 0;
	public static HashMap<String,Integer> relevant_doc_num;
	public static HashMap<String,HashMap <String,Integer>> term_freq_in_doc;
	public static HashMap<String,Integer> term_freq_in_query;
	
	public static void adjust_atcatc(){
		
	}
	public static void adjust_atnatn(){
		
	}
	public static void adjust_annbpn(){
		
	}
	public static void adjust_my_weighting(){
		
	}
	public static void adjust_bm25(){
		
	}
	
	public static void setStopwords(CharArraySet stops){
		stopwords = stops;
	}
	
	public static void buildIndex(String indexDir, String docsPath, CharArraySet stops) {
		data = "";
		atcatc = 0;
		atnatn = 0;
		annbpn = 0;
		my_weighting = 0;
		bm25 = 0;

		docs_by_rel = new HashMap<String,Integer>(); //term to # rel_docs
		docs_with_term = new HashMap<String,Integer>(); //term to # total_docs
		total_docs = 0;
		relevant_doc_num = new HashMap<String,Integer>();
		term_freq_in_doc = new HashMap<String,HashMap <String,Integer>> ();
		term_freq_in_query = new HashMap<String,Integer>();
		
		
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
		data = "";
		
		
		
		
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
					
//					BufferedReader buff = new BufferedReader(new InputStreamReader(fis));
//					StringBuffer stringBuffer = new StringBuffer();
//					String line = null;
//					while((line =buff.readLine())!=null){
//					   stringBuffer.append(line).append("\n");
//					}
//					String[] words = stringBuffer.toString().split("\\s+");
//					System.out.println(stringBuffer.toString());
					
					List<String> tokens = AnalyzeThings(new BufferedReader(new InputStreamReader(fis)));
					data += file.getName() + tokens.toString() + "\n";
					

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
	
	public static List<String> tokenizeString(String str) throws IOException{
		Analyzer analyzer = new StandardAnalyzer(stopwords);		
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
	
	public static List<String> AnalyzeThings(BufferedReader buff) throws IOException{
		StringBuffer stringBuffer = new StringBuffer();
		String line = null;
		while((line = buff.readLine())!=null){
		   stringBuffer.append(line).append("\n");
		}
				
		List<String> result = tokenizeString(stringBuffer.toString());
        
//		String[] words = stringBuffer.toString().split("\\s+");
//		System.out.println(stringBuffer.toString());
//		
//		HashMap map = new HashMap();
//		for(String x : words){
//			map.put(x, map.get(x) == null ? ((int)map.get(x) + 1) : 1);
//		}
//		System.out.println("'''''''''''''''''''''''''''''''''''''");
//		System.out.println(map.toString());
//		return map;
		return result;
	}
	
	public static void createdb(String thingy,String associated){
		database_name = "data/"+ associated + "database.txt";
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
	
	
	public static ArrayList<String> runQuery(String check_term_line, int numResults, HashSet<String> answers) throws IOException{
		rel_docs = answers;
		File database = new File(database_name);
		if (!database.exists()) {
			return new ArrayList<String>();
		}
		database.setReadable(true);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(database);
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
		
		BufferedReader buff = new BufferedReader(new InputStreamReader(fis));
		ArrayList<ArrayList <String>> stringArray = new ArrayList<ArrayList <String>>();
		String line = null;
		ArrayList<String> check_terms = (ArrayList<String>)tokenizeString(check_term_line);
		while((line = buff.readLine())!=null){
		   stringArray.add(check_line(line,check_terms));
		}
		
		
		Collections.sort(stringArray, new DocListComparator());
		
		ArrayList <String> resultArray = new ArrayList <String>();
		for( ArrayList<String> x : stringArray.subList(0,numResults)){
			resultArray.add(x.get(0));
		}
		System.out.println(check_term_line);
		System.out.println(resultArray);
		
		
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();		
//		System.out.println(docs_by_rel); //term to # rel_docs
//		System.out.println(docs_with_term);
//		System.out.println(total_docs);
//		System.out.println(relevant_doc_num);
//		System.out.println(term_freq_in_doc);
//		System.out.println(term_freq_in_query);
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
		
		
		return resultArray;
		
	}
	
	static class DocListComparator implements Comparator<ArrayList<String>>
	 {
	     public int compare(ArrayList<String> l1, ArrayList<String> l2)
	     {
	    	 //Sorts in reverse
	         return ((Integer)Integer.parseInt(l2.get(1))).compareTo((Integer)Integer.parseInt(l1.get(1)));
	     }
	 }
	
	public static ArrayList <String> check_line(String line, List<String> check_terms){
		Integer res = 0;
		String[] x = line.substring(0, line.length()-1).split("[\\[\\]]");
		String topic = x[0];
		List<String> terms = Arrays.asList(x[1].split("\\s*,\\s*"));
		
		for(String ter : terms){
			if(rel_docs.contains(topic)){
				if(docs_by_rel.get(ter) == null){
					docs_by_rel.put(ter,1);
				}else{
					docs_by_rel.put(ter,docs_by_rel.get(ter)+1);
				}
			}
			docs_with_term.put(ter, (docs_with_term.get(ter) == null) ? 0 : docs_with_term.get(ter)+1);
			term_freq_in_query.put(ter, (term_freq_in_query.get(ter) == null) ? 0 : term_freq_in_query.get(ter)+1);
			String k = ter;
//			System.out.println(topic);
			if(term_freq_in_doc.get(topic) == null){
				term_freq_in_doc.put(topic, new HashMap <String,Integer>());
				term_freq_in_doc.get(topic).put(k, 0);
			}else if(term_freq_in_doc.get(topic).get(ter) == null){
				term_freq_in_doc.get(topic).put(ter, 1);
			}else{
				term_freq_in_doc.get(topic).put(ter, term_freq_in_doc.get(topic).get(ter) + 1);	
			}
			term_freq_in_query.put(ter, term_freq_in_query.get(ter) + 1);
			
			relevant_doc_num.put(ter, rel_docs.size());
						
		}
		total_docs++;
		
		
		
		
		
		
		for(String g : terms){
			for(String k : check_terms){
				if (g.equals(k)){
					res++;
				}
			}
		}
		ArrayList<String> result = new ArrayList<String>();
		result.add(topic);
		result.add(res.toString());
		
		return result;
	}

}

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
import java.util.HashMap;
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


public class NewDatabaseEngine {
	public static String data = "";
	public static CharArraySet stopwords;
	
	public static void setStopwords(CharArraySet stops){
		stopwords = stops;
	}
	
	public void createdb(String thingy){
		try{
			File database = new File("/data/database.txt");
			if (!database.exists()) {
				database.createNewFile();
			}
			FileWriter filewriter = new FileWriter(database.getAbsoluteFile());
			BufferedWriter writer = new BufferedWriter(filewriter);
			writer.write(thingy);
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void IndexFiles(){		
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
					data += file.getAbsolutePath() + tokens.toString() + "\n";
					

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
		Analyzer analyzer = new StandardAnalyzer(new CharArraySet(10,true));		
		List<String> result = new ArrayList<String>();
        TokenStream stream  = analyzer.tokenStream(null, new StringReader(str));
		
        try {
            while(stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        }
        catch(IOException e) {
            // not thrown b/c we're using a string reader...
        }
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
	
	
	public void runQuery(String check_term_line) throws IOException{
		File database = new File("/data/database.txt");
		if (!database.exists()) {
			return;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(database);
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
		BufferedReader buff = new BufferedReader(new InputStreamReader(fis));
		ArrayList<ArrayList <String>> stringArray = new ArrayList<ArrayList <String>>();;
		String line = null;
		ArrayList<String> check_terms = (ArrayList<String>)tokenizeString(check_term_line);
		while((line = buff.readLine())!=null){
		   stringArray.add(check_line(line,check_terms));
		}
		
		
	}
	
	public ArrayList <String> check_line(String line, List<String> check_terms){
		Integer res = 0;
		String[] x = line.split("[");
		String topic = x[0];
		List<String> terms = Arrays.asList(x[1].substring(0, x[1].length()-1).split("\\s*,\\s*"));
		for(String g : terms){
			for(String k : check_terms){
				if (g == k){
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

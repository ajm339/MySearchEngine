import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
// import lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class EvaluateQueries {
	
	public static double meanmeanaverageaverageprecisionprecision = 0;
	
	public static void main(String[] args) throws IOException {
		String cacmDocsDir = "data/cacm"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents
		
		String cacmIndexDir = "data/index/cacm"; // the directory where index is written into
		String medIndexDir = "data/index/med"; // the directory where index is written into
		
		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file
		
		int cacmNumResults = 100;
		int medNumResults = 100;

	    // CharArraySet stopwords = new CharArraySet(Version.LUCENE_44,0,false);
	    CharArraySet stopwords = new CharArraySet(0, false);
//		evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
//				cacmAnswerFile, cacmNumResults, stopwords);

		
		evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, medNumResults, stopwords);

	}
	
	private static Map<Integer, String> loadQueries(String filename) {
		HashMap<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), line
						.substring(pos + 1));
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}

	private static double precision(HashSet<String> answers, List<String> results) {
		double matches = 0;
		for (String result : results) {
			if (answers.contains(result))
				matches++;
		}

		return matches / results.size();
	}
	
	private static double averagePrecision(HashSet<String> answers, List<String> results) {
		double matches = 0.0;
		double accurate = 0.0;
		double total = 0.0;
		for (String result : answers) {
			total++;
			if (results.contains(result.replaceAll(".txt",""))){
				accurate++;
				matches += accurate/total;
			}
		}
		matches *= (2.0/3.0);
		return matches / answers.size();
	}
	
	private static void evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, int numResults,
			CharArraySet stopwords) throws IOException {

		// Build Index
		NewSearchEngine.buildIndex(indexDir, docsDir, stopwords);

		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);

		// Search and evaluate
		String[] finarr = new String[queries.size()];
		double sum = 0.0;
//		System.out.println(queries.keySet());
		for (Integer i : queries.keySet()) {
				ArrayList<String> results = NewSearchEngine.runQuery(queries.get(i), numResults, docsDir, queryAnswers.get(i));
				HashMap<String, Integer> tokenized = NewSearchEngine.tokenizeString(queries.get(i));
				
				ArrayList<String> clustering_results = CompleteClustering.analyze(20, results);
				
				double averagePrecision = averagePrecision(queryAnswers.get(i), results);
				double averagePrecision2 = averagePrecision(queryAnswers.get(i), clustering_results);
				sum += averagePrecision;

				System.out.printf("\n Topic %d  \n", i);

				if(i==20){
					System.out.println(tokenized);
					System.out.println("Results: " + results);
					NewSearchEngine.print_results(results);
				}

				finarr[i-1] = NewSearchEngine.Rocchio(4.0,16.0,0.0,tokenized, results);

				System.out.println("Answers: " + queryAnswers.get(i));
				System.out.println(averagePrecision);

				
		}

		System.out.println("MAP: " + sum/queries.size());
		for(int x = 0; x < finarr.length; x++){
			System.out.println(Integer.toString(x+1)+ ": " + finarr[x]);
		}
	}
	
	
}

package com.neu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 * To create Apache Lucene index in a folder and add files into this index based
 * on the input of the user.
 */
public class SmallSearchEngine {
	private static Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_47);
	private IndexWriter writer;
	private ArrayList<File> queue = new ArrayList<File>();

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		System.out.println("Enter the FULL path where the index should be created: (e.g. /Usr/index or c:\\temp\\index)");

		String indexLocation = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();

		SmallSearchEngine indexer = null;
		try {
			indexLocation = line;
			//Checks if the path provided is valid; If yes then make that the index storing location
			indexer = new SmallSearchEngine(line);
		} catch (Exception ex) {
			System.out.println("Cannot create index..." + ex.getMessage());
			System.exit(-1);
		}

		// Read input from  the User. Or press q for exiting the program
		while (!line.equalsIgnoreCase("q")) {
			try {
				System.out.println("Enter the FULL path to add into the index (q=quit):");
				System.out.println("[Acceptable file types: .html]");
				line = br.readLine();
				if (line.equalsIgnoreCase("q")) {
					break;
				}

				// try to add file into the index
				indexer.indexFileOrDirectory(line);
				
				break;
			} catch (Exception e) {
				System.out.println("Error indexing " + line + " : "+ e.getMessage());
			}
		}

		//Call the close Index as indexing is now complete
		indexer.closeIndex();

		//Open the Index created
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
		
		//Create a List of terms and term frequency model
		createTermFrequencyList(reader);
		
		IndexSearcher searcher = new IndexSearcher(reader);
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);

		line = "";
		while (!line.equalsIgnoreCase("q")) {
			try {
				System.out.println("Enter the search query (q=quit):");
				
				line = br.readLine();
				
				if (line.equalsIgnoreCase("q")) {
					break;
				}

				Query q = new QueryParser(Version.LUCENE_47, "contents",analyzer).parse(line);
				searcher.search(q, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

				// 4. display results
				System.out.println("Found " + hits.length + " hits.");
				PrintWriter writer = new PrintWriter("query - "+line+".txt", "UTF-8");
				for (int i = 0; i < hits.length; ++i) {
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
					String printed = (i + 1) + ". " + d.get("filename") + " score=" + hits[i].score
							+" snippet is - "+snippetGeneration(d, line);
					writer.println(printed);
					System.out.println((i + 1) + ". " + d.get("filename") + " score=" + hits[i].score);
				}
				writer.close();

			} catch (Exception e) {
				System.out.println("Error searching " + line + " : "+ e.getMessage());
				break;
			}

		}

	}
	
	
	/** 
	 * Generate snippets starting from the word occurring earliest in the query
	 * @param doc
	 * @param s
	 * @return
	 */
	private static String snippetGeneration(Document doc, String s){
		
		String document = doc.get("contents").replace("\n", " ").replace("\t", " ");
		String[] words = s.split(" "); 
		int firstindex = -1;
		for(String word : words){
			int i = document.indexOf(word);
			if(i != -1){
				if(firstindex == -1){
					firstindex = i;
				}
				else if(firstindex > i){
					firstindex = i;
				}
			}
		}
		if(firstindex == -1){
			firstindex = 0;
		}
		int lastIndex = (document.length() < (firstindex+200)) ? document.length() : (firstindex+ 200); 
		return document.substring(firstindex, lastIndex);
		
	}

	/**
	 * Constructor
	 * 
	 * @param indexDir
	 *            the name of the folder in which the index should be created
	 * @throws java.io.IOException
	 *             when exception creating index.
	 */
	SmallSearchEngine(String indexDir) throws IOException {

		FSDirectory dir = FSDirectory.open(new File(indexDir));

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);

		writer = new IndexWriter(dir, config);
	}

	/**
	 * Indexes a file or directory
	 * gets the list of files in a folder (if user has submitted
	 * the name of a folder) or gets a single file name (is user
	 * has submitted only the file name) 
	 * @param fileName
	 *            the name of a text file or a folder we wish to add to the
	 *            index
	 * @throws java.io.IOException
	 *             when exception
	 */
	public void indexFileOrDirectory(String fileName) throws IOException {

		addFiles(new File(fileName));

		int originalNumDocs = writer.numDocs();
		for (File f : queue) {
			FileReader fr = null;
			try {
				Document doc = new Document();

				//contents are added to file
				fr = new FileReader(f);
				
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(fr);
				
				//Remove the tags from the html pages
				String line;
				while ( (line=br.readLine()) != null) {
					sb.append(line).append(System.getProperty("line.separator"));
				}
				String nohtml = sb.toString().replaceAll("\\<.*?>","");
				
				doc.add(new TextField("contents", nohtml, Field.Store.YES));
				doc.add(new StringField("path", f.getPath(), Field.Store.YES));
				doc.add(new StringField("filename", f.getName(),
						Field.Store.YES));

				writer.addDocument(doc);
				System.out.println("Added: " + f);
			} catch (Exception e) {
				System.out.println("Could not add: " + f);
			} finally {
				fr.close();
			}
		}

		int newNumDocs = writer.numDocs();
		System.out.println("");
		System.out.println("====================================");
		System.out.println((newNumDocs - originalNumDocs) + " documents were added.");
		System.out.println("====================================");

		queue.clear();
	}

	/**
	 * Adds all the relevant files to the queue
	 * @param file
	 */
	private void addFiles(File file) {

		//Check if the file exists
		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		
		//Check if the file is a directory
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();
			// Add the html files to queue otherwise skip them
			if (filename.endsWith(".html")) {
				queue.add(file);
			} else {
				System.out.println("Skipped " + filename);
			}
		}
	}

	
	
	/**
	 * This method performs 2 functions.
	 * 1. Sort and print terms and term-frequency as per the decreasing order of frequency.
	 *     and print this to file.
	 * 2. Create a file containing the rank and probability for creation of zipf's graph
	 * @param reader
	 * @return
	 * @throws IOException
	 * 
	 * Reference - stackoverflow.com
	 */
	private static void createTermFrequencyList(IndexReader reader) throws IOException{
		
		System.out.println("");
		System.out.println("====================================");
		System.out.println("Started creating Term Frequency List");
		System.out.println("====================================");
		
		Set<String> wordsList = new HashSet<String>();
		Bits liveDocs = MultiFields.getLiveDocs(reader);
		
		String field = "contents";

		TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(null);
		BytesRef bytesRef;
		while ((bytesRef = termEnum.next()) != null) {
			if (termEnum.seekExact(bytesRef)) {
				DocsEnum docsEnum = termEnum.docs(liveDocs, null);
				if (docsEnum != null) {
					int doc;
					while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
						String word=bytesRef.utf8ToString();
						wordsList.add(word);
					}
				}
			}
		}
		
		HashMap<String, Long> wordFreq = new HashMap<String, Long>();
		for(String everyWord : wordsList){
			Term termInstance = new Term("contents", everyWord);
			long termFreq = reader.totalTermFreq(termInstance);
			wordFreq.put(everyWord, termFreq);
						
		}
		//Sort by Frequency in descending order
		wordFreq = sortHashMapByValuesD(wordFreq);
		
		int rank = 1;
		double totalWords = wordsList.size();
		
		//Print the term and term-freq output
		PrintWriter writer = new PrintWriter("term and term-freq.txt", "UTF-8");
		
		//Print the Rank and the Probability of the occurance of the term
		PrintWriter writer2 = new PrintWriter("Zipf's law values.txt", "UTF-8");
		writer.println("Term\tTerm-Frequency");
		writer2.println("Rank\tProb.OfTerm");
		for(String word : wordFreq.keySet()){

			//Get the term Frequency
			long termFreq = wordFreq.get(word);
			writer.println(word+"\t"+termFreq);
			
			//Get the probability of the term
			double probabilityOfTerm = (double)termFreq/(double)totalWords;
			writer2.println(rank++ +"\t"+probabilityOfTerm);
		}
		writer.close();
		writer2.close();
		
		System.out.println("");
		System.out.println("====================================");
		System.out.println("Finished creating Term Frequency List");
		System.out.println("====================================");
		
	}

	/**
	 * Closes the index.
	 * @throws java.io.IOException
	 */
	public void closeIndex() throws IOException {
		writer.close();
	}
	
	/**
	 * Sort by values in the Map in descending order
	 * @param passedMap
	 * @return
	 * Reference - stackoverflow.com 
	 */
	public static LinkedHashMap sortHashMapByValuesD(HashMap<String, Long> passedMap) {
		   List<String> mapKeys = new ArrayList<String>(passedMap.keySet());
		   List<Long> mapValues = new ArrayList<Long>(passedMap.values());
		   Collections.sort(mapValues);
		   Collections.reverse(mapValues);
		   Collections.sort(mapKeys);

		   LinkedHashMap sortedMap = new LinkedHashMap();

		   Iterator valueIt = mapValues.iterator();
		   while (valueIt.hasNext()) {
		       Object val = valueIt.next();
		       Iterator keyIt = mapKeys.iterator();

		       while (keyIt.hasNext()) {
		           Object key = keyIt.next();
		           String comp1 = passedMap.get(key).toString();
		           String comp2 = val.toString();

		           if (comp1.equals(comp2)){
		               passedMap.remove(key);
		               mapKeys.remove(key);
		               sortedMap.put((String)key, (Long)val);
		               break;
		           }
		       }
		   }
		   return sortedMap;
		}
}
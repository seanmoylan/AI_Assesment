/*
	Sean Moylan
	G00299424

	References:
	https://jsoup.org/cookbook/extracting-data/dom-navigation
	http://pages.cs.wisc.edu/~hasti/cs302/examples/Parsing/parseString.html
	https://dzone.com/articles/how-to-sort-a-map-by-value-in-java-8

 */

package ie.gmit.sw;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

import ie.gmit.sw.ai.cloud.WordFrequency;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.stream.Collectors;


public class NodeParser {
	private final int MAX = 20;
	private final int TITLE_WEIGHT = 80;
	private final int HEADING1_WEIGHT = 30;
	private final int PARAGRAPH_WEIGHT = 1;
	private Map<String, Integer> wordMap = new TreeMap<>();
	private WordFrequency[] words;
	private Map<String, Integer> sortedByCount;
	private String term;

	private List<String> ignoreWords;
	
	// A set used for checking if a given url has already been visited
	private Set<String> closed = new ConcurrentSkipListSet<>();
	private Queue<DocumentNode> queue = new PriorityQueue<>(Comparator.comparing(DocumentNode::getScore));
	
	public NodeParser(String url, String term) throws Exception {
		this.term = term;
		Document doc = Jsoup.connect(url + term).get();
		int score = getHeuristicScore(doc);

		loadIgnoreWords();
		// Add first URL to the closed Set
		closed.add(url);
		
		// Add Document node to the queue
		queue.offer(new DocumentNode(doc, score));		
		
		// Process
		search();

		// Print map to console
		sortedByCount = sortByValue(wordMap);

		WordFrequency[] words = new WordFrequency[20];
		words = getWordFrequencyKeyValue(20);


	}
	
	private void search() {
		while(!queue.isEmpty() && closed.size() <= MAX) {
			DocumentNode node = queue.poll();
			Document doc = node.getDocument();
			
			Elements edges = doc.select("a[href]");
			
			for(Element e : edges) {
				String link = e.absUrl("href");
				
				if(closed.size() <= MAX && link != null && !closed.contains(link)) {
					try {
						System.out.println(link);
						Document child = Jsoup.connect(link).get();
						int score = getHeuristicScore(child);
						closed.add(link);
						queue.offer(new DocumentNode(child, score));
					} catch (IOException ex) {
						
					}
					
				}
			}
		}
		
	}

	private int getHeuristicScore(Document doc) {
		// Initialize scores
		int heuristicScore = 0;
		double fuzzyScore = 0;
		int titleScore = 0;
		int headingScore = 0;
		int bodyScore = 0;

		// Assign titleScore
		titleScore += getFrequency(doc.title()) * TITLE_WEIGHT;

		String title = doc.title();

		System.out.println(closed.size() + " --> " + title);
		Elements headings = doc.select("h1");
		for (Element heading : headings){
			String h1 = heading.text();
			// Assign headingScore
			headingScore  += getFrequency(h1) * HEADING1_WEIGHT;
		}

		// Assign body score
		String body = doc.body().text();
		bodyScore  = getFrequency(body) * PARAGRAPH_WEIGHT;

		fuzzyScore = getFuzzyHeuristics(titleScore, headingScore, bodyScore);


		if(fuzzyScore > 20.0)
		{
			index(title, headings.text(), body);
		}

		heuristicScore = titleScore + headingScore + bodyScore;


		// Display the score that will be passed into wordcloud.fcl
		System.out.println("Fuzzy title to process: " + titleScore);
		System.out.println("Fuzzy heading to process: " + headingScore);
		System.out.println("Fuzzy body to process: " + bodyScore);

		System.out.println("Fuzzy score: " + fuzzyScore);

		return heuristicScore;
	}

	private int getFrequency(String s){
		int count = 0;
		String[] wordArray = parseString(s);
		try {
			for (String word : wordArray) {
				if (word.equalsIgnoreCase(term))
					count++;
			}
			System.out.println("Frequency Count: " + count);
			return count;
		}catch (Exception e){
			return 0;
		}

	}

	private String[] parseString(String... string){
		String fullString = String.join(" ", string);
		String delims = "[ ©–—”•↑★#,.\"\'/:$%&();?!| ]+";


		for (char c : fullString.toCharArray()) {
			if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
				String[] wordArray = fullString.split(delims);
				return wordArray;
			}
		}
		return null;

	}

	private void index(String... text){
		// String array "text" gets added to a single string

		String[] wordArray = parseString(text);

		for (String word : wordArray) {
			try{
				// Extract each word from the string and add to map after filtering with ignore words
				if(!ignoreWords.contains(word) && word.length() > 4){
					Integer n = wordMap.get(word);
					n = (n == null) ? 1 : ++n;
					wordMap.put(word, n);

				}
			}
			catch (Exception ex){

			}


		}
	}

	private double getFuzzyHeuristics(int titles, int headings, int body){

		FIS fis = FIS.load("./conf/wordcloud.fcl", true);
		FunctionBlock functionBlock = fis.getFunctionBlock("wordcloud");

		// Set Variables
		fis.setVariable("title", titles);
		fis.setVariable("heading", headings);
		fis.setVariable("body", body);

		// Evaluate
		fis.evaluate();

		// Get output variables
		Variable score = functionBlock.getVariable("score");

		return score.getValue();
	}


	public static void main(String[] args) throws Exception {
		new NodeParser("https://duckduckgo.com/html/?q=", "donald");


	}


	public WordFrequency[] getWordFrequencyKeyValue(int numberOfWords) {

		words = new WordFrequency[numberOfWords+1];
		int i = 1;
		for(Map.Entry<String, Integer> entry: sortedByCount.entrySet()){
			System.out.println("Word: " + entry.getKey() + " ---> Count: " + entry.getValue());
			words[i] = new WordFrequency(entry.getKey(),entry.getValue());

			if(i == numberOfWords){
				return words;
			}
			i++;
		}
		return words;
	}

	private static Map<String, Integer> sortByValue(final Map<String, Integer> wordCounts) {

		return wordCounts.entrySet()

				.stream()

				.sorted((Map.Entry.<String, Integer>comparingByValue().reversed()))

				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

	}

	private void loadIgnoreWords() throws FileNotFoundException {
		Scanner s = new Scanner(new File("/Users/seanmoylan/Desktop/AI_Assesment/src/ie/gmit/sw/ignorewords.txt"));
		ignoreWords = new ArrayList<>();
		while (s.hasNext()){
			ignoreWords.add(s.next());
		}
		s.close();
	}



	

}

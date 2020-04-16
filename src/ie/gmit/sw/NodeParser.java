/*
	Sean Moylan
	G00299424

	References:
	https://jsoup.org/cookbook/extracting-data/dom-navigation
	http://pages.cs.wisc.edu/~hasti/cs302/examples/Parsing/parseString.html
	https://dzone.com/articles/how-to-sort-a-map-by-value-in-java-8

 */

package ie.gmit.sw;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

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
	private Map<String, Integer> sortedByCount;
	private String term;
	
	// A set used for checking if a given url has already been visited
	private Set<String> closed = new ConcurrentSkipListSet<>();
	private Queue<DocumentNode> queue = new PriorityQueue<>(Comparator.comparing(DocumentNode::getScore));
	
	public NodeParser(String url, String term) throws Exception {
		this.term = term;
		Document doc = Jsoup.connect(url + term).get();
		int score = getHeuristicScore(doc);
		
		// Add first URL to the closed Set
		closed.add(url);
		
		// Add Document node to the queue
		queue.offer(new DocumentNode(doc, score));		
		
		// Process
		process();

		// Print map to console
		sortedByCount = sortByValue(wordMap);
		sortedByCount.entrySet().forEach(entry->{
			if(entry.getValue() > 20) {
				System.out.println(entry.getKey() + " " + entry.getValue());
			}
		});


	}
	
	private void process() {
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


		index(title, headings.text(), body);
		int temp = titleScore + headingScore + bodyScore;


		// Display the score that will be passed into wordcloud.fcl
		System.out.println("Fuzzy title to process: " + titleScore);
		System.out.println("Fuzzy heading to process: " + headingScore);
		System.out.println("Fuzzy body to process: " + bodyScore);

		System.out.println("Fuzzy score: " + fuzzyScore);

		return 0;
	}

	private int getFrequency(String s){
		int count = 0;
		String[] wordArray = parseString(s);
		for (String word : wordArray){
			if(word.equalsIgnoreCase(term))
				count++;
		}
		System.out.println("Frequency Count: " + count);
		return count;
	}

	private String[] parseString(String... string){
		String fullString = String.join(" ", string);
		String delims = "[ ©–—”•↑★#1234567890,.\"\'-/:$%&();?!| ]+";
		String[] wordArray = fullString.split(delims);

		return wordArray;
	}

	private void index(String... text){
		// String array "text" gets added to a single string

		String[] wordArray = parseString(text);

		for (String word : wordArray) {
			// Extract each word from the string and add to map after filtering with ignore words

			Integer n = wordMap.get(word);
			n = (n == null) ? 1 : ++n;
			wordMap.put(word, n);
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


		//if(fuzzy score is high)
		//	call index on the title, headings and body

		return score.getValue();
	}


	public static void main(String[] args) throws Exception {
		new NodeParser("https://duckduckgo.com/html/?q=", "trump");


	}


	public WordFrequency[] getWordFrequencyKeyValue() {
		// Convert Map to WordFrequency array



		WordFrequency[] words = new WordFrequency[4];



		return words;
	}

	public static Map<String, Integer> sortByValue(final Map<String, Integer> wordCounts) {

		return wordCounts.entrySet()

				.stream()

				.sorted((Map.Entry.<String, Integer>comparingByValue().reversed()))

				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

	}



	

}

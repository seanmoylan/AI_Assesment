package ie.gmit.sw;

import org.jsoup.nodes.Document;

public class DocumentNode {
	private Document d;
	private int score;
	
	public DocumentNode(Document doc, int score) {
		super();
		this.d = doc;
		this.score = score;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public Document getDocument() {
		return d;
	}
}

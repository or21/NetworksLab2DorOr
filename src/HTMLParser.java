import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class HTMLParser {

	ArrayList<String> m_Pictures;
	ArrayList<String> m_Videos;
	ArrayList<String> m_HtmlPages;

	private String m_HtmlIdentifier = "href=";
	private String m_ImgaeIdentifier = "img src=";
	private String m_VideoIdentifier = "video src=";

	public HTMLParser() {
		m_Pictures = new ArrayList<String>();
		m_Videos = new ArrayList<String>();
		m_HtmlPages = new ArrayList<String>();
	}

	public void parse() {
		File fileToRead = new File("test.html");

		try (BufferedReader br = new BufferedReader(new FileReader(fileToRead))) {
			String line;
			try {
				while ((line = br.readLine()) != null) {
					findAllRelevantData(line, m_HtmlIdentifier);
					findAllRelevantData(line, m_ImgaeIdentifier);
					findAllRelevantData(line, m_VideoIdentifier);
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (StringIndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		} 
		catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void findAllRelevantData(String line, String i_ObjectToLookFor) {
		String linkIdentifier = i_ObjectToLookFor;
		int indexEnd;
		char endIdentifier;
		if (line.contains(linkIdentifier)) {
			int indexHref = line.indexOf(linkIdentifier);
			endIdentifier = line.charAt(indexHref + linkIdentifier.length());
			indexEnd = line.indexOf(endIdentifier, indexHref + linkIdentifier.length() + 1);

			while (indexHref >= 0) {
				if (endIdentifier == '"' || endIdentifier == "'".charAt(0)) {
					String pageAddress = line.substring(indexHref, indexEnd + 1);
					System.out.println(pageAddress);
					if (isValidAddress(pageAddress)) {
						if (linkIdentifier.equals(m_HtmlIdentifier)) {
							m_HtmlPages.add(pageAddress);
						} else if (linkIdentifier.equals(m_ImgaeIdentifier)) {
							m_Pictures.add(pageAddress);
						}
					}
				}
				indexHref = line.indexOf(linkIdentifier, indexHref + 1);
				endIdentifier = line.charAt(indexHref + linkIdentifier.length());
				indexEnd = line.indexOf(endIdentifier, indexHref + linkIdentifier.length() + 1);
			}
		}
	}

	private boolean isValidAddress(String pageAddress) {
		// TODO: add validation of links
		return true;
	}
}

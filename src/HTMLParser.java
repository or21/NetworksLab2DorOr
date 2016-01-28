import java.util.ArrayList;

public class HTMLParser {

	ArrayList<String> m_Urls;

	private String m_HtmlIdentifier = "href=";
	private String m_ImageIdentifier = "img src=";
	private String m_VideoIdentifier = "video src=";

	public HTMLParser() {
		m_Urls = new ArrayList<>();
	}

	public ArrayList<String> parse(Response response) {
		String responseAsString = response.getContent();
		String replacedString = responseAsString.replaceAll("<", "\n<");
		String[] lines = replacedString.split("\n");

		for(String line : lines) {
			findAllRelevantData(line, m_HtmlIdentifier);
			findAllRelevantData(line, m_ImageIdentifier);
			findAllRelevantData(line, m_VideoIdentifier);
		}

		return m_Urls;
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
					if (isValidAddress(pageAddress)) {
						m_Urls.add(pageAddress);
					}
				}
				indexHref = line.indexOf(linkIdentifier, indexHref + 1);
				endIdentifier = line.charAt(indexHref + linkIdentifier.length());
				indexEnd = line.indexOf(endIdentifier, indexHref + linkIdentifier.length() + 1);
			}
		}
	}

	private boolean isValidAddress(String pageAddress) {
		return (pageAddress.startsWith(m_HtmlIdentifier + '"' + '/') || pageAddress.startsWith(m_HtmlIdentifier + "'" + '/'));
	}
}

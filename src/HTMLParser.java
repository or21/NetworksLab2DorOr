import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {

	ArrayList<String> m_Urls;

	private String m_HtmlOrVideoIdentifier = "<a href";
	private String m_ImageIdentifier = "<img src";
	private int m_NumberOfLinksFound = 0;

	public HTMLParser() {
		m_Urls = new ArrayList<>();
	}

	public ArrayList<String> parse(Response response) {
		String responseAsString = response.GetContent();
		String replacedString = responseAsString.replaceAll("<", "\n<");
		String[] lines = replacedString.split("\n");

		for(String line : lines) {
			findAllRelevantData(line, m_HtmlOrVideoIdentifier);
			findAllRelevantData(line, m_ImageIdentifier);
		}

		System.out.println(m_NumberOfLinksFound + " links extracted from " + response.GetUrl());
		return m_Urls;
	}

	private void findAllRelevantData(String line, String i_ObjectToLookFor) {
		String[] identifiers = i_ObjectToLookFor.split(" ");
		Pattern pattern = Pattern.compile(identifiers[0] + "\\s[^>]*?\\s*" + identifiers[1] + "=\\s*['\"]([^'\"]*?)['\"][^>]*?");
		
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			filterAddress(matcher.group(1), i_ObjectToLookFor);
		}
	}

	private void filterAddress(String i_PageAddress, String i_LinkIdentifier) {
		if (i_PageAddress.startsWith("/")) {
			m_Urls.add(i_PageAddress);
			m_NumberOfLinksFound++;
		}
		else {
			HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
		}
	}
}

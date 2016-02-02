import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {

	ArrayList<String> m_Urls;

	private String m_HtmlOrVideoIdentifier = "<a ";
	private String m_ImageIdentifier = "<img ";

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

		return m_Urls;
	}

	private void findAllRelevantData(String line, String i_ObjectToLookFor) {
		Pattern pattern = Pattern.compile(i_ObjectToLookFor + "(.*)=('|\")(.*)('|\")>");
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			filterAddress(matcher.group(3), i_ObjectToLookFor);
		}
	}

	private void filterAddress(String i_PageAddress, String i_LinkIdentifier) {
		if (i_PageAddress.startsWith("/")) {
			System.out.println("Parser: " + i_PageAddress);
			m_Urls.add(i_PageAddress);
		}
		else {
			HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
		}
	}
}

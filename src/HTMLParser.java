import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {

	ArrayList<String> m_Urls;

	private String m_HtmlOrVideoIdentifier = "<a href";
	private String m_ImageIdentifier = "<img src";
	private int m_NumberOfLinksFound = 0;
	private List<String> m_HtmlExtensions = Arrays.asList("html", "asp", "htm", "aspx", "php");

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
		Pattern pattern = Pattern.compile(identifiers[0] + "\\s[^>]*?\\s*(" + identifiers[1] + ")?\\s*=\\s*['\"]([^'\"]*?)['\"][^>]*?");

		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			filterAddress(matcher.group(2), i_ObjectToLookFor);
		}
	}

	private void filterAddress(String i_PageAddress, String i_LinkIdentifier) {
		if (i_PageAddress.startsWith("//")) {
			i_PageAddress = i_PageAddress.replaceFirst("//", "");
		}

		if (i_PageAddress.startsWith("/")) {
			m_Urls.add(i_PageAddress);
			m_NumberOfLinksFound++;
		} else {
			Pattern pattern = Pattern.compile("^(([^:\\/?#]+):)?(\\/\\/([^\\/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
			Matcher matcher = pattern.matcher(i_PageAddress);
			if (matcher.matches()) {
				try {
					if (matcher.group(4).equals(HtmlRepository.GetInstance().Host)) {
						m_Urls.add(i_PageAddress);
						m_NumberOfLinksFound++;
					}
					else {
						HtmlRepository.GetInstance().addExternalDomain(matcher.group(4));
						HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
					}
				} catch (Exception e) {
					if (i_PageAddress.startsWith(HtmlRepository.GetInstance().Host)) {
						m_Urls.add(i_PageAddress);
						m_NumberOfLinksFound++;
					}
					else {
						if (i_PageAddress.contains("/")) {
							if (!(i_PageAddress.substring(0, i_PageAddress.indexOf("/"))).contains(".") || 
									(i_PageAddress.substring(0, i_PageAddress.indexOf("/") + 1)).contains("../")) {
								m_Urls.add("/" + i_PageAddress);
								m_NumberOfLinksFound++;
							}
							else {
								HtmlRepository.GetInstance().addExternalDomain(matcher.group(4));
								HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
							}
						}
						else {
							try {
								String[] parts = i_PageAddress.split(Pattern.quote("."));
								String extension = parts[parts.length -1];
								if (m_HtmlExtensions.contains(extension) || HtmlRepository.GetInstance().isKnownExtension(extension)) {
									m_Urls.add("/" + i_PageAddress);
									m_NumberOfLinksFound++;
								} else {
									HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
								}
							} catch (Exception ie) {
								HtmlRepository.GetInstance().AddExternalLink(i_PageAddress);
							}
						}
					}
				}
			}
		}
	}
}
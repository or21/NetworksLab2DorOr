import java.util.ArrayList;

public class Parser extends Thread {

	@Override
	public void run() {
		Response responseToParse = HtmlRepository.GetInstance().GetResponse();
		HTMLParser parser = new HTMLParser();
		ArrayList<String> parsedUrls = parser.parse(responseToParse);
		for(String url : parsedUrls) {
			HtmlRepository.GetInstance().AddUrl(url);
		}
	}
}

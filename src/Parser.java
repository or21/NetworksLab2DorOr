import java.util.ArrayList;

public class Parser extends Thread {

	private Runnable m_OnAddedUrl;


	public Parser(Runnable i_OnAddedUrl) {
		m_OnAddedUrl = i_OnAddedUrl;
	}

	@Override
	public void run() {
		Response responseToParse;
		while ((responseToParse = HtmlRepository.GetInstance().GetResponse()) != null) {
			HTMLParser parser = new HTMLParser();
			ArrayList<String> parsedUrls = parser.parse(responseToParse);
			for(String url : parsedUrls) {
				HtmlRepository.GetInstance().AddUrl(url);
				m_OnAddedUrl.run();
			}
		}
	}
}

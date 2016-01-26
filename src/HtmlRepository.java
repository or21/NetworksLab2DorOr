import java.util.ArrayList;
import java.util.HashMap;

public class HtmlRepository {

	private ArrayList<String> m_PendingUrlsToDownload;	
	private ArrayList<Response> m_PendingResponsesToParse;

	private HashMap<String, Response> m_ExistingResponses;

	private final Object LOCK_OBJECT = new Object();
	private final Object PARSER_LOCK_OBJECT = new Object();

	public HtmlRepository() {
		m_PendingResponsesToParse = new ArrayList<>();
		m_PendingUrlsToDownload = new ArrayList<>();
	}

	public void AddContent(eLinkType i_Type, String i_Url, String i_Content, int i_ContentLength) {
		
	}

	public Response GetResponse() {
		Response responseToParse = null;
		synchronized (PARSER_LOCK_OBJECT) {
			if (m_PendingResponsesToParse.size() != 0) {
				responseToParse = m_PendingResponsesToParse.remove(0);
			}
		}
		
		return responseToParse;
	}

	public String GetUrl() {
		String urlToParse = null;
		synchronized (PARSER_LOCK_OBJECT) {
			if (m_PendingUrlsToDownload.size() != 0) {
				urlToParse = m_PendingUrlsToDownload.remove(0);
				// TODO : move to other place
				String htmlContent = readFromUrl(urlToParse);
				m_PendingResponsesToParse.add(new Response(urlToParse, htmlContent, "text/html"));
			}
		}
		
		return urlToParse;
	}

	public boolean ExistsUrlToParse(String i_Url) {
		synchronized(LOCK_OBJECT) {
			return m_ExistingResponses.containsKey(i_Url);
		}
	}

	public boolean ExistsUrlToDownload(String i_Url) {
		boolean exists = m_PendingUrlsToDownload.contains(i_Url);
		return exists;
	}

	private String readFromUrl(String i_urlAddress) {
		// TODO: send get request and read the Data from the response
	}
}

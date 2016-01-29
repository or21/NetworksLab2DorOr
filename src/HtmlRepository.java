import java.util.ArrayList;
import java.util.HashMap;

public class HtmlRepository {

	private ArrayList<String> m_PendingUrlsToDownload;	
	private ArrayList<Response> m_PendingResponsesToParse;
	private ArrayList<String> m_ExternalLinks;

	private HashMap<String, Response> m_ExistingResponses;

	private final static Object RESPONSES_LOCK_OBJECT = new Object();
	private final static Object URLS_LOCK_OBJECT = new Object();
	private final static Object EXTERNALS_LOCK_OBJECT = new Object();

	private static HtmlRepository m_Instance;

	private HtmlRepository() {
		m_PendingResponsesToParse = new ArrayList<>();
		m_PendingUrlsToDownload = new ArrayList<>();
		m_ExistingResponses = new HashMap<>();
		m_ExternalLinks = new ArrayList<>();
	}

	public String Host; // Public property

	public static HtmlRepository GetInstance() {
		if(m_Instance == null) {
			synchronized (RESPONSES_LOCK_OBJECT) {
				if(m_Instance == null) {
					m_Instance = new HtmlRepository();
				}
			}
		}
		return m_Instance;
	}

	public boolean IsReadyForDiagnostics() {
		return m_PendingResponsesToParse.size() == 0 && m_PendingUrlsToDownload.size() == 0;
	}

	public void AddUrl(String i_UrlToAdd) {
		synchronized (URLS_LOCK_OBJECT) {
			if ((!m_ExistingResponses.containsKey(i_UrlToAdd)) && (!m_PendingUrlsToDownload.contains(i_UrlToAdd))) {
				m_PendingUrlsToDownload.add(i_UrlToAdd);
				m_ExistingResponses.put(i_UrlToAdd, null);
			}
		}
	}

	public Response GetResponse() {
		Response responseToParse = null;
		synchronized (RESPONSES_LOCK_OBJECT) {
			if (m_PendingResponsesToParse.size() != 0) {
				responseToParse = m_PendingResponsesToParse.remove(0);
			}
		}

		return responseToParse;
	}

	public void AddResponse(Response i_ResponeToAdd) {
		synchronized (RESPONSES_LOCK_OBJECT) {
			m_PendingResponsesToParse.add(i_ResponeToAdd);
			m_ExistingResponses.put(i_ResponeToAdd.getUrl(), i_ResponeToAdd);
		}
	}

	public String GetUrl() {
		String urlToParse = null;
		synchronized (URLS_LOCK_OBJECT) {
			if (m_PendingUrlsToDownload.size() != 0) {
				urlToParse = m_PendingUrlsToDownload.remove(0);
			}
		}

		return urlToParse;
	}

	public void AddExternalLink(String i_LinkToAdd) {
		synchronized (EXTERNALS_LOCK_OBJECT) {
			if (!m_ExternalLinks.contains(i_LinkToAdd)) {
				m_ExternalLinks.add(i_LinkToAdd);
			}
		}
	}
}

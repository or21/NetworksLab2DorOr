import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class HtmlRepository {

	private ArrayList<String> m_PendingUrlsToDownload;	
	private ArrayList<Response> m_PendingResponsesToParse;
	private ArrayList<String> m_ExternalLinks;

	private HashSet<String> m_DisallowedUrls;
	private HashSet<String> m_AllowedUrls;

	private HashMap<String, Response> m_ExistingResponses;
	
	private ArrayList<String> m_ImagesTypes;
	private ArrayList<String> m_VideosTypes;
	private ArrayList<String> m_DocsTypes;

	private final static Object RESPONSES_LOCK_OBJECT = new Object();
	private final static Object URLS_LOCK_OBJECT = new Object();
	private final static Object EXTERNALS_LOCK_OBJECT = new Object();
	private final static Object AVERAGE_LOCK_OBJECT = new Object();
	
	private long m_SumOfRtt;
	private long m_NumOfHttpRequestsSent;

	private static HtmlRepository m_Instance;

	private HtmlRepository() {
		HashMap<String, String> configParams = ConfigFile.GetInstance().GetConfigurationParameters();
		
		m_PendingResponsesToParse = new ArrayList<>();
		m_PendingUrlsToDownload = new ArrayList<>();
		m_ExistingResponses = new HashMap<>();
		m_DisallowedUrls = new HashSet<>();
		m_AllowedUrls = new HashSet<>();
		m_ExternalLinks = new ArrayList<>();
		
		m_ImagesTypes = getTypesFromConfig(configParams.get("imageExtensions")); 
		m_VideosTypes = getTypesFromConfig(configParams.get("videoExtensions")); 
		m_DocsTypes = getTypesFromConfig(configParams.get("documentExtensions")); 
	}
	
	private ArrayList<String> getTypesFromConfig(String i_TypesAsString) {
		ArrayList<String> typesToReturn = new ArrayList<String>();
		String[] typesAsArray = i_TypesAsString.split(",");
		for (String type : typesAsArray) {
			typesToReturn.add(type.trim());
		}
		
		return typesToReturn;
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

	public HashSet<String> GetDisallowedUrls() {
		return m_DisallowedUrls;
	}
	
	public boolean IsReadyForDiagnostics() {
		synchronized(RESPONSES_LOCK_OBJECT) {
			synchronized(URLS_LOCK_OBJECT) {
				return m_PendingResponsesToParse.size() == 0 && m_PendingUrlsToDownload.size() == 0;
			}
		}
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
				System.out.println(m_PendingResponsesToParse.size() + " responses left");
			}
		}

		return responseToParse;
	}

	public void AddResponse(Response i_ResponeToAdd) {
		synchronized (RESPONSES_LOCK_OBJECT) {
			m_PendingResponsesToParse.add(i_ResponeToAdd);
			m_ExistingResponses.put(i_ResponeToAdd.GetUrl(), i_ResponeToAdd);
		}
	}

	public String GetUrl() {
		String urlToParse = null;
		synchronized (URLS_LOCK_OBJECT) {
			if (m_PendingUrlsToDownload.size() != 0) {
				urlToParse = m_PendingUrlsToDownload.remove(0);
				System.out.println(m_PendingUrlsToDownload.size() + " urls left");
			}
		}

		return urlToParse;
	}

	public void ParseRobotsContent(String i_RobotsContent) {
		String[] lines = i_RobotsContent.split("\r\n");
		for(String line : lines) {
			String[] ruleResultPair = line.split(": ");
			if (ruleResultPair[0].toLowerCase().equals("allow")) {
				m_AllowedUrls.add(ruleResultPair[1].trim());
			} else if (ruleResultPair[0].toLowerCase().equals("disallow")) {
				m_DisallowedUrls.add(ruleResultPair[1].trim());
			}
		}
	}

	public void AddExternalLink(String i_LinkToAdd) {
		synchronized (EXTERNALS_LOCK_OBJECT) {
			if (!m_ExternalLinks.contains(i_LinkToAdd)) {
				m_ExternalLinks.add(i_LinkToAdd);
			}
		}
	}

	public String CreateStatistics(boolean i_IgnoreRobotsEnabled, boolean i_TCPPortScanEnabled, ArrayList<Integer> i_OpenPorts) {
		int numberOfImages = 0;
		int totalImagesSize = 0;
		int numberOfVideos = 0;
		int totalVideosSize = 0;
		int numberOfDocs = 0;
		int totalDocsSize = 0;
		int numberOfPages = 0;
		int totalPagesSize = 0;
		int numOfInternalLinks = m_ExistingResponses.size();
		int numOfExternalLinks = m_ExternalLinks.size();
		for (String existingKey : m_ExistingResponses.keySet()) {
			Response response = m_ExistingResponses.get(existingKey);
			if (response == null) {
				continue;
			}

			String extension = response.GetExtension();
			int contentLength = response.GetContentLength();
			if (m_ImagesTypes.contains(extension)) {
				numberOfImages++;
				totalImagesSize += contentLength;
			}
			else if (m_VideosTypes.contains(extension)) {
				numberOfVideos++;
				totalVideosSize += contentLength;
			}
			else if (m_DocsTypes.contains(extension)) {
				numberOfDocs++;
				totalDocsSize += contentLength;
			}
			else {
				numberOfPages++;
				totalPagesSize += contentLength;
			}
		}

		StringBuilder response = new StringBuilder();
		
		response.append("<html><head><title>Your results are here!</title></head><body>");
		response.append("Crawler respected robots.txt: ").append(!i_IgnoreRobotsEnabled).append("<br>");
		response.append("Number of images is: ").append(numberOfImages).append("<br>");
		response.append("Total size (in bytes) of images is: ").append(totalImagesSize).append("<br>");
		response.append("Number of videos is: ").append(numberOfVideos).append("<br>");
		response.append("Total size (in bytes) of videos is: ").append(totalVideosSize).append("<br>");
		response.append("Number of documents is: ").append(numberOfDocs).append("<br>");
		response.append("Total size (in bytes) of documents is: ").append(totalDocsSize).append("<br>");
		response.append("Number of pages (all detected files excluding images, videos and documents): ").append(numberOfPages).append("<br>");
		response.append("Total size (in bytes) of pages is: ").append(totalPagesSize).append("<br>");
		response.append("Number of internal links is: ").append(numOfInternalLinks).append("<br>");
		response.append("Number of external links is: ").append(numOfExternalLinks).append("<br>");
		
		if (i_TCPPortScanEnabled) {
			response.append("The opened ports are: ");

			for (Integer port : i_OpenPorts) {
				response.append(port + ",");
			}
			response.append("<br>");
		}
		
		response.append("Average RTT in milliseconds is: ").append(HtmlRepository.GetInstance().AverageRtt()).append("<br>");
		
		response.append("<br>" + "Main page: ").append("<a href=\"/" + "\">" + "Home" +"</a>" + "<br>");
		response.append("</body></html>");

		return response.toString();
	}
	
	public void Dispose() {
		m_Instance = null;
	}
	
	public void UpdateAverageRtt(long i_Rtt) {
		synchronized (AVERAGE_LOCK_OBJECT) {
			m_NumOfHttpRequestsSent++;
			m_SumOfRtt += i_Rtt;
		}
	}
	
	public long AverageRtt() {
		return m_SumOfRtt / m_NumOfHttpRequestsSent;
	}
}

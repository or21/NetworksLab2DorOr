import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class HtmlRepository {

	private static HtmlRepository m_Instance;

	private final static Object RESPONSES_LOCK_OBJECT = new Object();
	private final static Object URLS_LOCK_OBJECT = new Object();
	private final static Object EXTERNALS_LOCK_OBJECT = new Object();
	private final static Object AVERAGE_LOCK_OBJECT = new Object();

	private ArrayList<Response> m_PendingResponsesToParse;
	private ArrayList<String> m_PendingUrlsToDownload;	
	private ArrayList<String> m_ExternalLinks;
	private ArrayList<String> m_PreviousDomains;
	private ArrayList<String> m_ImagesTypes;
	private ArrayList<String> m_VideosTypes;
	private ArrayList<String> m_DocsTypes;

	private HashSet<String> m_ExternalsDomains;
	private HashSet<String> m_DisallowedUrls;
	private HashSet<String> m_AllowedUrls;

	private HashMap<String, Response> m_ExistingResponses;

	private long m_SumOfRtt;
	private long m_NumOfHttpRequestsSent;

	private HtmlRepository() {
		HashMap<String, String> configParams = ConfigFile.GetInstance().GetConfigurationParameters();

		m_PendingResponsesToParse = new ArrayList<>();
		m_PendingUrlsToDownload = new ArrayList<>();
		m_ExistingResponses = new HashMap<>();
		m_DisallowedUrls = new HashSet<>();
		m_AllowedUrls = new HashSet<>();
		m_ExternalLinks = new ArrayList<>();
		m_ExternalsDomains = new HashSet<String>();

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
		if (m_Instance == null) {
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
		response.append("Number of domains the crawled domain is connected to: ").append(m_ExternalsDomains.size()).append("<br>");

		addPreviousDomains();
		for (String domain : m_ExternalsDomains) {
			if (m_PreviousDomains.contains(domain)) {
				response.append(("<a href=\"http://" + domain + "\">" + domain +"</a>" + "<br>"));
			}
			else {
				response.append(domain + "<br>");
			}
		}

		if (i_TCPPortScanEnabled) {
			response.append("The opened ports are: ").append(Arrays.toString(i_OpenPorts.toArray()).replace("[", "").replace("]", "")).append("<br>");
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

	public boolean isKnownExtension(String i_Extension) {
		return m_DocsTypes.contains(i_Extension) || m_ImagesTypes.contains(i_Extension) || m_VideosTypes.contains(i_Extension);
	}

	public long AverageRtt() {
		return m_SumOfRtt / m_NumOfHttpRequestsSent;
	}

	public boolean IsHtml(String i_Extension) {
		return !m_ImagesTypes.contains(i_Extension) && !m_VideosTypes.contains(i_Extension) && !m_VideosTypes.contains(i_Extension);
	}

	public void addExternalDomain(String i_Domain) {
		m_ExternalsDomains.add(i_Domain);
	}

	public boolean IsImage(String i_Extension) {
		return m_ImagesTypes.contains(i_Extension);
	}

	private void addPreviousDomains() {
		m_PreviousDomains = new ArrayList<String>();
		try {
			String fileName = Crawler.HISTORY_DOMAINS;
			try{
				FileReader inputFile = new FileReader(fileName);
				BufferedReader bufferReader = new BufferedReader(inputFile);
				String line = bufferReader.readLine();
				while ((line = bufferReader.readLine()) != null)   {
					if (!m_PreviousDomains.contains(line)) {
						m_PreviousDomains.add(line);
					}
				}

				bufferReader.close();
			} catch(Exception e){
				System.out.println("Error while reading file line by line:" + e.getMessage());                      
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Response GetResponseForHome() {
		return m_ExistingResponses.get("/");
	}
}

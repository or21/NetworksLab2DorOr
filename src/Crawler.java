import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.mail.MessagingException;

public class Crawler {

	private final String TEXT_BOX_HOST_KEY = "textBoxURL";
	private final String CHECK_BOX_IGNORE_ROBOTS_KEY = "checkBoxIgnoreRobots";
	private final String CHECK_BOX_TCP_PORT_SCAN_KEY = "checkBoxTCPPortScan";
	private final String TEXT_BOX_EMAIL_ADDRESS_KEY = "textBoxEmail";
	private final String CHECK_BOX_SHOULD_USE_CHUNKED = "checkBoxShouldUseChunked";
	public static String MAIN_PAGE_NAME = RequestFactory.m_ConfigFileRootPath + "static/html/index.html";
	public static String ALREADY_RUNNING = RequestFactory.m_ConfigFileRootPath + "static/html/crawler_is_already_running.html";
	public static String HISTORY_DOMAINS = RequestFactory.m_ConfigFileRootPath + "static/html/history_domains.txt";

	private final static Object m_ParsersLock = new Object();
	private final static Object m_DownloadersLock = new Object();

	private HtmlRepository m_HtmlRepository;
	private Parser[] m_Parsers;
	private Downloader[] m_Downloaders;
	private boolean m_TCPPortScanEnabled = false;
	private boolean m_IgnoreRobotsEnabled = false;
	private static boolean m_IsReadingChunkedEnabled = false;
	private String m_EmailAddress;
	private boolean m_ShouldSendEmail;
	private ArrayList<Integer> m_OpenPorts;

	public static boolean isCrawlerRunning;

	// Callback events used by the downloader and parser threads respectively.
	private final Runnable onAddedResponse = new Runnable() {
		@Override
		public void run() {
			synchronized (m_ParsersLock) {
				UpdateNewParser();
			}
		}
	};

	private final Runnable onAddedUrl = new Runnable() {

		@Override
		public void run() {
			synchronized(m_DownloadersLock) {
				UpdateNewDownloader();
			}
		}
	};

	public Crawler(HashMap<String, String> i_Params) throws IllegalArgumentException, IllegalStateException {
		isCrawlerRunning = true;
		HashMap<String, String> configParams = ConfigFile.GetInstance().GetConfigurationParameters();
		m_HtmlRepository = HtmlRepository.GetInstance();
		m_HtmlRepository.Host = i_Params.get(TEXT_BOX_HOST_KEY);

		if (m_HtmlRepository.Host == null || m_HtmlRepository.Host.length() == 0) {
			throw new IllegalArgumentException("Hostname not specified");
		}

		try {
			InetAddress.getByName(m_HtmlRepository.Host).isReachable(1000);
		} catch (Exception e) {
			throw new IllegalStateException("Hostname unreachable");
		}

		m_TCPPortScanEnabled = i_Params.containsKey(CHECK_BOX_TCP_PORT_SCAN_KEY);
		m_IgnoreRobotsEnabled = i_Params.containsKey(CHECK_BOX_IGNORE_ROBOTS_KEY);
		m_IsReadingChunkedEnabled  = i_Params.containsKey(CHECK_BOX_SHOULD_USE_CHUNKED);
		m_EmailAddress = i_Params.get(TEXT_BOX_EMAIL_ADDRESS_KEY);
		m_EmailAddress = m_EmailAddress.replace("%40", "@");
		m_ShouldSendEmail = !m_EmailAddress.equals("");
		m_Parsers = new Parser[Integer.parseInt(configParams.get("maxAnalyzers"))];
		m_Downloaders = new Downloader[Integer.parseInt(configParams.get("maxDownloaders"))];
		m_OpenPorts = new ArrayList<Integer>();

		if (m_TCPPortScanEnabled) {
			m_OpenPorts = new ArrayList<>();
		}
	}

	/**
	 * Main flow of the crawler (paraphrased from the README file):
	 * Once the user submits the form, the crawler starts up on its own thread (if the crawler was already running, or for some reason failed, 
	 * an appropriate message will be returned to the user). First, it sets up the file to return its data to the user, and then sends a /robots.txt request, 
	 * to get its relevant data, and finally starts the producer-consumer cycle, by adding the "/" request to the url queue, and waits for all its
	 * child threads to finish.
	 * Once this process is over, the crawler turns to the HtmlRepository to aggregate all of the statistics. 
	 * It is important to note here that if a response was null, no parsing was done on it, and therefore it is not added to the statistical report.
	 * Once the html is ready, it is saved for the user evaluation, and its link is presented in the main page. If the user has chosen to receive the email of the report,
	 * an email will be sent to the given email address, or the crawler will print out that the email was unreachable, or that there was an internal error (for example
	 * the libraries and jars were not compiled with the .java files...)
	 */
	public void Run() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String filename = RequestFactory.m_ConfigFileRootPath + "static/html/crawler_results/" + m_HtmlRepository.Host + "_" + dateFormat.format(new Date()) + ".html";
		m_HtmlRepository.AddUrl("/robots.txt");
		Downloader robotsRequest = new Downloader(null, m_IgnoreRobotsEnabled);
		robotsRequest.start();

		try {
			robotsRequest.join();
		} catch (InterruptedException e1) {
			System.out.println("Got interrupt during robots scanning");
		}

		m_HtmlRepository.AddUrl("/");
		if (m_IgnoreRobotsEnabled) {
			for (String disallowedUrl : m_HtmlRepository.GetDisallowedUrls()) {
				m_HtmlRepository.AddUrl(disallowedUrl);
			}
		}
		
		Downloader request = new Downloader(onAddedResponse, m_IgnoreRobotsEnabled);
		request.start();

		try {
			request.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		while(areThreadsStillRunning()) {
			for(Parser parser : m_Parsers) {
				try {
					if (parser != null) {
						parser.join();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			for(Downloader downloader : m_Downloaders) {
				try {
					if (downloader != null) {
						downloader.join();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();	
				}
			}
		}

		if (m_TCPPortScanEnabled) {
			performPortScan();
		}

		System.out.println("\nWe finished parsing through everything! :)\n");

		String statistics = HtmlRepository.GetInstance().CreateStatistics(m_IgnoreRobotsEnabled, m_TCPPortScanEnabled, m_OpenPorts);
		File result = new File(filename);
		result.setWritable(true);
		try {
			result.createNewFile();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
			writer.write(statistics);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		addResultsLinkToAllHtmlFiles(filename);
		HtmlRepository.GetInstance().Dispose();
		if (m_ShouldSendEmail) {
			try {
				EmailService.SendEmail("Crawler", m_EmailAddress, "CrawlerResults", filename, m_HtmlRepository.Host);
				System.out.println("Check your email for the results");
			} catch (MessagingException me) {
				System.out.println("WebCrawler: Email address is not valid, email was not sent");
			} catch (Exception e) {
				System.out.println("WebCrawler: Internal email server error, please try again");
			}
		}
		
		isCrawlerRunning = false;
	}

	private void addResultsLinkToAllHtmlFiles(String i_Filename) {
		addResultsLinkToFile(i_Filename.substring(i_Filename.indexOf(m_HtmlRepository.Host)), MAIN_PAGE_NAME);
		addResultsLinkToFile(i_Filename.substring(i_Filename.indexOf(m_HtmlRepository.Host)), PostRequest.CRAWLER_STARTED_SUCCESSFULLY);
		addResultsLinkToFile(i_Filename.substring(i_Filename.indexOf(m_HtmlRepository.Host)), PostRequest.CRAWLER_FAILED_BAD_HOSTNAME);
		addResultsLinkToFile(i_Filename.substring(i_Filename.indexOf(m_HtmlRepository.Host)), PostRequest.CRAWLER_FAILED_EMPTY_HOSTNAME);
		addResultsLinkToFile(i_Filename.substring(i_Filename.indexOf(m_HtmlRepository.Host)), ALREADY_RUNNING);
		addLinkToHistoryFile(HtmlRepository.GetInstance().Host);
	}

	/**
	 * Method used to write and add the link to the results to the index.html and all other appropriate files
	 * 
	 * @param i_FileToAdd
	 * @param i_FileName
	 */
	private void addResultsLinkToFile(String i_FileToAdd, String i_FileName) {
		File file = new File(i_FileName); 
		File temp;
		try {
			temp = File.createTempFile("temp-file-name", ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw =  new PrintWriter(new FileWriter(temp));
			String line;
			while ((line = br.readLine()) != null) {
				pw.println(line);
				if (line.contains("Older crawled files")) {
					pw.println("<a href=\"" + "/crawler_results/" + i_FileToAdd + "\">" + i_FileToAdd.replace("_", "-").replace(".html", "") +"</a>" + "<br>");
				}
			}
			br.close();
			pw.close();
			file.delete();
			temp.renameTo(file);
		} catch (IOException e) {
			System.out.println("Failed to change file: " + i_FileName);
		}
	}

	/**
	 * Writes the current domain to the history file
	 * 
	 * @param i_Domain
	 */
	private void addLinkToHistoryFile(String i_Domain) {
		File file = new File(HISTORY_DOMAINS); 
		File temp;
		try {
			temp = File.createTempFile("temp-file-name", ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw =  new PrintWriter(new FileWriter(temp));
			String line;
			while ((line = br.readLine()) != null) {
				pw.println(line);
				if (line.contains("History")) {
					pw.println(i_Domain);
				}
			}
			
			br.close();
			pw.close();
			file.delete();
			temp.renameTo(file);
		} catch (IOException e) {
			System.out.println("Failed to update history file");
		}
	}

	private void performPortScan() {
		for (int port = 1; port <= 1024; port++) {
			try {
				System.out.println("Scanning port: " + port);
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(HtmlRepository.GetInstance().Host, port), 100);
				socket.close();
				m_OpenPorts.add(port);
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Checks in both arrays that no threads are running
	 * 
	 * @return
	 */
	private boolean areThreadsStillRunning() {
		for(Parser parser : m_Parsers) {
			if (parser != null && parser.isAlive()) {
				return true;
			}
		}

		for(Downloader downloader : m_Downloaders) {
			if (downloader != null && downloader.isAlive()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks that the parsers are running, and if they are not, then creates a new parser to parse the newly added response
	 * Used in the onAddedResponse callback method
	 */
	public void UpdateNewParser() {
		for(int i = 0; i < m_Parsers.length; i++) {
			if (m_Parsers[i] == null) {
				m_Parsers[i] = new Parser(onAddedUrl);
				m_Parsers[i].start();
				break;
			} else if (!m_Parsers[i].isAlive()) {
				m_Parsers[i] = new Parser(onAddedUrl);
				m_Parsers[i].start();
				break;
			} else {
				// All good with this one, the others will get it
			}
		}
	}

	/**
	 * Checks that the Downloaders are running, and if they are not, then creates a new Downloader to get the newly added url
	 * Used in the onAddedResponse callback method
	 */
	public void UpdateNewDownloader() {
		for(int i = 0; i < m_Downloaders.length; i++) {
			if (m_Downloaders[i] == null ) {
				m_Downloaders[i] = new Downloader(onAddedResponse, m_IgnoreRobotsEnabled);
				m_Downloaders[i].start();
				break;
			} else if (!m_Downloaders[i].isAlive()) {
				m_Downloaders[i] = new Downloader(onAddedResponse, m_IgnoreRobotsEnabled);
				m_Downloaders[i].start();
				break;
			} else {
				// All good with this one, the others will get it
			}
		}
	}

	public static boolean IsChunkedEnabled() {
		return m_IsReadingChunkedEnabled;
	}
}

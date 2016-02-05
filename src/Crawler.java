import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Crawler {

	private final String TEXT_BOX_HOST_KEY = "textBoxURL";
	private final String CHECK_BOX_IGNORE_ROBOTS_KEY = "checkBoxIgnoreRobots";
	private final String CHECK_BOX_TCP_PORT_SCAN_KEY = "checkBoxTCPPortScan";
	private final String MAIN_PAGE_NAME = "static/html/index.html";
	private final String CHECK_BOX_SHOULD_USE_CHUNKED = "checkBoxShouldUseChunked";
	public static String ALREADY_RUNNING = "static/html/crawler_is_already_running.html";
	public static String HISTORY_DOMAINS = "static/html/history_domains.txt";

	private HtmlRepository m_HtmlRepository;
	private Parser[] m_Parsers;
	private Downloader[] m_Downloaders;
	private boolean m_TCPPortScanEnabled = false;
	private boolean m_IgnoreRobotsEnabled = false;
	private static boolean m_IsReadingChunkedEnabled = false;
	private ArrayList<Integer> m_OpenPorts;
	
	public static boolean isCrawlerRunning;

	private final Runnable onAddedResponse = new Runnable() {
		@Override
		public void run() {
			UpdateNewParser();
		}
	};

	private final Runnable onAddedUrl = new Runnable() {

		@Override
		public void run() {
			UpdateNewDownloader();
		}
	};

	public Crawler(HashMap<String, String> i_Params) throws IllegalArgumentException {
		isCrawlerRunning = true;
		HashMap<String, String> configParams = ConfigFile.GetInstance().GetConfigurationParameters();
		m_HtmlRepository = HtmlRepository.GetInstance();
		m_HtmlRepository.Host = i_Params.get(TEXT_BOX_HOST_KEY);

		if (m_HtmlRepository.Host == null || m_HtmlRepository.Host.length() == 0) {
			throw new IllegalArgumentException("Hostname not specified");
		}

		m_TCPPortScanEnabled = i_Params.containsKey(CHECK_BOX_TCP_PORT_SCAN_KEY);
		m_IgnoreRobotsEnabled = i_Params.containsKey(CHECK_BOX_IGNORE_ROBOTS_KEY);
		m_IsReadingChunkedEnabled  = i_Params.containsKey(CHECK_BOX_SHOULD_USE_CHUNKED);
		m_Parsers = new Parser[Integer.parseInt(configParams.get("maxAnalyzers"))];
		m_Downloaders = new Downloader[Integer.parseInt(configParams.get("maxDownloaders"))];
		m_OpenPorts = new ArrayList<Integer>();

		if (m_TCPPortScanEnabled) {
			m_OpenPorts = new ArrayList<>();
		}
	}

	public String Run() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String filename = "static/html/crawler_results/" + m_HtmlRepository.Host + "_" + dateFormat.format(new Date()) + ".html";
		m_HtmlRepository.AddUrl("/robots.txt");
		Downloader robotsRequest = new Downloader(null, m_IgnoreRobotsEnabled);
		robotsRequest.start();

		try {
			robotsRequest.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
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
					// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		addFileLinkToIndexHtml(filename.substring(28));
		addFileLinkToHistoryHtml(HtmlRepository.GetInstance().Host);
		HtmlRepository.GetInstance().Dispose();
		isCrawlerRunning = false;
		return filename;
	}

	private void addFileLinkToIndexHtml(String i_FileToAdd) {
		File file = new File(MAIN_PAGE_NAME); 
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
			e.printStackTrace();
		}

	}
	
	private void addFileLinkToHistoryHtml(String i_FileToAdd) {
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
					pw.println(i_FileToAdd);
				}
			}
			br.close();
			pw.close();
			file.delete();
			temp.renameTo(file);
		} catch (IOException e) {
			e.printStackTrace();
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
				// All good with this one, they will get it
			}
		}
	}

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
				// All good with this one, they will get it
			}
		}
	}
	
	public static boolean IsChunkedEnabled() {
		return m_IsReadingChunkedEnabled;
	}
}

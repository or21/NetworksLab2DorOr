import java.util.ArrayList;
import java.util.HashMap;

public class Crawler {

	private final String TEXT_BOX_HOST_KEY = "textBoxURL";
	private final String CHECK_BOX_IGNORE_ROBOTS_KEY = "checkBoxIgnoreRobots";
	private final String CHECK_BOX_TCP_PORT_SCAN_KEY = "checkBoxTCPPortScan";
	// TODO: Add this later:
	private final String CHECK_BOX_SHOULD_USE_CHUNKED = "checkBoxShouldUseChunked";

	private HtmlRepository m_HtmlRepository;
	private Parser[] m_Parsers;
	private Downloader[] m_Downloaders;
	private boolean m_TCPPortScanEnabled = false;
	private boolean m_IgnoreRobotsEnabled = false;
	private ArrayList<Integer> m_OpenPorts;

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
		m_HtmlRepository = HtmlRepository.GetInstance();
		m_HtmlRepository.Host = i_Params.get(TEXT_BOX_HOST_KEY);
		if (m_HtmlRepository.Host == null || m_HtmlRepository.Host.length() == 0) {
			throw new IllegalArgumentException("Hostname not specified");
		}
		m_TCPPortScanEnabled = i_Params.containsKey(CHECK_BOX_TCP_PORT_SCAN_KEY);
		m_IgnoreRobotsEnabled = i_Params.containsKey(CHECK_BOX_IGNORE_ROBOTS_KEY);
		m_Parsers = new Parser[2]; // TODO: Use configfile
		m_Downloaders = new Downloader[10]; // TODO: Use configfile
		m_OpenPorts = new ArrayList<Integer>();
	}

	public String Run() {
		String filename = "Statistics results " + System.currentTimeMillis();
		m_HtmlRepository.AddUrl("/robots.txt");
		Downloader robotsRequest = new Downloader(null);
		robotsRequest.start();
		try {
			robotsRequest.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		m_HtmlRepository.AddUrl("/");
		Downloader request = new Downloader(onAddedResponse);
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
		System.out.println("We finished parsing through everything! :)");
		System.out.println(HtmlRepository.GetInstance().CreateStatistics(m_IgnoreRobotsEnabled, m_TCPPortScanEnabled, m_OpenPorts));
		return filename;
		// TODO: Return filename
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
				m_Downloaders[i] = new Downloader(onAddedResponse);
				m_Downloaders[i].start();
				break;
			} else if (!m_Downloaders[i].isAlive()) {
				m_Downloaders[i] = new Downloader(onAddedResponse);
				m_Downloaders[i].start();
				break;
			} else {
				// All good with this one, they will get it
			}
		}
	}
}

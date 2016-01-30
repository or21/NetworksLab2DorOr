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
	
	private String m_Host;
	private boolean m_TCPPortScanEnabled = false;
	private boolean m_IgnoreRobotsEnabled = false;
	
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
	}
	
	public String Run() {
		String filename = "Statistics results " + System.currentTimeMillis();
		m_HtmlRepository.AddUrl("/");
		Downloader request = new Downloader();
		request.start();
		
		while(!m_HtmlRepository.IsReadyForDiagnostics()) {
			UpdateNewParser();
			UpdateNewDownloader();
			for(Parser parser : m_Parsers) {
				try {
					parser.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for(Downloader downloader : m_Downloaders) {
				try {
					downloader.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return filename;
		// TODO: Return filename
	}
	
	public void UpdateNewParser() {
		for(Parser parser : m_Parsers) {
			if (parser == null ) {
				parser = new Parser();
				parser.start();
				break;
			} else if (!parser.isAlive()) {
				parser = new Parser();
				parser.start();
			} else {
				// All good with this one, they will get it
			}
		}
	}
	
	public void UpdateNewDownloader() {
		for(Downloader downloader : m_Downloaders) {
			if (downloader == null ) {
				downloader = new Downloader();
				downloader.start();
				break;
			} else if (!downloader.isAlive()) {
				downloader = new Downloader();
				downloader.start();
			} else {
				// All good with this one, they will get it
			}
		}
	}

	private boolean parsersAreBusy() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean downloadersAreBusy() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
	//		HtmlRepository.GetInstance().AddUrl("/");
	//		HtmlRepository.GetInstance().Host = "www.ynet.co.il";
	//		
	//		
	//		
	//		try {
	//			request.join();
	//		} catch (InterruptedException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		
	//		Parser parser = new Parser();
	//		parser.start();
	//		try {
	//			parser.join();
	//		} catch (InterruptedException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		System.out.println("");
	//		
	//		// Finish
	//	}
	//

}

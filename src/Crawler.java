
public class Crawler {
	
	private HtmlRepository m_HtmlRepository;
	private Parser[] m_Parsers;
	private Downloader[] m_Downloaders;
	
	public Crawler(String i_Host) {
		m_HtmlRepository = HtmlRepository.GetInstance();
		m_HtmlRepository.Host = i_Host;
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
					// TODO Auto-generated catch block
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

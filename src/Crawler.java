
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
		m_HtmlRepository.AddUrl("/");
		Downloader request = new Downloader();
		request.start();
		while(!m_HtmlRepository.IsReadyForDiagnostics()) {
			
		}
		
		
		return null;
		// TODO: Return filename
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

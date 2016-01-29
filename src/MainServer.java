public class MainServer {

	public static void main(String[] args) {
//		try {
//			ConfigFile config = ConfigFile.GetInstance();
//			config.Parse(ConfigFile.CONFIG_FILE_PATH);
//			WebServer server = new WebServer(config);
//			server.Run();
//		} 
//		catch(Exception e) {
//			System.out.println("Usage: Config file not found");
//		}
		
		Crawler crawler = new Crawler("www.google.com");
		crawler.Run();
	}

		// Send URL and get content and submit response

		//		
		//		buildResponsePage();
}
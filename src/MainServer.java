import java.io.IOException;
import java.net.InetAddress;

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

		//		HTMLParser parser = new HTMLParser();
		//		parser.parse();
		
		// Send URL and get content and submit response
		
		HtmlRepository htmlRepo = new HtmlRepository();
//		while (true) {
//			String url = htmlRepo.GetUrl();
//			String response = SendHTTPGetRequest(url);
//			Response responseObj = buildResponseObj(response);
//			htmlRepo.AddResponse(responseObj);
//		}
//		
//		// Get response and parse it
//		while (true) {
//			Response response = htmlRepo.GetResponse();
//			String[] urlsFromRepo = parseResponse(response);
//			for (String url : urlsFromRepo) {
//				htmlRepo.AddUrl(url);
//			}
//		}
//		
//		buildResponsePage();
		HttpGetRequest request = new HttpGetRequest("www.google.com", "/", htmlRepo);
		try {
			request.sendRequest();
		} catch (IOException i_ioe) {
			System.out.println(i_ioe);
		}
		
		// Finish
	}
}

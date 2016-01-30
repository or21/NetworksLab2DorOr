import java.io.IOException;

public class Downloader extends Thread {	
	
	public Downloader() {
	}

	@Override
	public void run() {
		String url = HtmlRepository.GetInstance().GetUrl();
		HttpGetRequest request = new HttpGetRequest(HtmlRepository.GetInstance().Host, url);
		String fullResponse = null;
		try {
			 fullResponse = request.sendRequestReceiveResponse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(fullResponse);
		if (url.equals("/robots.txt")) {
			String[] robotsResponse = fullResponse.split("\r\n\r\n");
			HtmlRepository.GetInstance().ParseRobotsContent(robotsResponse[robotsResponse.length - 1]);
		} else {
			HtmlRepository.GetInstance().AddResponse(Response.GenerateResponse(url, fullResponse));
		}
	}
}
import java.io.IOException;

public class Downloader extends Thread {

	
	private Runnable m_OnAddedResponseEvent;

	public Downloader(Runnable i_OnAddedResponseEvent) {
		m_OnAddedResponseEvent = i_OnAddedResponseEvent;
	}

	@Override
	public void run() {
		String url;
		while ((url = HtmlRepository.GetInstance().GetUrl()) != null) {
			HttpGetRequest request = new HttpGetRequest(HtmlRepository.GetInstance().Host, url);
			String fullResponse = null;
			try {
				fullResponse = request.sendRequestReceiveResponse();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (fullResponse == null) {
				// TODO: Remove from hashmap once basic flow works
				continue;
			}

			if (url.equals("/robots.txt")) {
				String[] robotsResponse = fullResponse.split("\r\n\r\n");
				HtmlRepository.GetInstance().ParseRobotsContent(robotsResponse[robotsResponse.length - 1]);
			} else {
				HtmlRepository.GetInstance().AddResponse(Response.GenerateResponse(url, fullResponse));
				m_OnAddedResponseEvent.run();
			}
		}
	}
}
import java.io.IOException;

public class Downloader extends Thread {

	private Runnable m_OnAddedResponseEvent;
	private boolean m_IsIgnoringRobots;

	public Downloader(Runnable i_OnAddedResponseEvent, boolean i_IsIgnoringRobotsTxt) {
		m_OnAddedResponseEvent = i_OnAddedResponseEvent;
		m_IsIgnoringRobots = i_IsIgnoringRobotsTxt;
	}

	@Override
	public void run() {
		String url;
		while ((url = HtmlRepository.GetInstance().GetUrl()) != null) {
			if (!m_IsIgnoringRobots) {
				if (HtmlRepository.GetInstance().GetDisallowedUrls().contains(url)) {
					System.out.println("Downloader: url is disallowed: " + url);
					continue;
				}
			}
			System.out.println("Downloader starts downloading URL " + url);
			HttpCrawlerRequest request = new HttpCrawlerRequest(HtmlRepository.GetInstance().Host, url);
			String fullResponse = null;
			try {
				fullResponse = request.sendRequestReceiveResponse();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (fullResponse == null) {
				// TODO: Remove from hashmap once basic flow works
				continue;
			}

			System.out.println("Downloader ends downloading the URL " + url);
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
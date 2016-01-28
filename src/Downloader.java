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
		HtmlRepository.GetInstance().AddResponse(Response.GenerateResponse(url, fullResponse));
	}
}
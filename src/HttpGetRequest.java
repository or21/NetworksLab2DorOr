import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGetRequest {
	
	private String m_UrlAddress;
	private final String USER_AGENT = "Mozilla/5.0";
	private HtmlRepository m_Repository;

	
	public HttpGetRequest(String i_Url, HtmlRepository i_Repository) {
		m_UrlAddress = i_Url;
		m_Repository = i_Repository;
	}
	
	public String sendRequest() throws IOException {
		URL url = new URL(m_UrlAddress);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		//add request header
		connection.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = connection.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			response.append("\n");
		}
		in.close();

		//print result
		System.out.println(response.toString());
		
		return response.toString();
	}
}

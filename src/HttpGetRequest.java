import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpGetRequest {
	
	private String m_Host;
	private String m_RequestPage;
	private final String USER_AGENT = "Mozilla/5.0";
	private HtmlRepository m_Repository;

	
	public HttpGetRequest(String i_Host, String i_RequestPage, HtmlRepository i_Repository) {
		m_Host = i_Host;
		m_Repository = i_Repository;
		m_RequestPage = i_RequestPage;
	}
	
	public String sendRequest() throws IOException {
		Socket socket = new Socket(m_Host, 80); 

		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))); 
		out.println("GET " + m_RequestPage + " HTTP/1.1");
		out.println("Host:" + m_Host);
		out.println(); 
		out.flush();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		StringBuilder responseAsString = new StringBuilder();
		String line;
		while (!(line = reader.readLine()).equals("")) {
			responseAsString.append(line + "\r\n");
		}
		
		if(responseAsString.toString().contains("200 OK")) {
			while((line = reader.readLine()) != null) {
				responseAsString.append(line + "\r\n");
			}
		} else {
			// Redirect
			if (responseAsString.toString().contains("301")) {
				String[] response = responseAsString.toString().split("\r\n");
				for(String header : response) {
					if (header.contains("Location: ")) {
						new HttpGetRequest(m_Host, header.split(" ")[1], m_Repository).sendRequest();
					}
				}
			} else {
				String[] response = responseAsString.toString().split("\r\n");
				for(String header : response) {
					if (header.contains("Location: ")) {
						String newHost = header.split(" ")[1];
						// http://www.google.co.il/page...
						Pattern pattern = Pattern.compile("http[s]?://([a-zA-Z0-9.]*)/(.*)");
						Matcher matcher = pattern.matcher(newHost);
						if(matcher.find()) {
							new HttpGetRequest(matcher.group(0), "/" + matcher.group(1), m_Repository).sendRequest();
						}
					}
				}
			}
		}
		
		reader.close();

		//print result
		System.out.println(responseAsString.toString());
		
		return responseAsString.toString();
	}
}

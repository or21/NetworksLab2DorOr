import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpCrawlerRequest {

	private String m_Host;
	private String m_RequestPage;
	private int m_NumberOfRedirects = 0;
	private int MAX_NUMBER_OF_REDIRECTS = 3;
	private boolean isHtml;

	private long m_StartingHttpRequestTime;

	public HttpCrawlerRequest(String i_Host, String i_RequestPage) {
		m_Host = i_Host;
		m_RequestPage = i_RequestPage;
		m_NumberOfRedirects = 0;
	}

	public HttpCrawlerRequest(String i_Host, String i_RequestPage, int i_NumberOfRedirects) {
		this(i_Host, i_RequestPage);
		m_NumberOfRedirects = i_NumberOfRedirects;
	}

	/**
	 * Uses a given socket to send a request to the given url, and return a response from it. Depending
	 * on whether the url is an html, sends either a GET or HEAD.
	 * 
	 * @param i_Socket
	 * @throws IOException
	 */
	private void sendRequest(Socket i_Socket) throws IOException {
		m_StartingHttpRequestTime = System.currentTimeMillis();
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(i_Socket.getOutputStream()))); 
		if (m_RequestPage.startsWith("//")) {
			m_RequestPage = m_RequestPage.substring(1);
		}
		if (m_RequestPage.contains(m_Host)) {
			m_RequestPage = m_RequestPage.substring(m_RequestPage.lastIndexOf(m_Host) + m_Host.length());
		}

		String extension = Response.FindExtension(m_RequestPage);
		isHtml = HtmlRepository.GetInstance().IsHtml(extension);

		out.println((isHtml ? "GET " : "HEAD ") + m_RequestPage + (Crawler.IsChunkedEnabled() ? " HTTP/1.1" : " HTTP/1.0"));
		out.println("Host:" + m_Host);
		out.println(); 
		out.flush();
	}

	/**
	 * Sends a request, and receives a response. Depending on the response headers, deals with it accordingly.
	 * If the response was a 200 OK, returns it as is.
	 * If the response header was a 301 or 302 redirects, recursively sends other requests, unless it has been redirected
	 * three times.
	 * If the response was neither of those, returns null.
	 * 
	 * @return the response as a full string, or a null
	 * @throws IOException
	 */
	public String sendRequestReceiveResponse() throws IOException {
		Socket socket = new Socket(m_Host, 80); 

		sendRequest(socket);

		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		StringBuilder responseAsString = new StringBuilder();
		String line;
		boolean measureEndTime = true;
		while (!(line = reader.readLine()).equals("")) {
			if (measureEndTime) {
				HtmlRepository.GetInstance().UpdateAverageRtt(System.currentTimeMillis() - m_StartingHttpRequestTime);
				measureEndTime = false;
			}

			responseAsString.append(line + "\r\n");
		}

		String[] response = responseAsString.toString().split("\r\n");
		HashMap<String, String> headers = CreateResponseHeaders(response);
		String responseCode = headers.get("response_code");
		if(responseCode.contains("200 OK")) {
			boolean isChunked = headers.containsKey("transfer-encoding") && 
					headers.get("transfer-encoding").equals("chunked");
			responseAsString.append("\r\n");
			
			if (!isHtml)
			{
				return responseAsString.toString();
			}
			
			if (isChunked) {
				readResponseAsChunked(reader, line, responseAsString);
			} else {
				while((line = reader.readLine()) != null) {
					responseAsString.append(line).append("\r\n");
				}
			}
			
			reader.close();
			
			return responseAsString.toString();
		} else  {
			reader.close();
			
			if (m_NumberOfRedirects == MAX_NUMBER_OF_REDIRECTS ){
				return null; // 
			}
			
			if (headers.get("response_code").contains("301")) {	
				if (headers.get("location").contains(m_Host)) {
					return new HttpCrawlerRequest(m_Host, headers.get("location").substring(headers.get("location").lastIndexOf(m_Host + "/") + m_Host.length()), m_NumberOfRedirects + 1).sendRequestReceiveResponse();
				} else {
					return new HttpCrawlerRequest(m_Host, headers.get("location"), m_NumberOfRedirects + 1).sendRequestReceiveResponse();
				}
			} else if (headers.get("response_code").contains("302")) {
				String newHost = headers.get("location");
				Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
				Matcher matcher = pattern.matcher(newHost);
				if(matcher.find()) {
					return new HttpCrawlerRequest(matcher.group(2), "/" + matcher.group(4), m_NumberOfRedirects + 1).sendRequestReceiveResponse();
				}
			} 
		}
		
		return null;
	}

	public static HashMap<String, String> CreateResponseHeaders(String[] response) {
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("response_code", response[0].substring(response[0].indexOf(" ")));
		for(int i = 1; i < response.length; i++) {
			String[] keyValuePair = response[i].split(": ");
			if (keyValuePair.length > 1) {
				headers.put(
						keyValuePair[0].toLowerCase().trim(),
						keyValuePair[1].toLowerCase().trim()
						);
			}
		}
		
		return headers;
	}

	/**
	 * If for some reason, the user has chosen to send an http/1.1 request, that means that data will be received as CHUNKED, meaning
	 * that the {@link Downloader} should read as chunked.
	 * 
	 * This section is a BONUS.
	 * 
	 * @param i_Reader
	 * @param line
	 * @param responseAsString
	 * @throws IOException
	 */
	private void readResponseAsChunked(BufferedReader i_Reader, String line, StringBuilder responseAsString) throws IOException {
		while((line = i_Reader.readLine()) != null) {
			int amountToRead = Integer.parseInt(line, 16);
			if (amountToRead == 0) {
				break;
			}
			
			char[] buffer = new char[amountToRead + 1];
			for(int i = 0; i <= amountToRead; i++) {
				buffer[i] = (char) i_Reader.read();
			}
			
			i_Reader.readLine();
			responseAsString.append(buffer);
			responseAsString.append("\r\n");
		}		
	}
}

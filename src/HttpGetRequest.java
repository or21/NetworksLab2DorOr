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

public class HttpGetRequest {

	private String m_Host;
	private String m_RequestPage;
	private String m_PrevRequestPage;

	public HttpGetRequest(String i_Host, String i_RequestPage) {
		m_Host = i_Host;
		m_RequestPage = i_RequestPage;
		m_PrevRequestPage = "";
	}
	
	public HttpGetRequest(String i_Host, String i_RequestPage, String i_PrevRequestPage) {
		this(i_Host, i_RequestPage);
		m_PrevRequestPage = i_PrevRequestPage;
	}
	
	private void sendRequest(Socket i_Socket) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(i_Socket.getOutputStream()))); 
		if (m_RequestPage.startsWith("//")) {
			m_RequestPage = m_RequestPage.substring(1);
		}
		if (m_RequestPage.contains(m_Host)) {
			m_RequestPage = m_RequestPage.substring(m_RequestPage.lastIndexOf(m_Host) + m_Host.length());
		}

		out.println("GET " + m_RequestPage + " HTTP/1.0");
		out.println("Host:" + m_Host);
		out.println(); 
		out.flush();
	}

	public String sendRequestReceiveResponse() throws IOException {
		Socket socket = new Socket(m_Host, 80); 

		sendRequest(socket);

		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		StringBuilder responseAsString = new StringBuilder();
		String line;
		while (!(line = reader.readLine()).equals("")) {
			responseAsString.append(line + "\r\n");
		}

		String[] response = responseAsString.toString().split("\r\n");
		HashMap<String, String> headers = createResponseHeaders(response);
		String responseCode = headers.get("response_code");
		if(responseCode.contains("200 OK")) {
			boolean isChunked = headers.containsKey("transfer-encoding") && 
					headers.get("transfer-encoding").equals("chunked");
			responseAsString.append("\r\n");
			if(isChunked) {
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
			if (headers.containsKey("location") && headers.get("location").equals(m_PrevRequestPage)) {
				return null; // 
			}
			if (headers.get("response_code").contains("301")) {	
				if (headers.get("location").contains(m_Host)) {
					return new HttpGetRequest(m_Host, headers.get("location").substring(headers.get("location").lastIndexOf(m_Host + "/") + m_Host.length()), headers.get("location")).sendRequestReceiveResponse();
				} else {
					return new HttpGetRequest(m_Host, headers.get("location"), headers.get("location")).sendRequestReceiveResponse();
				}
			} else if (headers.get("response_code").contains("302")) {
				String newHost = headers.get("location");
				Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
				Matcher matcher = pattern.matcher(newHost);
				if(matcher.find()) {
					return new HttpGetRequest(matcher.group(2), "/" + matcher.group(4), headers.get("location")).sendRequestReceiveResponse();
				}
			} 
		}
		return null;
	}

	public static HashMap<String, String> createResponseHeaders(String[] response) {
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

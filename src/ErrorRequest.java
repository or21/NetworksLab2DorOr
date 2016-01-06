import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class ErrorRequest implements IClientRequest {

	private final String m_Type = "text/html";
	private String m_FilePath;
	private String m_Header;
	private Socket m_Socket;
	
	/*
	 * Constructor
	 */
	public ErrorRequest(String i_FilePath, String i_Header, Socket i_Socket) {
		m_FilePath = i_FilePath;
		m_Header = i_Header;
		m_Socket = i_Socket;
	}
	
	/*
	 * Build the response for the received error
	 */
	@Override
	public void ReturnResponse() throws IOException {
		OutputStream outputStream = m_Socket.getOutputStream();
		StringBuilder responseString = new StringBuilder();
		responseString.append(m_Header);
		
		byte[] content = Tools.ReadFile(new File(m_FilePath));
		HashMap<String, String> defaultHeaders = Tools.SetupResponseHeaders(content, m_Type);
		for(String header : defaultHeaders.keySet()) {
			responseString.append(header).append(": ").append(defaultHeaders.get(header)).append("\r\n");
		}
		
		System.out.println(responseString);
		responseString.append("\r\n");
		
		try {
			outputStream.write(responseString.toString().getBytes());
			outputStream.write(content);
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			System.out.println("No socket to write the respone to.");
		}
	}

}

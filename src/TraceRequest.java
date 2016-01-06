import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class TraceRequest extends HeadRequest {
	
	private String m_Request;

	/*
	 * Constructor
	 */
	public TraceRequest(String[] i_FirstHeaderRow, HashMap<String,String> requestHeaders, String i_Request, Socket i_Socket) {
		super (i_FirstHeaderRow, requestHeaders, i_Socket);
		m_Request = i_Request;
	}

	/*
	 * Return response for Trace request
	 */
	@Override
	public void ReturnResponse() throws IOException {
		OutputStream outputStream = m_Socket.getOutputStream();
		File fileToReturn;
		fileToReturn = openFileAccordingToUrl(m_Url);
		if (!fileToReturn.exists()) {
			ReturnNotFoundResponse();
		} else {
			m_Content = m_Request.getBytes();
			m_Headers = Tools.SetupResponseHeaders(m_Content, m_Type);
			m_Headers.put("Content-Length", String.valueOf(Integer.valueOf(m_Headers.get("Content-Length")) + 4));
			StringBuilder responseString = new StringBuilder(createHeaders());
			responseString.append("\r\n").append(m_Request.length() + 4)
			.append("\r\n").append(m_Request);
			System.out.println(responseString);

			try {
				outputStream.write(responseString.toString().getBytes());
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				System.out.println("No socket to write the respone to.");
			}
		}
	}
}

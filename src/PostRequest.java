import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class PostRequest extends GetRequest {

	public final static String CRAWLER_STARTED_SUCCESSFULLY = "static/html/crawler_started_successfully.html";
	public final static String CRAWLER_FAILED_BAD_HOSTNAME = "static/html/crawler_failed_bad_hostname.html";
	public final static String CRAWLER_FAILED_EMPTY_HOSTNAME = "static/html/crawler_failed_empty_hostname.html";

	/*
	 * Constructor
	 */
	public PostRequest(String[] i_FirstHeaderRow, HashMap<String, String> i_RequestHeaders, Socket i_Socket) {
		super(i_FirstHeaderRow, i_RequestHeaders, i_Socket);

		if (i_RequestHeaders.containsKey("Content-Length")) {
			int contentLength = Integer.valueOf(i_RequestHeaders.get("Content-Length"));
			if (contentLength > 0) {
				if (m_Params == null) {
					m_Params = new HashMap<String, String>();
				}
				parseParams(i_RequestHeaders.get("params"));
			}
			System.out.println();
		}
	}

	/*
	 * Return response for POST request
	 * Regular requests will go to GET response. 
	 * If it's post from our HTML - return the parameters from it.
	 */
	@Override
	public void ReturnResponse() throws IOException {
		if (!(m_Url.equals("/params_info.html") || m_Url.equals("/execResults.html"))) {
			super.ReturnResponse();
		}
		else {
			OutputStream outputStream = m_Socket.getOutputStream();
			
			if (m_Url.equals("/params_info.html")){
				StringBuilder content = new StringBuilder();
				content.append("<html>\n<head><title>Params Info</title><link rel=\"shortcut icon\" href=\"/favicon.jpg\" type=\"image/jpg\"/></head>\n<body>");
				content.append("<b>Params</b>: <br>");
				
				for (String key : m_Params.keySet()) {
					content.append(key + ": " + m_Params.get(key) + "<br>");
				}

				content.append("</body>\n</html>");
				m_Content = content.toString().getBytes();
			} else {
				if (Crawler.isCrawlerRunning) {
					m_Content = Tools.ReadFile(new File(Crawler.ALREADY_RUNNING));
				} else {
					System.out.println(m_Params);
					try {
						final Crawler webcrawler = new Crawler(m_Params);
						m_Content = Tools.ReadFile(new File(CRAWLER_STARTED_SUCCESSFULLY));
						new Thread() {
							@Override
							public void run() {
								webcrawler.Run();
							};
						}.start();
					} catch (IllegalArgumentException iae) {
						m_Content = Tools.ReadFile(new File(CRAWLER_FAILED_EMPTY_HOSTNAME));
						Crawler.isCrawlerRunning = false;
					} catch (IllegalStateException ise) {
						m_Content = Tools.ReadFile(new File(CRAWLER_FAILED_BAD_HOSTNAME));
						Crawler.isCrawlerRunning = false;
					} 
				}
			}
			
			m_Headers = m_ShouldSendChunked ? Tools.SetupChunkedResponseHeaders(m_Type) : Tools.SetupResponseHeaders(m_Content, m_Type);
			StringBuilder responseString = new StringBuilder(createHeaders());
			System.out.println(responseString);
			responseString.append(CRLF);
			try {
				outputStream.write(responseString.toString().getBytes());
				if (m_ShouldSendChunked) {
					DataOutputStream output = new DataOutputStream(outputStream);
					byte[] buffer = new byte[m_ChunkSize];
					ByteArrayInputStream inputStream = new ByteArrayInputStream(m_Content);
					int amountOfDataRead;
					while ((amountOfDataRead = inputStream.read(buffer, 0, m_ChunkSize)) != -1) {
						sendChunk(output, buffer, amountOfDataRead);
					}
					System.out.println((Integer.toHexString(0) + CRLF));
					output.write((Integer.toHexString(0) + CRLF).getBytes());
					output.write(m_CRLFInByteArray);
				} else {
					outputStream.write(m_Content);
					outputStream.flush();
				}
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				System.out.println("No socket to write the respone to.");
			}
		}
	}
}

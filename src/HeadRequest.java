import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class HeadRequest implements IClientRequest {

	private final String HTML = "html";
	private final String JPG = "jpg";
	private final String GIF = "gif";
	private final String PNG = "png";
	private final String BMP = "bmp";
	private final String PATH_HTML = "html/";
	private final String PATH_IMAGE = "images/";
	private final String PATH_ICON = "icon/";
	private final String PATH_DEFAULT = "default/";
	private final String ICON = "ico";
	private final String TYPE_HTML = "text/html";
	private final String TYPE_ICON = "icon";
	private final String TYPE_IMAGE = "image/";
	private final String TYPE_OCTET = "application/octet";
	private final String m_404NotFoundPath = RequestFactory.m_ConfigFileRootPath + "static/html/404notfound.html";
	private final String m_404NotFoundHeader = "HTTP/1.1 404 Not Found\r\n";
	private final String[] m_ErrorFilePath = new String[] {"/400BadRequest.html", "/404notfound.html", "/403ForbiddenRequest.html", "/500InternalError.html", "/501NotImplemented.html" };

	protected final String HTTP_200_OK = "HTTP/1.1 200 OK\r\n";
	protected final String CRLF = "\r\n";
	protected final String m_StaticFilesPath = "static/";
	protected boolean m_ShouldSendChunked = false;
	protected String m_Type;
	protected String m_Url;
	protected HashMap<String, String> m_Headers;
	protected byte[] m_Content;
	protected String m_DefaultFromClient = "/";
	protected String m_Extension;
	protected Socket m_Socket;

	/*
	 * Constructor.
	 */
	public HeadRequest(String[] i_FirstHeaderRow, HashMap<String, String> requestHeaders, Socket i_Socket) {
		this.m_Socket = i_Socket;
		

		m_Url = i_FirstHeaderRow[1].replace("..", "");
		m_Url = m_Url.replaceAll("[/]+", "/");
		
		if (checkForbiddenURL()) {
			throw new IllegalAccessError();
		}
		
		m_ShouldSendChunked = requestHeaders.containsKey("chunked") && requestHeaders.get("chunked").equals("yes");

		if (m_Url.equals(m_DefaultFromClient)) {
			m_Type = TYPE_HTML;
		} else { 
			int i = m_Url.lastIndexOf('.');
			if (i > 0) {
				int substringTo = m_Url.contains("?") ? m_Url.indexOf("?") : m_Url.length();
				m_Extension = m_Url.substring(i + 1, substringTo);
				m_Type = determineType(m_Extension); 
			} else {
				m_Type = TYPE_HTML;
			}
		}
	}

	private boolean checkForbiddenURL() {
		for (String path : m_ErrorFilePath) {
			if (m_Url.equals(path)) {
				return true;
			}
		}
		
		return false;
	}

	/*
	 * Select the type of the request file according to the request
	 */
	private String determineType(String i_Extension) {
		if(i_Extension.equals(HTML)) {
			return TYPE_HTML;
		} else if (i_Extension.equals(ICON)) {
			return TYPE_ICON;
		} else if (i_Extension.equals(JPG)) {
			return TYPE_IMAGE + JPG;
		} else if (i_Extension.equals(BMP)) {
			return TYPE_IMAGE + BMP;
		} else if (i_Extension.equals(GIF)) {
			return TYPE_IMAGE + GIF;
		} else if (i_Extension.equals(PNG)) {
			return TYPE_IMAGE + PNG;
		} else {
			return TYPE_OCTET;
		}
	}

	/*
	 * Build the headers for the response
	 */
	protected String createHeaders() {
		StringBuilder responseString = new StringBuilder(HTTP_200_OK);

		for(String header : m_Headers.keySet()) {
			responseString.append(header).append(": ").append(m_Headers.get(header)).append(CRLF);
		}

		return responseString.toString();
	}

	/*
	 * Open the file from the URL request
	 */
	protected File openFileAccordingToUrl(String i_Url) {
		return (m_Url.equals(m_DefaultFromClient) ? 
				new File(RequestFactory.m_ConfigFileRootPath + m_StaticFilesPath + PATH_HTML + RequestFactory.m_ConfigFileDefaultPage) : 
					new File(RequestFactory.m_ConfigFileRootPath + m_StaticFilesPath + determineFileLocation() + m_Url));
	}

	/*
	 * Determine in which folder to look for the file
	 */
	private String determineFileLocation() {
		if (m_Type.equals(TYPE_HTML)) {
			return PATH_HTML;
		} else if (m_Type.equals(TYPE_ICON)) {
			return PATH_ICON;
		} else if (m_Type.startsWith(TYPE_IMAGE)) {
			return PATH_IMAGE;
		} else {
			return PATH_DEFAULT;
		}
	}

	/*
	 * Return the response for HEAD request
	 */
	@Override
	public void ReturnResponse() throws IOException {
		File fileToReturn;
		fileToReturn = openFileAccordingToUrl(m_Url);

		if (!fileToReturn.exists()) {
			ReturnNotFoundResponse();
		} else {
			OutputStream outputStream = m_Socket.getOutputStream();
			m_Content = Tools.ReadFile(fileToReturn);	
			m_Headers = Tools.SetupResponseHeaders(m_Content, m_Type);
			String headersToReturn = createHeaders();
			headersToReturn += "\r\n";
			System.out.println(headersToReturn);

			try {
				outputStream.write(headersToReturn.getBytes());
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				System.out.println("No socket to write the respone to.");
			}
		}
	}

	protected void ReturnNotFoundResponse() throws IOException {
		new ErrorRequest(m_404NotFoundPath, m_404NotFoundHeader, m_Socket).ReturnResponse();
	}
}
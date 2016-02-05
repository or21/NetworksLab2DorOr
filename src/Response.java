import java.util.HashMap;

public class Response {

	private final String HTML_TYPE = "text/html";
	private final String IMAGE_TYPE = "image/";

	private String m_Content;
	private String m_Url;
	private String m_Extension;
	private int m_ContentLength;
	private eLinkType m_ContentType;

	public Response(String i_Url, String i_Content, String i_ContentType) {
		m_Url = i_Url;
		m_Content = i_Content;
		m_ContentLength = i_Content.length();
		m_ContentType = determineContentType(i_ContentType);
		m_Extension = FindExtension(m_Url);
	}

	public static String FindExtension(String i_Url) {
		int i = i_Url.lastIndexOf('.');
		return i_Url.substring(i + 1, i_Url.length());
	}

	public Response(String i_Url, String i_Content, int i_ContentLength, String i_ContentType) {
		m_Url = i_Url;
		m_Content = i_Content;
		m_ContentLength = i_ContentLength;
		m_ContentType = determineContentType(i_ContentType);
		m_Extension = FindExtension(m_Url);
	}

	private eLinkType determineContentType(String i_ContentType) {
		return i_ContentType.contains(HTML_TYPE) ? eLinkType.HTML :
			i_ContentType.contains(IMAGE_TYPE) ? eLinkType.IMAGE :
				eLinkType.VIDEO;
	}

	public String GetExtension() {
		return m_Extension;
	}

	public String GetContent() {
		return m_Content;
	}

	public int GetContentLength() {
		return m_ContentLength;
	}

	public eLinkType GetType() {
		return m_ContentType;
	}

	public String GetUrl() {
		return m_Url;
	}

	public static Response GenerateResponse(String i_Url, String i_FullResponse) {
		if (i_FullResponse == null) {
			return null;
		}
		
		String[] headersAndContent = i_FullResponse.split("\r\n\r\n");
		HashMap<String, String> headers = HttpCrawlerRequest.createResponseHeaders(headersAndContent[0].split("\r\n"));
		int contentLength = headers.containsKey("content-length") ? Integer.parseInt(headers.get("content-length")) : headersAndContent[1].length();
		String contentType = headers.get("content-type");

		StringBuilder responseContent = new StringBuilder();
		for (int i = 1; i < headersAndContent.length; i++) {
			responseContent.append(headersAndContent[i]).append("\n");
		}

		return new Response(i_Url, responseContent.toString(), contentLength, contentType);
	}
}

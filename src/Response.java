
public class Response {

	private final String HTML_TYPE = "text/html";
	private final String IMAGE_TYPE = "image/";
	
	private String m_Content, m_Url;
	private int m_ContentLength;
	private eLinkType m_ContentType;
	
	public Response(String i_Url, String i_Content, String i_ContentType) {
		m_Url = i_Url;
		m_Content = i_Content;
		m_ContentLength = i_Content.length();
		m_ContentType = determineContentType(i_ContentType);
	}
	
	public Response(String i_Url, String i_Content, int i_ContentLength, String i_ContentType) {
		m_Url = i_Url;
		m_Content = i_Content;
		m_ContentLength = i_ContentLength;
		m_ContentType = determineContentType(i_ContentType);
	}

	private eLinkType determineContentType(String i_ContentType) {
		return i_ContentType.equals(HTML_TYPE) ? eLinkType.HTML :
			i_ContentType.startsWith(IMAGE_TYPE) ? eLinkType.IMAGE :
				eLinkType.VIDEO;
	}
	
	public String getContent() {
		return m_Content;
	}
	
	public int getContentLength() {
		return m_ContentLength;
	}
	
	public eLinkType getType() {
		return m_ContentType;
	}
	
	public String getUrl() {
		return m_Url;
	}
}

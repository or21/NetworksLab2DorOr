import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Create the relevant object for a request
 */
public class RequestFactory {

	public final static String m_ConfigFileRootPath = ConfigFile.GetInstance().GetConfigurationParameters().get(ConfigFile.CONFIG_FILE_ROOT_KEY);
	public final static String m_ConfigFileDefaultPage = ConfigFile.GetInstance().GetConfigurationParameters().get(ConfigFile.CONFIG_FILE_DEFAULT_PAGE_KEY);
	public final static String m_NotImplementedPath = m_ConfigFileRootPath + "static/html/501NotImplemented.html";
	public final static String m_NotImplementedHeader = "HTTP/1.1 501 Not Implemented\r\n";
	public final static String m_BadRequestPath = m_ConfigFileRootPath + "static/html/400BadRequest.html";
	public final static String m_BadRequestHeader = "HTTP/1.1 400 Bad Request\r\n";
	public final static String m_ForbiddenRequestPath = m_ConfigFileRootPath + "static/html/403ForbiddenRequest.html";
	public final static String m_ForbiddenRequestHeader = "HTTP/1.1 403 Forbidden\r\n";
	public final static String m_InternalErrorPath = m_ConfigFileRootPath + "static/html/500InternalError.html";
	public final static String m_InternalErrorHeader = "HTTP/1.1 500 Internal Server Error\r\n";

	/*
	 * Create object according to request
	 */
	public static IClientRequest CreateRequest(String i_Request, Socket i_Socket) {
		String[] requestSplitByBreak = i_Request.split("\r\n\r\n");
		String[] allHeaders = requestSplitByBreak[0].split("\r\n");
		HashMap<String, String> requestHeaders = Tools.SetupRequestHeaders(Arrays.copyOfRange(allHeaders, 1, allHeaders.length));

		if (requestSplitByBreak.length == 2 && requestSplitByBreak[1] != null) {
			requestHeaders.put("params", requestSplitByBreak[1]);
		}

		String[] firstHeader = allHeaders[0].split("[ ]+");
		if ((firstHeader.length != 3) || (!eSupportedHTTP.isInEnum(firstHeader[2]))){ 
			return new ErrorRequest(m_BadRequestPath, m_BadRequestHeader, i_Socket);
		} else if (!checkValidPath(firstHeader[1])) {
			return new ErrorRequest(m_ForbiddenRequestPath, m_ForbiddenRequestHeader, i_Socket);
		} else {
			try {
				eMethods caseSwitch = eMethods.valueOf(firstHeader[0]);
				switch(caseSwitch) {
				case GET: 
					return new GetRequest(firstHeader, requestHeaders, i_Socket);		

				case POST: 
					return new PostRequest(firstHeader, requestHeaders, i_Socket);		

				case HEAD: 
					return new HeadRequest(firstHeader, requestHeaders, i_Socket);		

				case TRACE: 
					return new TraceRequest(firstHeader, requestHeaders, i_Request, i_Socket);

				default:
					return new ErrorRequest(m_NotImplementedPath, m_NotImplementedHeader, i_Socket);
				}
			}
			catch (IllegalAccessError iae) {
				return new ErrorRequest(m_ForbiddenRequestPath, m_ForbiddenRequestHeader, i_Socket);
			}
			catch (IllegalArgumentException iae) {
				return new ErrorRequest(m_NotImplementedPath, m_NotImplementedHeader, i_Socket);
			}
			catch (NullPointerException npe) {
				return new ErrorRequest(m_NotImplementedPath, m_NotImplementedHeader, i_Socket);
			}
			catch (Exception e) {
				return new ErrorRequest(m_InternalErrorPath, m_InternalErrorHeader, i_Socket);
			}
		}
	}

	/*
	 * Check if a path is valid
	 */
	private static boolean checkValidPath(String i_Url) {
		boolean isValid = true;

		if (!i_Url.startsWith("/")) {
			isValid = false;
		}

		return isValid;
	}

	/*
	 * Enum with all supported requests
	 */
	public static enum eMethods {
		GET,
		POST,
		HEAD,
		TRACE
	}

	/*
	 * Enum with the supported HTTP versions
	 */
	public enum eSupportedHTTP {
		ONE("HTTP/1.0"),
		ONEPOINTONE("HTTP/1.1");

		private final String m_Value;

		eSupportedHTTP (String value) { 
			this.m_Value = value; 
		}

		public String getValue() { 
			return m_Value; 
		}

		public static boolean isInEnum(String str) {
			boolean isEnumValue = false;
			for (eSupportedHTTP enumVar : eSupportedHTTP.values()) {
				if (str.equals(enumVar.getValue())) {
					isEnumValue = true;
				}
			}

			return isEnumValue;
		}
	}
}

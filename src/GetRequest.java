import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public class GetRequest extends HeadRequest {

	protected int m_ChunkSize = 132;
	protected HashMap<String, String> m_Params;
	byte[] m_CRLFInByteArray = CRLF.getBytes();

	public GetRequest(String[] i_FirstHeaderRow, HashMap<String, String> requestHeaders, Socket i_Socket) {
		super(i_FirstHeaderRow, requestHeaders, i_Socket);

		if (m_Url.contains("?")) {
			m_Params = new HashMap<String, String>();
			parseParams(m_Url.substring(m_Url.indexOf("?") + 1));
		}
	}

	/*
	 * Parse the parameters from the request
	 */
	protected void parseParams(String i_ParamsString) {
		String[] parametersArray = i_ParamsString.split("&");

		for (String string : parametersArray) {
			String[] keyValuePair = string.split("=");
			if (keyValuePair.length == 1) {
				m_Params.put(keyValuePair[0], "");
			} else {
				m_Params.put(keyValuePair[0], keyValuePair[1]);
			}
		}
	}

	/*
	 * Build the response for GET request
	 */
	@Override
	public void ReturnResponse() throws IOException {
		OutputStream outputStream = m_Socket.getOutputStream();
		File fileToReturn;
		fileToReturn = openFileAccordingToUrl(m_Url);
		if (!fileToReturn.exists()) {
			ReturnNotFoundResponse();
		} 
		// Create chunk response
		else if (m_ShouldSendChunked) {
			m_Headers = Tools.SetupChunkedResponseHeaders(m_Type);
			StringBuilder responseString = new StringBuilder(createHeaders());
			responseString.append(CRLF);
			writeChunked(new DataOutputStream(outputStream), responseString.toString().getBytes(), fileToReturn);
		} else {
			// Create and send regular response
			m_Content = Tools.ReadFile(fileToReturn);
			m_Headers = Tools.SetupResponseHeaders(m_Content, m_Type);
			StringBuilder responseString = new StringBuilder(createHeaders());

			System.out.println(responseString);
			responseString.append(CRLF);

			try {
				outputStream.write(responseString.toString().getBytes());
				outputStream.write(m_Content);
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				System.out.println("No socket to write the respone to.");
			}
		}
	}

	/*
	 * Write the response in chunks
	 */
	private void writeChunked(DataOutputStream i_OutputStream, byte[] i_HeadersData, File i_FileToReturn) throws NumberFormatException, IOException {
		int amountOfDataRead;
		byte[] dataToSend = new byte[m_ChunkSize];
		
		System.out.println(new String(i_HeadersData));
		i_OutputStream.write(i_HeadersData);
		FileInputStream fis = new FileInputStream(i_FileToReturn);
		
		// Read and build each chunk according to chunkSize
		while ((amountOfDataRead = fis.read(dataToSend, 0, m_ChunkSize)) != -1) {
				sendChunk(i_OutputStream, dataToSend, amountOfDataRead);
				dataToSend = new byte[m_ChunkSize];
		}
		
		// Finish the file - send 0 to let the client know.
		// System.out.println((Integer.toHexString(0) + CRLF));
		i_OutputStream.write((Integer.toHexString(0) + CRLF).getBytes());
		i_OutputStream.write(m_CRLFInByteArray);
		i_OutputStream.flush();
		i_OutputStream.close();
		fis.close();
	}

	/*
	 * Send the data in chunk format
	 */
	protected void sendChunk(DataOutputStream i_OutputStream, byte[] i_Data, int i_AmountOfDataToWrite) throws IOException {
		String chunkSize = Integer.toHexString(i_AmountOfDataToWrite);
		//System.out.println(chunkSize + CRLF + new String(i_Data));
		i_OutputStream.write(chunkSize.getBytes());
		i_OutputStream.write(m_CRLFInByteArray);
		i_OutputStream.write(i_Data, 0, i_AmountOfDataToWrite);
		i_OutputStream.write(m_CRLFInByteArray);
		i_OutputStream.flush();
	}
}
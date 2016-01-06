import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class Tools {

	/*
	 * Create headers for HTTP response
	 */
	public static HashMap<String, String> SetupResponseHeaders(byte[] i_Content, String i_Type) {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", i_Type);
		headers.put("Content-Length", String.valueOf(i_Content.length));
		headers.put("Date", getServerTime());
		return headers;
	}
	
	/*
	 * Create dictionary from the request according to headers
	 */
	public static HashMap<String, String> SetupRequestHeaders(String[] i_RequestHeaders) throws IllegalArgumentException {
		HashMap<String, String> responseDictionary = new HashMap<String, String>();
		for (String header :  i_RequestHeaders) {
			String[] splitted = header.split(": ");
			if (splitted.length != 2) {
				throw new IllegalArgumentException();
			}
			responseDictionary.put(splitted[0], splitted[1]);
		}
		return responseDictionary;
	}
	
	/*
	 * Read from file.
	 * Return the file in byte array.
	 */
	public static byte[] ReadFile(File i_File)
	{
		FileInputStream fis = null;
		try
		{
			byte[] fileInBytesArray = new byte[(int)i_File.length()];
			fis = new FileInputStream(i_File);
			while(fis.available() != 0)
			{
				fis.read(fileInBytesArray, 0, fileInBytesArray.length);
			}
			return fileInBytesArray;
		}
		catch(FileNotFoundException i_FNFE)
		{
			System.out.println("File not found");
			return null;
		}
		catch(IOException i_IOE)
		{
			System.out.println("IOException");
			return null;
		} 
		finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					System.out.println("No socket to write the respone to.");
				}
			}
		}
	}

	/*
	 * Create headers for chunk response
	 */
	public static HashMap<String, String> SetupChunkedResponseHeaders(String i_Type) {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", i_Type);
		headers.put("Transfer-Encoding", "chunked");
		headers.put("Date", getServerTime());
		return headers;
	}
	
	private static String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}
}

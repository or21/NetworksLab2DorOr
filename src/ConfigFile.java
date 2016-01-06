import java.io.File;
import java.util.HashMap;

/**
 * Singleton implementation of the config file
 * 
 * @author Dor Or
 *
 */
public class ConfigFile implements IParser {
	
	public static final String CONFIG_FILE_PATH = "config/config.ini";
	public static final String CONFIG_FILE_ROOT_KEY = "root";
	public static final String CONFIG_FILE_DEFAULT_PAGE_KEY = "defaultPage";
	
	private HashMap<String, String> m_ConfigDictionary;

	private static ConfigFile m_Instance = null;
	private static Object m_Lock = new Object();

	private ConfigFile() {}

	public static ConfigFile GetInstance() {
		if (m_Instance == null) {
			synchronized (m_Lock) {
				if (m_Instance == null) {
					m_Instance = new ConfigFile();					
				}
			}
		}
		return m_Instance;
	}
	
	/**
	 * Parse the config to dictionary
	 */
	@Override
	public void Parse(String i_Filename) {
		m_ConfigDictionary = new HashMap<>();
		byte[] contentAsByteArray = Tools.ReadFile(new File(i_Filename));
		String[] content = new String(contentAsByteArray).split("\r\n");
		for(String line : content) {
			String[] keyValuePair = line.split("=");
			m_ConfigDictionary.put(keyValuePair[0], keyValuePair[1]);
		}
	}
	
	public HashMap<String, String> GetConfigurationParameters() {
		return m_ConfigDictionary;
	}
}

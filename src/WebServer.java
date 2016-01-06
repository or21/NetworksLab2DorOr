import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class WebServer {

	private int m_Port, m_MaxThreads;
	private ThreadPool m_ThreadPool;
	private ServerSocket m_ServerSocket;

	/**
	 * Constructor, receives a configuration files, and parses its keys
	 * @param i_ConfigFile
	 */
	public WebServer(ConfigFile i_ConfigFile) {
		HashMap<String, String> configParams = i_ConfigFile.GetConfigurationParameters();
		m_Port = Integer.parseInt(configParams.get("port"));
		m_MaxThreads = Integer.valueOf(configParams.get("maxThreads"));
		m_ThreadPool = new ThreadPool(m_MaxThreads);
		m_ServerSocket = createServerSocket();
	}

	/*
	 * Wait for connection. 
	 * For each connection - create HTTPRequestHandler.
	 * When the thread finished - manage the thread pool.
	 */
	public void Run() {
		System.out.println("Listening on port: " + m_Port);
		while (true)
		{
			Socket connection = waitForConnection();
			if (connection == null) { 
				continue;
			} else {
				System.out.println("Recieved a new HTTP request");
				HTTPRequestHandler request = new HTTPRequestHandler(connection, new Runnable() {

					@Override
					public void run() {
						onFinishThread();
					}
				});

				m_ThreadPool.AddThread(request);
			}
		}
	}

	private Socket waitForConnection() {
		try {
			return m_ServerSocket.accept();
		} catch (IOException e) {
			System.out.println("No socket to write the respone to.");
		}
		return null;
	}

	private ServerSocket createServerSocket() {
		try {
			return new ServerSocket(m_Port);
		}
		catch (BindException be) {
			System.out.println("Usage: Another process is using the port given by the config file. Shut it down so that this process will be able to use this port");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("No socket to write the respone to.");
		} 
		return null;
	}

	private void onFinishThread() {
		System.out.println("Thread Finished\n");
		m_ThreadPool.Manage();
	}
}

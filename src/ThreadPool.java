import java.util.ArrayList;

public class ThreadPool {

	private ArrayList<Thread> m_RunningRequests;
	private ArrayList<Thread> m_WaitingRequests;
	private int m_MaxNumOfThreads;
	private Object m_ManageLock = new Object();

	/*
	 * Initialize the arrays
	 */
	public ThreadPool(int i_MaxNumOfThreads) {
		this.m_MaxNumOfThreads = i_MaxNumOfThreads;
		this.m_RunningRequests = new ArrayList<Thread>(m_MaxNumOfThreads);
		this.m_WaitingRequests = new ArrayList<Thread>();
	}

	/*
	 * For a new request - add it to the waiting list and call manage
	 */
	public void AddThread(Thread i_ThreadToAdd) {
		m_WaitingRequests.add(i_ThreadToAdd);
		Manage();
	}

	/*
	 * Check if some thread finished. 
	 * If so - remove it from the running threads. 
	 * If there is a space in running threads - add a new thread from the waiting list.
	 */
	public void Manage() {
		synchronized (m_ManageLock) {
			for (int i = 0; i < m_RunningRequests.size(); i++) {
				if ((m_RunningRequests.get(i) != null) && (((HTTPRequestHandler) m_RunningRequests.get(i)).getSocket().isClosed())) { 
					m_RunningRequests.remove(i);
					i--;
				}
			}

			if ((m_RunningRequests.size() < m_MaxNumOfThreads) && (m_WaitingRequests.size() > 0)) {
				Thread currentThread = m_WaitingRequests.get(0);
				m_WaitingRequests.remove(0);
				m_RunningRequests.add(currentThread);
				System.out.println("Starts new thread: " + m_RunningRequests.size() + "\n");
				currentThread.start();
			}
		}
	}
}

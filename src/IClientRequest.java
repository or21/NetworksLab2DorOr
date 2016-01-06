import java.io.IOException;

/*
 * Interface for all the requests in the server
 */
public interface IClientRequest {
	
	void ReturnResponse() throws IOException;
	
}

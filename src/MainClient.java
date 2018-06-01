import java.io.Console;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Client Main class.
 * @author grrdozier
 */
public class MainClient {
    /**
     * Runs the Client program.
     *
     * @param args none
     */
    public static void main(String[] args){
        try {
            Console c = System.console();
            String hostname = c.readLine("Hostname: ");
            int port = Integer.parseInt(c.readLine("Port: "));
            Client client = new Client(hostname, port);
            client.playGame();
        } catch (SocketTimeoutException e) {
            System.out.println("Connection timed out.");
        } catch (IOException e) {
            System.out.println("Connection closed with exception:");
            e.printStackTrace();
        }
    }
}

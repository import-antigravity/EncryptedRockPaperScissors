import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Server Main class.
 * @author grrdozier
 */
public class MainServer {
    /**
     * Runs server program.
     *
     * @param args Just the port number
     */
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            System.out.println("Hosting rock-paper-scissors game on port " + port);
            Server server = new Server(port);
            server.playGame();
        } catch (SocketTimeoutException e) {
            System.out.println("Connection timed out.");
        } catch (IOException e) {
            System.out.println("Connection closed with exception:");
            e.printStackTrace();
        }
    }
}

import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is the client side of the game. It connects to the server, then processes messages from the server, as
 * well as sending its own.
 *
 * @author grrdozier
 */
public class Client extends NetworkPlayer {
    private Socket socket;
    private DataOutputStream out_data;
    private DataInputStream in_data;
    private BigInteger publicKey;
    private BigInteger modulus;

    /**
     * Creates a new {@code Client} instance with a given hostname and port number.
     *
     * @param hostname     hostname of server
     * @param portNumber   port number server is hosting the game on
     * @throws IOException
     */
    public Client(String hostname, int portNumber) throws IOException {
        socket = new Socket(hostname, portNumber);
        socket.setSoTimeout(20000);
        out_data = new DataOutputStream(socket.getOutputStream());
        in_data = new DataInputStream(socket.getInputStream());
        System.out.println("Connected!");
    }

    /**
     * @return Public key from server
     */
    @Override
    public BigInteger getPublicKey() {
        return publicKey;
    }

    /**
     * @return Modulus from server
     */
    @Override
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * @return A {@code String} from the socket connection, after checking to see if the message is the public key.
     * @throws IOException
     */
    @Override
    public String read() throws IOException {
        String read = super.read();
        Matcher keyMatcher = Pattern.compile("KEY: (?<key>\\d+)").matcher(read);
        Matcher modMatcher = Pattern.compile("MOD: (?<mod>\\d+)").matcher(read);
        if (keyMatcher.find())
            publicKey = new BigInteger(keyMatcher.group("key"));
        if (modMatcher.find())
            modulus = new BigInteger(modMatcher.group("mod"));
        return read;
    }

    /**
     * If an encryption key exists, encrypt the data, then pass it to the super method.
     *
     * @param data Byte array of data
     * @throws IOException
     */
    @Override
    public void writeBytes(byte[] data) throws IOException {
        if (getPublicKey() != null && getModulus() != null) {
            // Encrypt data
            int splitFactor = 1;
            BigInteger[] encrypted = new BigInteger[data.length];
            for (int i = 0; i < data.length; i++) {
                BigInteger encryptedByte = BigInteger.valueOf(data[i]).modPow(getPublicKey(), getModulus());
                splitFactor = Math.max(splitFactor, encryptedByte.toByteArray().length);
                encrypted[i] = encryptedByte;
            }
            // Split data into byte-sized pieces (pun intended)
            byte[] encryptedBytes = new byte[data.length * splitFactor];
            for (int i = 0; i < encryptedBytes.length; i += splitFactor) {
                // Copy data to array of length splitFactor
                byte[] byteArray = Arrays.copyOf(encrypted[i / splitFactor].toByteArray(), splitFactor);
                System.arraycopy(byteArray, 0, encryptedBytes, i, splitFactor);
            }
            getDataOutputStream().writeInt(splitFactor);
            super.writeBytes(encryptedBytes);
        }
        else super.writeBytes(data);
    }

    /**
     * @return Socket data input stream
     */
    @Override
    public DataInputStream getDataInputStream() {
        return in_data;
    }

    /**
     * @return Socket data output stream
     */
    @Override
    public DataOutputStream getDataOutputStream() {
        return out_data;
    }

    /**
     * Closes socket connection with server
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }

    /**
     * Plays rock-paper-scissors game with the server. Continually scans for server messages and terminates the
     * loop when the game is over.
     *
     * @throws IOException
     */
    @Override
    public void playGame() throws IOException {
        boolean keepConnection = true;
        Console c = System.console();
        while (keepConnection) {
            String serverResponse = read();
            switch (serverResponse) {
                case "PROMPT_MOVE":
                    // Send move to server
                    System.out.println();
                    boolean validMoveWasEntered = false;
                    while (!validMoveWasEntered) {
                        try {
                            String move = c.readLine("Enter your move: ");
                            int moveInt = Player.getMoveFromString(move);
                            write("MOVE: " + moveInt);
                            validMoveWasEntered = true;
                        } catch (IllegalArgumentException e) {
                            // just try again
                            System.out.println("Invalid input.");
                        }
                    }
                    System.out.println("Waiting for opponent...");
                    break;
                case "WIN":
                    System.out.println();
                    System.out.println("You win!");
                    break;
                case "LOSE":
                    System.out.println();
                    System.out.println("You lose!");
                    break;
                case "TIE":
                    System.out.println();
                    System.out.println("Tie. Try again");
                    break;
                case "END":
                    keepConnection = false;
                    break;
            }
        }
        c.readLine("Press ENTER to end"); // Probably the laziest way I could've done this, I know
    }
}

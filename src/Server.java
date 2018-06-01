import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is the server side of the game. It opens a socket connection which the client accepts, and runs the game,
 * as well as participating as a player.
 *
 * @author grrdozier
 */
public class Server extends NetworkPlayer {
    private ServerSocket socket;
    private DataOutputStream out_data;
    private DataInputStream in_data;
    private BigInteger publicKey;
    private BigInteger privateKey;
    private BigInteger modulus;

    /**
     * Creates a new {@code Server} instance, opens up a socket connection over the given port number, waits for the
     * client to connect, then sends the encryption key to the client.
     *
     * @param portNumber   Port number for socket connection
     * @throws IOException
     */
    public Server(int portNumber) throws IOException {
        computeKey();
        socket = new ServerSocket(portNumber);
        Socket client = socket.accept();
        System.out.println("Connected!");
        client.setSoTimeout(20000);
        in_data = new DataInputStream(client.getInputStream());
        out_data = new DataOutputStream(client.getOutputStream());
        write("KEY: " + getPublicKey());
        write("MOD: " + getModulus());
        System.out.println("Key sent to client...");
    }

    /**
     * Computes RSA key from two hard-coded prime numbers.
     */
    public void computeKey() {
        System.out.print("Computing Encryption Key...");
        // Two prime numbers.
        final BigInteger p = BigInteger.valueOf(45481);
        final BigInteger q = BigInteger.valueOf(45691);
        // Will find this many possible exponents before settling on one (otherwise the exponents would always be 1)
        final int securityPublic = 5;
        final int securityPrivate = 1;
        // Modulus
        BigInteger n = p.multiply(q);
        // Find totient t = lcm(p-1, q-1) = lcm(a, b)
        BigInteger t = BigInteger.ZERO;
        BigInteger k = BigInteger.ONE;
        BigInteger l = BigInteger.ONE;
        BigInteger a = p.subtract(BigInteger.ONE);
        BigInteger b = q.subtract(BigInteger.ONE);
        while (t.equals(BigInteger.ZERO)) {
            if (a.multiply(k).equals(b.multiply(l)))
                t = a.multiply(k);
            else if (k.multiply(a).min(l.multiply(b)).equals(k.multiply(a)))
                k = k.add(BigInteger.ONE);
            else
                l = l.add(BigInteger.ONE);
        }
        System.out.print(".");
        // Find public exponent e coprime to t
        BigInteger e = BigInteger.ZERO;
        int securityCountPublic = 0;
        while (e.equals(BigInteger.ZERO)) {
            for (BigInteger i = BigInteger.ONE; i.compareTo(t) < 0; i = i.add(BigInteger.ONE)) {
                e = i.gcd(t).equals(BigInteger.ONE) && ++securityCountPublic == securityPublic ? i : e;
                if (!e.equals(BigInteger.ZERO))
                    i = t;
            }
        }
        System.out.print(".");
        // Find private exponent d st de % t == 1
        BigInteger d = BigInteger.ZERO;
        int securityCountPrivate = 0;
        boolean found = false;
        while (!found) {
            d = d.add(BigInteger.ONE);
            found = d.multiply(e).mod(t).equals(BigInteger.ONE) && ++securityCountPrivate == securityPrivate;
        }
        System.out.print(".");

        modulus = n;
        publicKey = e;
        privateKey = d;

        System.out.print("Done." + System.lineSeparator());
    }

    /**
     * @return RSA Modulus
     */
    @Override
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * @return RSA Public Exponent
     */
    @Override
    public BigInteger getPublicKey() {
        return publicKey;
    }

    /**
     * @return RSA Private Exponent
     */
    public BigInteger getPrivateKey() {
        return privateKey;
    }

    /**
     * @return {@code DataInputStream} for incoming data
     */
    @Override
    public DataInputStream getDataInputStream() {
        return in_data;
    }

    /**
     * @return {@code DataOutputStream} for outgoing data
     */
    @Override
    public DataOutputStream getDataOutputStream() {
        return out_data;
    }

    /**
     * Closes the socket connection
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }

    /**
     * If an RSA key exists, decrypts the incoming data and then passes the decrypted bytes to the super method.
     * Otherwise, simply calls the super method
     *
     * @return {@code byte} array of data from the {@code DataInputStream}
     * @throws IOException
     */
    @Override
    public byte[] readBytes() throws IOException {
        if (getPublicKey() != null && getModulus() != null) {
            int splitFactor = getDataInputStream().readInt();
            byte[] encryptedBytes = super.readBytes();
            BigInteger[] encryptedInts = new BigInteger[encryptedBytes.length / splitFactor];
            // Merge bytes back into BigIntegers
            for (int i = 0; i < encryptedBytes.length; i += splitFactor) {
                byte[] bytes = Arrays.copyOfRange(encryptedBytes, i, i + splitFactor);
                // Truncate empty bytes
                int j = 0;
                do
                    j++;
                while (j < bytes.length && bytes[j] != 0);
                bytes = Arrays.copyOfRange(bytes, 0, j);
                encryptedInts[i / splitFactor] = new BigInteger(bytes);
            }
            // Decrypt the BigIntegers
            byte[] decryptedBytes = new byte[encryptedInts.length];
            for (int i = 0; i < decryptedBytes.length; i++)
                decryptedBytes[i] = encryptedInts[i].modPow(getPrivateKey(), getModulus()).byteValueExact();
            return decryptedBytes;
        }
        else return super.readBytes();
    }

    /**
     * Starts a rock-paper-scissors game with the client, then closes the socket when the game is over.
     *
     * @throws IOException
     */
    @Override
    public void playGame() throws IOException {
        boolean keepConnection = true;
        Player server = new Player();
        Player client = new Player();
        while (keepConnection) {
            try {
                // Prompt the client for their move
                write("PROMPT_MOVE");
                // Prompt the user for their move
                Console c = System.console();
                System.out.println();
                boolean validMoveWasEntered = false;
                while (!validMoveWasEntered) {
                    try {
                        String move = c.readLine("Enter your move: ");
                        server.setMove(Player.getMoveFromString(move));
                        validMoveWasEntered = true;
                    } catch (IllegalArgumentException e) {
                        // just try again
                        System.out.println("Invalid input.");
                    }
                }

                // Get move from client
                System.out.println("Waiting for opponent...");
                String response = read();
                Matcher m = Pattern.compile("MOVE: (?<move>\\d)").matcher(response);
                if (m.find())
                    client.setMove(Integer.parseInt(m.group("move")));
                else throw new IOException("Unrecognized response: " + response);
                Player winner = Player.getWinner(server, client);
                if (winner == server) {
                    System.out.println();
                    System.out.println("You win!");
                    write("LOSE");
                    keepConnection = false;
                } else if (winner == client) {
                    System.out.println();
                    System.out.println("You lose!");
                    write("WIN");
                    keepConnection = false;
                } else if (winner == null) {
                    System.out.println();
                    System.out.println("Tie. Try again");
                    write("TIE");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input. Try again.");
                e.printStackTrace();
            }
        }
        write("END");
        close();
        Console c = System.console();
        c.readLine("Press ENTER to end"); // Probably the laziest way I could've done this, I know
    }
}

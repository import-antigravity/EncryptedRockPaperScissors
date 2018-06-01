import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * This is the parent class to both {@code Client} and {@code Server}.
 * @author grrdozier
 */
public abstract class NetworkPlayer {
    /**
     * First reads an {@code int} containing the length of the message to be received, then reads that many bytes from
     * the {@code DataInputStream}.
     *
     * @return Byte array containing message
     * @throws IOException
     */
    public byte[] readBytes() throws IOException {
        int length = getDataInputStream().readInt();
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++)
            data[i] = (byte) getDataInputStream().read();
        return data;
    }

    /**
     * First writes an {@code int} containing the length of the message to be sent, then writes that many bytes to
     * the {@code DataOutputStream}.
     *
     * @param data Byte array to be sent
     * @throws IOException
     */
    public void writeBytes(byte[] data) throws IOException {
        // Send data
        getDataOutputStream().writeInt(data.length);
        getDataOutputStream().write(data, 0, data.length);
        getDataOutputStream().flush();
    }

    /**
     * @return An ASCII string from the byte array returned by {@code readBytes()}.
     * @throws IOException
     */
    public String read() throws IOException {
        byte[] data = readBytes();
        return new String(data, 0, data.length, StandardCharsets.US_ASCII);
    }

    /**
     * Encodes an ASCII string into a byte array, then writes it.
     *
     * @param msg String to be sent
     * @throws IOException
     */
    public void write(String msg) throws IOException {
        // Encode string to binary
        byte[] data = msg.getBytes(StandardCharsets.US_ASCII);
        writeBytes(data);
    }

    public abstract BigInteger getModulus();

    public abstract BigInteger getPublicKey();

    public abstract DataInputStream getDataInputStream();

    public abstract DataOutputStream getDataOutputStream();

    public abstract void close() throws IOException;

    public abstract void playGame() throws IOException;
}

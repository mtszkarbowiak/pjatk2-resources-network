package rscnet.communication;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Connection {
    /**
     * Attempts to send a message in a form of string.
     * @param message The message to be carried.
     * @throws IOException Any kind of exception thrown during the attempts.
     */
    void send(String message)throws IOException;

    /**
     * Blocks the thread until receiving the packet or timeout or information about no information to be sent.
     * @return Message or null.
     * @throws IOException Any kind of exception thrown during attempts of packet receiving.
     */
    String receive() throws IOException;

    /**
     * Address of a server socket, no matter what is the type of transport-layer communication.
     * @return Address of a server socket.
     */
    InetSocketAddress getRemoteSocketAddress();

    /**
     * Cleans up (e.g. closing, disposing) all resources used by the connection.
     * @throws IOException  Any kind of exception thrown during the cleanup. (Will not be re-attempted)
     */
    void close() throws IOException;
}
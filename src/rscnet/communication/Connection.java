package rscnet.communication;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Connection {
    void send(String message)throws IOException;

    // Returns NULL when nothing else to be received.
    String receive() throws IOException;

    InetSocketAddress getRemoteSocketAddress();

    InetSocketAddress getLocalSocketAddress();

    void close() throws IOException;
}

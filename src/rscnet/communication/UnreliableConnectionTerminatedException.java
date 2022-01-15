package rscnet.communication;

import java.io.IOException;

public class UnreliableConnectionTerminatedException extends IOException {
    public UnreliableConnectionTerminatedException(String message) {
        super(message);
    }
}

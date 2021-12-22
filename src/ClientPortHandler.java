import java.io.*;
import java.net.*;

public class ClientPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Client"; }
    @Override protected boolean keepAlive() { return true; }

    private InetSocketAddress inetSocketAddress;
    private Socket socket;

    public ClientPortHandler(InetAddress gatewayAddress, int gatewayPort) {
        this.inetSocketAddress = new InetSocketAddress(gatewayAddress, gatewayPort);
    }

    @Override
    protected void update() throws IOException {
        if(socket == null || socket.isConnected() == false){
            socket = new Socket();
            socket.connect(inetSocketAddress);
            log("Connection established.");
        }
    }

    @Override
    protected void onHalted() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

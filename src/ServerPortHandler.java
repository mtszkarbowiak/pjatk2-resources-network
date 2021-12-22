import java.io.*;
import java.net.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Server"; }
    @Override protected boolean keepAlive() { return true; }

    private int hostingPort;
    private ServerSocket serverSocket;

    public ServerPortHandler(int hostingPort) {
        this.hostingPort = hostingPort;
    }

    @Override
    protected void update() throws IOException {
        if(serverSocket == null || serverSocket.isClosed()){
            serverSocket = new ServerSocket(hostingPort);
        }

        var socket = serverSocket.accept();
        log("Connection established.");
    }

    @Override
    protected void onHalted() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
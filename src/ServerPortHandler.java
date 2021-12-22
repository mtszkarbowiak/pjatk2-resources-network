import java.io.*;
import java.net.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Server"; }
    @Override protected boolean keepAlive() { return true; }

    private ServerSocket serverSocket;
    private AppConfig config;

    public ServerPortHandler(AppConfig config) {
        this.config = config;
    }

    @Override
    protected void update() throws IOException {
        if(serverSocket == null || serverSocket.isClosed()){
            serverSocket = new ServerSocket(config.getHostingPort());
        }

        var currentSocket = serverSocket.accept();

        log("Connection established. Awaiting request.", LogType.Info);
        var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        var request = reader.readLine();
        var args = request.split(" ");

        switch (args[0]) {
            case NetCommandFormatting.HeadRequest -> {
                log("Requested sign to master.", LogType.In);
                if (config.isMasterHost()) {
                    writer.write(NetCommandFormatting.HeadResponseMeMaster);
                    log("Responded it's me.", LogType.Out);
                } else {
                    writer.write(NetCommandFormatting.HeadResponseAboutMaster
                            + " " + config.getGatewayAddress().getHostAddress()
                            + " " + config.getGatewayPort());
                    log("Responded with address to the master.", LogType.Out);
                }
                writer.newLine();
                writer.flush();
            }

            default -> log("Unrecognized request: " + args[0], LogType.Problem);
        }
    }

    @Override
    protected void onHalted() {
        try {
            if(serverSocket.isClosed() == false)
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
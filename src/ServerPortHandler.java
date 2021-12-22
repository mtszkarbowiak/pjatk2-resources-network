import java.io.*;
import java.net.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Server"; }
    @Override protected boolean keepAlive() { return true; }

    private ServerSocket serverSocket;
    private final AppConfig config;
    private final SlaveRegistry slaveRegistry;

    public static ServerPortHandler createSlaveServerPortHandler(AppConfig config){
        return new ServerPortHandler(config, null);
    }

    public static ServerPortHandler createMasterServerPortHandler(AppConfig config, SlaveRegistry slaveRegistry){
        return new ServerPortHandler(config, slaveRegistry);

    }

    private ServerPortHandler(AppConfig config, SlaveRegistry slaveRegistry) {
        this.config = config;
        this.slaveRegistry = slaveRegistry;
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
                log("Requested sign to master. (" + request + ")", LogType.In);
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

            case NetCommandFormatting.RegistrationRequest -> {
                log("Requested registration. (" + request + ")", LogType.In);
                var identifier = Integer.parseInt(args[1]);
                var slaveSocketAddress = currentSocket.getRemoteSocketAddress();
                var pass = slaveRegistry.tryRegister(identifier, slaveSocketAddress);

                if(pass){
                    writer.write(NetCommandFormatting.RegistrationResponseSuccess);
                    writer.newLine();
                    writer.flush();

                    log("Accepted registration.", LogType.Out);
                }else{
                    writer.write(NetCommandFormatting.RegistrationResponseDeny);
                    writer.newLine();
                    writer.flush();

                    log("Denied.", LogType.Out);
                }
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
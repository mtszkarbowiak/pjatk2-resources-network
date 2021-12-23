import java.io.*;
import java.net.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "> Server"; }
    @Override protected boolean keepAlive() { return true; }

    private ServerSocket serverSocket;
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final SlaveRegistry slaveRegistry;


    public static ServerPortHandler createSlaveServerPortHandler(
            AppConfig config, InternalCommunication internalCommunication){
        return new ServerPortHandler(config, internalCommunication, null);
    }

    public static ServerPortHandler createMasterServerPortHandler(
            AppConfig config, InternalCommunication internalCommunication, SlaveRegistry slaveRegistry){
        return new ServerPortHandler(config, internalCommunication, slaveRegistry);

    }

    private ServerPortHandler(AppConfig config, InternalCommunication internalCommunication, SlaveRegistry slaveRegistry) {
        this.config = config;
        this.internalCommunication = internalCommunication;
        this.slaveRegistry = slaveRegistry;
    }


    @Override
    protected void update() throws IOException {
        if(serverSocket == null || serverSocket.isClosed()){
            serverSocket = new ServerSocket(config.getHostingPort());
        }

        var currentSocket = serverSocket.accept();

        final var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        final var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        final var request = reader.readLine();
        final var args = request.split(" ");

        switch (args[0]) {
            case NetCommands.HeadRequest -> {
                log("Requested sign to master. (" + request + ")", LogType.In);
                if (config.isMasterHost()) {
                    writer.write(NetCommands.HeadResponseMeMaster);
                    log("Responded it's me.", LogType.Out);
                } else {
                    writer.write(NetCommands.HeadResponseAboutMaster
                            + " " + config.getGatewayAddress().getHostAddress()
                            + " " + config.getGatewayPort());
                    log("Responded with address to someone else to ask.", LogType.Out);
                }
                writer.newLine();
                writer.flush();
            }

            case NetCommands.RegistrationRequest -> {
                log("Requested registration. (" + request + ")", LogType.In);
                var identifier = Integer.parseInt(args[1]);
                var slaveSocketAddress = currentSocket.getRemoteSocketAddress();
                var pass = slaveRegistry.tryRegister(identifier, slaveSocketAddress);

                if(pass){
                    writer.write(NetCommands.RegistrationResponseSuccess);
                    writer.newLine();
                    writer.flush();

                    log("Accepted registration.", LogType.Out);
                }else{
                    writer.write(NetCommands.RegistrationResponseDeny);
                    writer.newLine();
                    writer.flush();

                    log("Denied.", LogType.Out);
                }
            }

            default -> {
                log("Incoming (interpreted allocations) request: " + request, LogType.In);

                var allocationsRequest = new AllocationRequest(request);
                internalCommunication.passAllocationRequest(allocationsRequest);
            }
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
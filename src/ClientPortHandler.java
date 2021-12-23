import java.io.*;
import java.net.*;

public class ClientPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Client >"; }
    @Override protected boolean keepAlive() { return isRegistered == false; } //TODO Client actions

    // You can ask a friend where is the master.
    private final InetSocketAddress friendInetSocketAddress;
    private final AppConfig config;
    private InetSocketAddress overFriendSocketAddress;
    private InetSocketAddress masterInetSocketAddress;
    private Socket currentSocket;
    private boolean isRegistered;

    public ClientPortHandler(AppConfig config) {
        this.config = config;
        this.friendInetSocketAddress = new InetSocketAddress(
                config.getGatewayAddress(),
                config.getGatewayPort());

        this.overFriendSocketAddress = null;
        this.masterInetSocketAddress = null;
        this.currentSocket = null;
        this.isRegistered = false;
    }

    @Override
    protected void update() throws IOException {
        if(masterInetSocketAddress == null){
            requestMasterAddress();
        }

        if(masterInetSocketAddress == null){
            log("Master's address is still uknown.", LogType.Info);
            return;
        }

        log("Establishing connection to the master.", LogType.Info);
        if(currentSocket == null)
            currentSocket = new Socket();
        if(currentSocket.isConnected() == false)
            currentSocket.connect(masterInetSocketAddress);

        // log("Connection with the master established.", LogType.Info);
        var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        if(isRegistered == false){
            isRegistered = requestRegistration(writer, reader);

            //TODO Client actions
            if(isRegistered == false)
                currentSocket.close();
        }

        //TODO Client actions
    }

    private void requestMasterAddress() throws IOException {
        if(overFriendSocketAddress == null) {
            log("Establishing connection to the base friend.", LogType.Info);
            overFriendSocketAddress = friendInetSocketAddress;
        }else{
            log("Establishing connection to the over-friend: " + overFriendSocketAddress, LogType.Info);
        }

        currentSocket = new Socket();
        currentSocket.connect(overFriendSocketAddress);

        log("Connection established.", LogType.Info);
        var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        log("Asking for the master...", LogType.Out);
        writer.write(NetCommandFormatting.HeadRequest);
        writer.newLine();
        writer.flush();

        var responseAboutMaster = reader.readLine();

        var args = responseAboutMaster.split(" ");
        switch (args[0]) {
            case NetCommandFormatting.HeadResponseAboutMaster -> {
                var masterAddress = InetAddress.getByName(args[1]);
                var masterPort = Integer.parseInt(args[2]);
                overFriendSocketAddress = new InetSocketAddress(masterAddress, masterPort);
                log("Next over-friend aknowledged: " + overFriendSocketAddress, LogType.In);
            }

            case NetCommandFormatting.HeadResponseMeMaster -> {
                masterInetSocketAddress = overFriendSocketAddress;
                overFriendSocketAddress = null;
                log("Friend is the master!", LogType.In);
            }

            case NetCommandFormatting.HeadResponseFail -> {
                masterInetSocketAddress = null;
                overFriendSocketAddress = null;
                log("Friend does not know any master and has no friends.", LogType.In);
            }
        }

        currentSocket.close();
        currentSocket = null;
    }

    private boolean requestRegistration(BufferedWriter writer, BufferedReader reader) throws IOException {
        log("Requesting registration.", LogType.Out);
        writer.write(NetCommandFormatting.RegistrationRequest + " " + config.getIdentifier());
        writer.newLine();
        writer.flush();

        var response = reader.readLine();
        switch (response) {
            case NetCommandFormatting.RegistrationResponseSuccess -> {
                log("Master successfully registered me.", LogType.In);
                return true;
            }
            case NetCommandFormatting.RegistrationResponseDeny -> {
                log("Master denied registration.", LogType.In);
                return false;
            }
            default -> {
                log("Invalid response (" + response + ")", LogType.Problem);
                return false;
            }
        }
    }

    @Override
    protected void onHalted() {
        try {
            currentSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

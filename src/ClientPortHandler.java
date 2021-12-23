import java.io.*;
import java.net.*;

public class ClientPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Client >"; }
    @Override protected boolean keepAlive() { return true; }

    // You can ask a friend where is the master.
    private final InetSocketAddress friendInetSocketAddress;
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private InetSocketAddress overFriendSocketAddress;
    private InetSocketAddress masterInetSocketAddress;
    private Socket currentSocket;

    public ClientPortHandler(AppConfig config, InternalCommunication internalCommunication) {
        this.config = config;
        this.friendInetSocketAddress = new InetSocketAddress(
                config.getGatewayAddress(),
                config.getGatewayPort());
        this.internalCommunication = internalCommunication;

        this.overFriendSocketAddress = null;
        this.masterInetSocketAddress = null;
        this.currentSocket = null;
    }

    @Override
    protected void update() throws IOException {
        // Check if client has anything productive to do. Wait without opening ports if not necessary.
        int idleDt = 0;
        while (internalCommunication.isIdle()) {
            int dT = 100;
            sleep(dT);
            idleDt++;

            if(idleDt % 100 == 0) {
                int secs = idleDt * dT / 1000;
                log("Client is idle for " + secs + " secs.", LogType.Info);
            }
        }

        // Establish who is the master host
        if(masterInetSocketAddress == null){
            requestMasterAddress();

            if(masterInetSocketAddress == null){
                log("Master's address is still uknown.", LogType.Info);
                return;
            }
        }

        // Establish connection to the master host
        log("Establishing connection to the master.", LogType.Info);
        if(currentSocket == null)
            currentSocket = new Socket();
        if(currentSocket.isConnected() == false)
            currentSocket.connect(masterInetSocketAddress);

        final var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        final var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        // Register if not already done
        if(internalCommunication.isRegistered() == false){
            if(requestRegistration(writer, reader))
                internalCommunication.markRegistered();
        }

        // Close occupied socket if there are no more things to do
        if(internalCommunication.isIdle())
            currentSocket.close();

        //TODO Internal communication requests handling.
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
        final var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        final var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

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

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
        sleepUntilWork();

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
        if(currentSocket.isConnected() == false) {
            currentSocket.connect(masterInetSocketAddress);
        }

        final var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
        final var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

        // Register if not already done
        if(internalCommunication.isRegistered() == false){
            if(requestRegistration(writer, reader))
                internalCommunication.markRegistered();
        }

        while (internalCommunication.pendingAllocationRequests.isEmpty() == false){
            var request = internalCommunication.pendingAllocationRequests.remove();
            log("Passing Allocation Request to Master is not yet implemented.", LogType.Problem);
        }

        // Close occupied socket if there are no more things to do
        if(internalCommunication.isIdle()) {
            log("Disconnecting.", LogType.Info);
            currentSocket.close();
            currentSocket = null;
        }

        //TODO Internal communication requests handling.
    }


    private void sleepUntilWork() {
        int interval = 100;
        final int intervalIncrement = 200;
        final int logDisplayNo = 5;
        int totalSleep = 0;
        int logIndex = 1;

        while (internalCommunication.isIdle()) {
            var currentSleepTime = interval;

            sleep(currentSleepTime);

            totalSleep += currentSleepTime;
            interval += intervalIncrement;

            if(logIndex++ % logDisplayNo == 0)
                log("Waiting idle for " + (totalSleep / 1000) + " sec.", LogType.Info);
        }
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
        writer.write(NetCommands.HeadRequest);
        writer.newLine();
        writer.flush();

        var responseAboutMaster = reader.readLine();

        var args = responseAboutMaster.split(" ");
        switch (args[0]) {
            case NetCommands.HeadResponseAboutMaster -> {
                var masterAddress = InetAddress.getByName(args[1]);
                var masterPort = Integer.parseInt(args[2]);
                overFriendSocketAddress = new InetSocketAddress(masterAddress, masterPort);
                log("Next over-friend aknowledged: " + overFriendSocketAddress, LogType.In);
            }

            case NetCommands.HeadResponseMeMaster -> {
                masterInetSocketAddress = overFriendSocketAddress;
                overFriendSocketAddress = null;
                log("Friend is the master!", LogType.In);
            }

            case NetCommands.HeadResponseFail -> {
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
        writer.write(NetCommands.RegistrationRequest + " " + config.getIdentifier());
        for (var keyVal : config.getResourcesSpaces().entrySet()) {
            writer.write(" " + keyVal.getKey() + ":" + keyVal.getValue());
        }

        writer.newLine();
        writer.flush();

        var response = reader.readLine();
        switch (response) {
            case NetCommands.RegistrationResponseSuccess -> {
                log("Master successfully registered me.", LogType.In);
                return true;
            }
            case NetCommands.RegistrationResponseDeny -> {
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

import java.io.*;
import java.net.*;

public class ClientPortHandler extends AbstractPortHandler
{
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final InetSocketAddress friendSocketAddress;
    private InetSocketAddress masterSocketAddress;
    private boolean isMasterTrue;

    protected String getLogPrefix() { return "Client >"; }


    public ClientPortHandler(AppConfig config, InternalCommunication internalCommunication) {
        this.config = config;
        this.friendSocketAddress = new InetSocketAddress(
                config.getGatewayAddress(),
                config.getGatewayPort());
        this.internalCommunication = internalCommunication;
    }


    @Override
    protected Socket openConnection() throws IOException {
        sleepUntilWork();
        var result = new Socket();

        if(masterSocketAddress != null)
            result.connect(masterSocketAddress);
        else
            result.connect(friendSocketAddress);

        return result;
    }

    @Override
    protected void useConnection(
            BufferedReader reader,
            BufferedWriter writer,
            ConnectionInfo connectionInfo) throws IOException
    {
        if(isMasterTrue == false){
            requestMasterAddress(reader, writer);
            return;
        }

        if(internalCommunication.isRegistered() == false){
            requestRegistration(reader, writer);
            return;
        }

        while (internalCommunication.pendingAllocationRequests.isEmpty() == false){
            var request = internalCommunication.pendingAllocationRequests.remove();
            log("Passing Allocation Request to Master is not yet implemented.", LogType.Problem);
        }

        //TODO Internal communication requests handling.
    }


    private void requestMasterAddress(
            BufferedReader reader,
            BufferedWriter writer) throws IOException
    {
        if(masterSocketAddress == null) {
            log("Establishing connection to the base friend.", LogType.Info);
            isMasterTrue = false;
        }else{
            log("Establishing connection to the over-friend: " + masterSocketAddress, LogType.Info);
        }

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
                masterSocketAddress = new InetSocketAddress(masterAddress, masterPort);
                isMasterTrue = false;
                log("Next over-friend aknowledged: " + masterSocketAddress, LogType.In);
            }

            case NetCommands.HeadResponseMeMaster -> {
                isMasterTrue = true;
                log("Friend is the master!", LogType.In);
            }

            case NetCommands.HeadResponseFail -> {
                masterSocketAddress = null;
                isMasterTrue = false;
                log("Friend does not know any master and has no friends.", LogType.In);
            }
        }
    }


    private void requestRegistration(
            BufferedReader reader,
            BufferedWriter writer) throws IOException
    {
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
                internalCommunication.markRegistered();
            }
            case NetCommands.RegistrationResponseDeny ->
                    log("Master denied registration.", LogType.In);

            default -> 
                    log("Invalid response (" + response + ")", LogType.Problem);
        }
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
}
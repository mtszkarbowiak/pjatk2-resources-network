package rscnet.communication;

import rscnet.data.AppConfig;
import rscnet.InternalCommunication;
import rscnet.logging.*;
import rscnet.utils.ConnectionUtils;
import rscnet.utils.ThreadBlocker;
import rscnet.utils.ThreadBlocking;

import java.io.*;
import java.net.*;

public class ClientSubhostPortHandler extends AbstractPortHandler
{
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final InetSocketAddress friendSocketAddress;
    private final UnreliableConnectionFactory unreliableConnectionFactory;
    private InetSocketAddress masterSocketAddress;
    private boolean isMasterTrue;

    public ClientSubhostPortHandler(
            AppConfig config,
            InternalCommunication internalCommunication,
            UnreliableConnectionFactory unreliableConnectionFactory) {
        this.config = config;
        this.internalCommunication = internalCommunication;
        this.unreliableConnectionFactory = unreliableConnectionFactory;
        this.friendSocketAddress = new InetSocketAddress(
                config.getGatewayAddress(),
                config.getGatewayPort());
    }


    @Override
    protected Connection openConnection() throws IOException {
        sleepUntilWork();

        if(getKeepAlive() == false)
            return null;

        // If possible communicate through unreliable channel.
        if( isMasterTrue &&
            internalCommunication.registrationConfirmation.getValue() &&
            unreliableConnectionFactory != null){

            var unreliableMasterSocketAddress = new InetSocketAddress(
                    masterSocketAddress.getAddress(),
                    masterSocketAddress.getPort() + 100);

            return unreliableConnectionFactory.openUnreliableConnection(
                    unreliableMasterSocketAddress);
        }

        var result = new Socket();
        var address =
                masterSocketAddress != null ? masterSocketAddress : friendSocketAddress;

        result.connect(address);

        return new ReliableConnection(result);
    }


    @Override
    protected void useConnection(Connection connection) throws IOException
    {
        if(isMasterTrue == false){
            requestMasterAddress(connection);
            return;
        }

        if(internalCommunication.registrationConfirmation.getValue() == false){
            requestRegistration(connection);
            return;
        }

        if(internalCommunication.allocationRequestInternalPass.hasValue()){
            requestAllocation(connection);
        }
    }


    private void requestMasterAddress(Connection connection) throws IOException
    {
        if(masterSocketAddress == null) {
            log("Establishing connection to the base friend.", LogType.Info);
            isMasterTrue = false;
        }else{
            log("Establishing connection to the over-friend: " + masterSocketAddress, LogType.Info);
        }

        log("Asking for the master...", LogType.Out);
        connection.send(NetCommands.HeadRequest);

        var responseAboutMaster = connection.receive();

        var args = responseAboutMaster.split(" ");
        switch (args[0]) {
            case NetCommands.HeadResponseAboutMaster -> {
                var masterAddress = InetAddress.getByName(args[1]);
                var masterPort = Integer.parseInt(args[2]);
                masterSocketAddress = new InetSocketAddress(masterAddress, masterPort);

                isMasterTrue = false;
                log("Next potential master acknowledged: " + masterSocketAddress, LogType.In);
            }

            case NetCommands.HeadResponseMeMaster -> {
                masterSocketAddress = connection.getRemoteSocketAddress();

                isMasterTrue = true;
                log("Friend is the master! Master validated.", LogType.In);
            }

            case NetCommands.HeadResponseFail -> {
                masterSocketAddress = null;
                isMasterTrue = false;
                log("Friend does not know any master and has no friends. (Error)", LogType.In);
            }
        }
    }


    private void requestRegistration(Connection connection) throws IOException
    {
        log("Requesting registration.", LogType.Out);

        var msg = new StringBuilder();

        msg .append(NetCommands.RegistrationRequest + " ")
            .append(config.getIdentifier());

        for (var keyVal : config.getResourcesSpaces().entrySet()) {
            msg .append(" ")
                .append(keyVal.getKey())
                .append(":")
                .append(keyVal.getValue());
        }
        connection.send(msg.toString());

        var response = connection.receive();
        switch (response) {
            case NetCommands.RegistrationResponseSuccess -> {
                log("Master successfully registered me.", LogType.In);
                internalCommunication.registrationConfirmation.pass(true);
            }
            case NetCommands.RegistrationResponseDeny ->
                    log("Master denied registration.", LogType.In);

            default ->
                    log("Invalid response (" + response + ")", LogType.Problem);
        }
    }


    private void requestAllocation(Connection connection) throws IOException
    {
        var request = internalCommunication.allocationRequestInternalPass.getValue();

        log("Sending request to the master", LogType.Out);
        connection.send(request.toString());

        log("Reading results.", LogType.In);
        var totalResponse = ConnectionUtils.receiveMultiline(connection);

        log("Passing results to the server.", LogType.Info);
        internalCommunication.allocationResponseInternalPass.pass(totalResponse);
    }


    private void sleepUntilWork() {
        var blocker = new ThreadBlocker() {
            @Override
            public boolean keepBlocking() {
                return internalCommunication.registrationConfirmation.getValue() &&
                        internalCommunication.allocationRequestInternalPass.hasValue() == false &&
                        getKeepAlive();
            }
        };

        ThreadBlocking.wait(blocker, this);
    }

    @Override
    protected String getLogPrefix() { return "Client >"; }
}
package rscnet.communication;

import rscnet.data.AllocationRequest;
import rscnet.data.AppConfig;
import rscnet.InternalCommunication;
import rscnet.logging.*;
import rscnet.utils.*;

import java.io.*;
import java.net.*;
import java.util.Map;

import static rscnet.Constants.App.NULL_PORT;
import static rscnet.Constants.NetCommands.*;

@SuppressWarnings("PointlessBooleanExpression")
public class ClientSubHostPortHandler extends AbstractPortHandler
{
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final InetSocketAddress friendSocketAddress;
    private final UnreliableConnectionFactory unreliableConnectionFactory;
    private InetSocketAddress masterTcpSocketAddress;
    private InetSocketAddress masterUdpSocketAddress;
    private boolean isMasterTrue;

    public ClientSubHostPortHandler(
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
        @SuppressWarnings("PointlessBooleanExpression")
        ThreadBlocker blocker = () ->
                internalCommunication.registrationConfirmation.getValue() &&
                internalCommunication.allocationRequestInternalPass.hasValue() == false &&
                internalCommunication.terminationRequestInternalPass.hasValue() == false &&
                getKeepAlive();

        ThreadBlocking.wait(blocker, this);


        if(getKeepAlive() == false)
            return null;

        // If possible communicate through unreliable channel.
        if( isMasterTrue &&
            internalCommunication.registrationConfirmation.getValue() &&
            unreliableConnectionFactory != null){

            return unreliableConnectionFactory.openUnreliableConnection(masterUdpSocketAddress);
        }

        Socket result = new Socket();
        InetSocketAddress address =
                masterTcpSocketAddress != null ? masterTcpSocketAddress : friendSocketAddress;

        result.connect(address);

        return new ReliableConnection(result);
    }


    @SuppressWarnings("UnnecessaryReturnStatement")
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
            return;
        }

        if(internalCommunication.terminationRequestInternalPass.hasValue()){
            requestTermination(connection);
            return;
        }
    }


    private void requestMasterAddress(Connection connection) throws IOException
    {
        if(masterTcpSocketAddress == null) {
            log("Establishing connection to the base friend.", LogType.Info);
            isMasterTrue = false;
        }else{
            log("Establishing connection to the over-friend: " + masterTcpSocketAddress, LogType.Info);
        }

        log("Asking for the master...", LogType.Out);
        connection.send(HEAD_REQUEST);

        String responseAboutMaster = connection.receive();

        String[] args = responseAboutMaster.split(" ");
        switch (args[0]) {
            case HEAD_RESPONSE_ABOUT_MASTER: {
                InetAddress masterAddress = InetAddress.getByName(args[1]);
                int masterPort = Integer.parseInt(args[2]);
                masterTcpSocketAddress = new InetSocketAddress(masterAddress, masterPort);

                isMasterTrue = false;
                log("Next potential master acknowledged: " + masterTcpSocketAddress, LogType.In);
            }  break;

            case HEAD_RESPONSE_I_AM_MASTER: {
                masterTcpSocketAddress = connection.getRemoteSocketAddress();

                int masterUdpPort = Integer.parseInt(args[1]);
                if(masterUdpPort != NULL_PORT) {
                    masterUdpSocketAddress = new InetSocketAddress(masterTcpSocketAddress.getAddress(), masterUdpPort);
                    log("Friend is the master with UDP channel: " + masterUdpPort, LogType.In);
                }
                else {
                    masterUdpSocketAddress = null;
                    log("Friend is the master without UDP channel.", LogType.In);
                }

                isMasterTrue = true;
            }  break;

            case HEAD_RESPONSE_FAIL: {
                masterTcpSocketAddress = null;
                isMasterTrue = false;
                log("Friend does not know any master and has no friends. (Error)", LogType.In);
            }  break;
        }
    }


    private void requestRegistration(Connection connection) throws IOException
    {
        log("Requesting registration.", LogType.Out);

        StringBuilder msg = new StringBuilder();

        msg.append(REGISTRATION_REQUEST);
        msg.append(' ');
        msg.append(config.getIdentifier());
        msg.append(' ');
        msg.append(config.getHostingTcpPort());
        msg.append(' ');
        msg.append(config.getHostingUdpPort());

        for (Map.Entry<String,Integer> keyVal : config.getResourcesSpaces().entrySet()) {
            msg .append(" ")
                .append(keyVal.getKey())
                .append(":")
                .append(keyVal.getValue());
        }
        connection.send(msg.toString());

        String response = connection.receive();
        switch (response) {
            case REGISTRATION_RESPONSE_SUCCESS: {
                log("Master successfully registered me.", LogType.In);
                internalCommunication.registrationConfirmation.pass(true);
            }  break;

            case REGISTRATION_RESPONSE_DENY: {
                log("Master denied registration.", LogType.In);
            }  break;

            default: {
                log("Invalid response (" + response + ")", LogType.Problem);
            }  break;
        }
    }


    private void requestAllocation(Connection connection) throws IOException
    {
        AllocationRequest request = internalCommunication.allocationRequestInternalPass.getValue();

        log("Sending allocation request to the master", LogType.Out);
        connection.send(request.toString());

        log("Reading allocation results.", LogType.In);
        String totalResponse = ConnectionUtils.receiveMultiline(connection);

        log("Passing results to the server.", LogType.Info);
        internalCommunication.allocationResponseInternalPass.pass(totalResponse);
    }


    private void requestTermination(Connection connection) throws IOException
    {
        internalCommunication.terminationRequestInternalPass.getValue();

        log("Sending termination request to the master", LogType.Out);
        connection.send(TERMINATION_REQUEST);

        log("Reading termination results.", LogType.In);
        String response = ConnectionUtils.receiveMultiline(connection);

        log("Passing results to the server.", LogType.Info);
        internalCommunication.terminationResponseInternalPass.pass(response);
    }

    @Override
    protected String getLogPrefix() { return "Client >"; }
}
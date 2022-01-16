package rscnet.communication;

import rscnet.*;
import rscnet.data.*;
import rscnet.logging.*;
import rscnet.logic.*;
import rscnet.utils.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static rscnet.Constants.NetCommands.*;

@SuppressWarnings("PointlessBooleanExpression")
public class ServerPortHandler extends AbstractPortHandler {
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final NetworkStatus networkStatus;
    private final UnreliableConnectionFactory unreliableConnectionFactory;
    private final TerminationListener appTerminationRequestHandler;
    private ServerSocket serverSocket;

    public ServerPortHandler(
            AppConfig config,
            InternalCommunication internalCommunication,
            UnreliableConnectionFactory unreliableConnectionFactory,
            TerminationListener appTerminationRequestHandler,
            NetworkStatus networkStatus) {
        this.config = config;
        this.internalCommunication = internalCommunication;
        this.unreliableConnectionFactory = unreliableConnectionFactory;
        this.appTerminationRequestHandler = appTerminationRequestHandler;
        this.networkStatus = networkStatus;

        if(config.isMasterHost()){
            try {
                log("Registering the Master into slave registry.", LogType.Info);

                HostMetadata hostMetadata = new HostMetadata(
                    new InetSocketAddress(InetAddress.getByName("localhost"), config.getHostingPort()),
                    config.getIdentifier(),
                    config.getResourcesSpaces()
                );

                networkStatus.tryRegister(hostMetadata);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Connection openConnection() throws IOException {
        int timeout = 50;

        int hostingPort = config.getHostingPort();
        serverSocket = new ServerSocket(hostingPort);
        serverSocket.setSoTimeout(timeout);

        while (getKeepAlive() == true){
            try{
                Socket socket = serverSocket.accept();
                return new ReliableConnection(socket);
            }catch (SocketTimeoutException ignored){}

            if(unreliableConnectionFactory == null) {
                if(getKeepAlive()) continue;
                else return null;
            }

            Connection result2 = unreliableConnectionFactory.
                    acceptUnreliableConnectionOrNull(timeout);
            if(result2 != null) return result2;
        }

        return null;
    }

    @Override
    protected void useConnection(Connection connection) throws IOException {
        final String request = connection.receive();
        final String[] args = request.split(" ");

        switch (args[0]) {
            case HEAD_REQUEST: { handleHeadRequest(connection, request); }  break;
            case REGISTRATION_REQUEST: { handleRegistrationRequest(connection, request, args); }  break;
            case TERMINATION_REQUEST: { handleTerminationRequest(connection); }  break;
            case COLLAPSE_REQUEST: {  handleCollapseRequest(); }  break;
            default: { handleAllocationRequest(connection, request); }  break;
        }

        serverSocket.close();
    }


    private void handleHeadRequest(Connection connection, String request) throws IOException {
        log("Requested sign to master. (" + request + ")", LogType.In);

        StringBuilder str = new StringBuilder();
        if (config.isMasterHost()) {
            str.append(HEAD_RESPONSE_I_AM_MASTER);
            log("Responded it's me.", LogType.Out);
        } else {
            str.append(HEAD_RESPONSE_ABOUT_MASTER + " ")
                    .append(config.getGatewayAddress().getHostAddress())
                    .append(" ")
                    .append(config.getGatewayPort());
            log("Responded with address to someone else to ask.", LogType.Out);
        }

        connection.send(str.toString());
    }


    private void handleRegistrationRequest(Connection connection, String request, String[] args) throws IOException {
        log("Requested registration. (" + request + ")", LogType.In);
        int identifier = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress slaveSocketAddress = new InetSocketAddress(
                connection.getRemoteSocketAddress().getAddress(), port);

        HashMap<String,Integer> space = new HashMap<>(args.length - 2);
        for (int i = 3; i < args.length; i++) {
            String[] keyVal = args[i].split(":");
            String key = keyVal[0];
            int val = Integer.parseInt(keyVal[1]);
            space.put(key, val);
        }

        boolean pass = networkStatus.tryRegister(new HostMetadata(slaveSocketAddress, identifier, space));

        if(pass){
            connection.send(REGISTRATION_RESPONSE_SUCCESS);

            log("Accepted registration.", LogType.Out);
        }else{
            connection.send(REGISTRATION_RESPONSE_DENY);

            log("Denied.", LogType.Out);
        }
    }


    private void handleAllocationRequest(Connection connection, String request) throws IOException {
        AllocationRequest allocationsRequest = new AllocationRequest(request);

        if(config.isMasterHost())
        {
            log("Incoming (interpreted allocations) request: " + request, LogType.In);

            AllocationResults allocationBuilder = AllocationResults.tryAllocate(allocationsRequest, networkStatus);

            connection.send(allocationBuilder.toString());

            log("Sending results: \n" + allocationBuilder, LogType.Out);
        }else{
            log("Passing allocation request to client.", LogType.Info);

            internalCommunication.allocationRequestInternalPass.pass(allocationsRequest);

            ThreadBlocking.wait(() -> !internalCommunication.allocationResponseInternalPass.hasValue(), this);

            String response = ConnectionUtils.translateResponse(
                    internalCommunication.allocationResponseInternalPass.getValue());

            connection.send(response);

            log("Passing results: \n" + response, LogType.Out);
        }
    }


    private void handleTerminationRequest(Connection connection) throws IOException {
        log("Incoming termination request.", LogType.In);

        if(config.isMasterHost())
        {
            connection.send(getAllocInfo());
            internalCommunication.collapseNetworkInternalPass.pass(new Object());

            log("Termination not fully implemented. (Network Collapse Call passed)", LogType.Problem);
        }else{
            internalCommunication.terminationRequestInternalPass.pass(new Object());

            ThreadBlocking.wait(() -> internalCommunication.terminationResponseInternalPass.hasValue() == false, this);

            String response = ConnectionUtils.translateResponse(
                    internalCommunication.terminationResponseInternalPass.getValue());

            connection.send(response);

            log("Passing results: \n" + response, LogType.Out);
        }
    }


    private void handleCollapseRequest(){
        log("Network-Collapse call arrived. Executing...", LogType.In);
        appTerminationRequestHandler.terminate();
    }


    private String getAllocInfo(){
        StringBuilder sb = new StringBuilder();

        for (HostStatus host : networkStatus.getHosts()) {
            for (FullAllocRecord reg : host.getFullAllocInfo()) {

                sb.append(reg.resource);
                sb.append('.');
                sb.append(reg.quantity);
                sb.append('.');
                sb.append(host.getMetadata().getSocketAddress().getAddress().getHostAddress());
                sb.append('.');
                sb.append(host.getMetadata().getSocketAddress().getPort());
                sb.append('\n');
            }
        }

        return sb.toString();
    }


    @Override
    protected String getLogPrefix() { return "> Server"; }
}
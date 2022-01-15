package rscnet.communication;

import rscnet.*;
import rscnet.data.*;
import rscnet.logging.*;
import rscnet.logic.*;
import rscnet.utils.ThreadBlocking;

import java.io.*;
import java.net.*;
import java.util.*;

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
            TerminationListener appTerminationRequestHandler) {
        this.config = config;
        this.internalCommunication = internalCommunication;
        this.unreliableConnectionFactory = unreliableConnectionFactory;
        this.appTerminationRequestHandler = appTerminationRequestHandler;


        if(config.isMasterHost()){
            networkStatus = new NetworkStatus();
            try {
                log("Registrating the Master into slave registry.", LogType.Info);

                var hostMetadata = new HostMetadata(
                    new InetSocketAddress(InetAddress.getLocalHost(), config.getHostingPort()),
                    config.getIdentifier(),
                    config.getResourcesSpaces()
                );

                networkStatus.tryRegister(hostMetadata);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }else{
            networkStatus = null;
        }
    }

    @Override
    protected Connection openConnection() throws IOException {
        int timeout = 50;

        var hostingPort = config.getHostingPort();
        serverSocket = new ServerSocket(hostingPort);
        serverSocket.setSoTimeout(timeout);

        while (getKeepAlive() == true){
            try{
                var socket = serverSocket.accept();
                return new ReliableConnection(socket);
            }catch (SocketTimeoutException ignored){}

            if(unreliableConnectionFactory == null) continue;

            var result2 = unreliableConnectionFactory.
                    acceptUnreliableConnectionOrNull(timeout);
            if(result2 != null) return result2;
        }

        return null;
    }

    @Override
    protected void useConnection(Connection connection) throws IOException {
        final var request = connection.receive();
        final var args = request.split(" ");

        switch (args[0]) {
            case NetCommands.HeadRequest -> handleHeadRequest(connection, request);
            case NetCommands.RegistrationRequest -> handleRegistrationRequest(connection, request, args);
            case NetCommands.TerminationRequest -> handleTerminationRequest(connection);
            default -> handleAllocationRequest(connection, request);
        }

        serverSocket.close();
    }


    private void handleHeadRequest(Connection connection, String request) throws IOException {
        log("Requested sign to master. (" + request + ")", LogType.In);

        var str = new StringBuilder();
        if (config.isMasterHost()) {
            str.append(NetCommands.HeadResponseMeMaster);
            log("Responded it's me.", LogType.Out);
        } else {
            str.append(NetCommands.HeadResponseAboutMaster + " ")
                    .append(config.getGatewayAddress().getHostAddress())
                    .append(" ")
                    .append(config.getGatewayPort());
            log("Responded with address to someone else to ask.", LogType.Out);
        }

        connection.send(str.toString());
    }


    private void handleRegistrationRequest(Connection connection, String request, String[] args) throws IOException {
        log("Requested registration. (" + request + ")", LogType.In);
        var identifier = Integer.parseInt(args[1]);
        var slaveSocketAddress = connection.getRemoteSocketAddress();

        var space = new HashMap<String,Integer>(args.length - 2);
        for (int i = 2; i < args.length; i++) {
            var keyVal = args[i].split(":");
            var key = keyVal[0];
            var val = Integer.parseInt(keyVal[1]);
            space.put(key, val);
        }

        var pass = networkStatus.tryRegister(new HostMetadata(slaveSocketAddress, identifier, space));

        if(pass){
            connection.send(NetCommands.RegistrationResponseSuccess);

            log("Accepted registration.", LogType.Out);
        }else{
            connection.send(NetCommands.RegistrationResponseDeny);

            log("Denied.", LogType.Out);
        }
    }


    private void handleAllocationRequest(Connection connection, String request) throws IOException {
        var allocationsRequest = new AllocationRequest(request);

        if(config.isMasterHost())
        {
            log("Incoming (interpreted allocations) request: " + request, LogType.In);

            var allocationBuilder = AllocationResults.tryAllocate(allocationsRequest, networkStatus);

            connection.send(allocationBuilder.toString());

            log("Sending results: \n" + allocationBuilder, LogType.Out);
        }else{
            log("Passing allocation request to client.", LogType.Info);

            internalCommunication.allocationRequestInternalPass.pass(allocationsRequest);

            int waitCycles = 0;
            final int interval = 100;
            while (internalCommunication.allocationResponseInternalPass.hasValue() == false){
                sleep(interval);
                if((waitCycles++) % (1000 / interval) == 0) log("Waiting...", LogType.Info);
            }

            String response = internalCommunication.allocationResponseInternalPass.getValue();
            String responseFormat = response.replace(NetCommands.NewLineReplacer,"\n");

            connection.send(responseFormat);

            log("Passing results: \n" + responseFormat, LogType.Out);
        }
    }


    private void handleTerminationRequest(Connection connection) throws IOException {
        log("Incoming termination request.", LogType.In);

        if(config.isMasterHost())
        {
            connection.send(getAllocInfo());
            internalCommunication.collapseNetworkInternalPass.pass(new Object());

            log("Termination not fully implemented. (Network Collapse Call passed, but not handled)", LogType.Problem);
        }else{
            internalCommunication.terminationRequestInternalPass.pass(new Object());

            ThreadBlocking.wait(() -> internalCommunication.terminationResponseInternalPass.hasValue() == false, this);

            var response = internalCommunication.terminationResponseInternalPass.getValue();

            connection.send(response);
        }
    }


    private String getAllocInfo(){
        var sb = new StringBuilder();

        for (var host : networkStatus.getHosts()) {
            for (var reg : host.getFullAllocInfo()) {
                sb.append(host.getMetadata().getIdentifier());
                sb.append('.');
                sb.append(reg.toString());
                sb.append('\n');
            }
        }

        return sb.toString();
    }


    @Override
    protected String getLogPrefix() { return "> Server"; }
}
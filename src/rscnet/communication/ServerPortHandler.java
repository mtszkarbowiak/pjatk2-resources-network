package rscnet.communication;

import rscnet.*;
import rscnet.logic.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "> Server"; }

    private final int totalAllocationTimeout = 15_000; // 15 secs
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final SlaveRegistry slaveRegistry;
    private ServerSocket serverSocket;

    public ServerPortHandler(AppConfig config, InternalCommunication internalCommunication) {
        this.config = config;
        this.internalCommunication = internalCommunication;


        if(config.isMasterHost()){
            slaveRegistry = new SlaveRegistry();
            try {
                log("Registrating the Master into slave registry.", LogType.Info);
                slaveRegistry.tryRegister(
                        config.getIdentifier(),
                        new InetSocketAddress(InetAddress.getLocalHost(), config.getHostingPort()), config.getResourcesSpaces());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }else{
            slaveRegistry = null;
        }
    }

    @Override
    protected Socket openConnection() throws IOException {
        serverSocket = new ServerSocket(config.getHostingPort());
        return serverSocket.accept();
    }

    @Override
    protected void useConnection(BufferedReader reader, BufferedWriter writer, ConnectionInfo connectionInfo) throws IOException {
        final var request = reader.readLine();
        final var args = request.split(" ");

        switch (args[0]) {
            case NetCommands.HeadRequest -> handleHeadRequest(writer, request);
            case NetCommands.RegistrationRequest -> handleRegistrationRequest(writer, connectionInfo, request, args);
            default -> handleAllocationRequest(writer, connectionInfo, request, args);
        }

        serverSocket.close();
    }


    private void handleHeadRequest(BufferedWriter writer, String request) throws IOException {
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


    private void handleRegistrationRequest(BufferedWriter writer, ConnectionInfo connectionInfo, String request, String[] args) throws IOException {
        log("Requested registration. (" + request + ")", LogType.In);
        var identifier = Integer.parseInt(args[1]);
        var slaveSocketAddress = connectionInfo.getInetSocketAddress();

        var space = new HashMap<String,Integer>(args.length - 2);
        for (int i = 2; i < args.length; i++) {
            var keyVal = args[i].split(":");
            var key = keyVal[0];
            var val = Integer.parseInt(keyVal[1]);
            space.put(key, val);
        }

        var pass = slaveRegistry.tryRegister(identifier, slaveSocketAddress, space);

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


    private void handleAllocationRequest(BufferedWriter writer, ConnectionInfo connectionInfo, String request, String[] args) throws IOException {
        if(config.isMasterHost())
        {
            log("Incoming (interpreted allocations) request: " + request, LogType.In);

            var allocationsRequest = new AllocationRequest(request);
            AllocationResultsBuilder allocationResultsBuilder = new AllocationResultsBuilder();
            AllocationRequest rest = slaveRegistry.tryAllocate(allocationsRequest, allocationResultsBuilder);

            if(rest.getResources().size() > 0) allocationResultsBuilder.markFailed();

            writer.write(allocationResultsBuilder.toString());
            writer.flush();
            log("Sending results: \n" + allocationResultsBuilder, LogType.Out);
        }else{
            log("Handling allocation requests from sub-host/slave is not yet implemented.",LogType.Problem);
        }
    }
}
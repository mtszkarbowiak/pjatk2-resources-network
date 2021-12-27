package rscnet.communication;

import rscnet.*;
import rscnet.data.*;
import rscnet.logging.*;
import rscnet.logic.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "> Server"; }

    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final NetworkStatus networkStatus;
    private ServerSocket serverSocket;

    public ServerPortHandler(AppConfig config, InternalCommunication internalCommunication) {
        this.config = config;
        this.internalCommunication = internalCommunication;


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

        var pass = networkStatus.tryRegister(new HostMetadata(slaveSocketAddress, identifier, space));

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
        var allocationsRequest = new AllocationRequest(request);

        if(config.isMasterHost())
        {
            log("Incoming (interpreted allocations) request: " + request, LogType.In);

            var allocationBuilder = AllocationResults.tryAllocate(allocationsRequest, networkStatus);

            writer.write(allocationBuilder.toString());
            writer.flush();

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

            writer.write(responseFormat);
            writer.flush();

            log("Passing results: \n" + responseFormat, LogType.Out);
        }
    }
}
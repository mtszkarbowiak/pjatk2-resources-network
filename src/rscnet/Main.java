package rscnet;

import rscnet.communication.ClientPortHandler;
import rscnet.communication.ServerPortHandler;

import java.net.*;

public class Main
{
    public static void main(String[] args) throws UnknownHostException {
        Tests.internalTests(false);

        System.out.println("---* PROGRAM START *---");

        var config = new AppConfig(args);
        var masterHostMode = config.isMasterHost();

        System.out.println(config);
        System.out.println(masterHostMode ? "Running as MASTER" : "Running as SLAVE");
        System.out.println();

        if(masterHostMode)
            startAsMaster(config);
        else
            startAsSlave(config);
    }

    private static void startAsSlave(AppConfig config){
        var internalCommunication = new InternalCommunication(config);

        var clientPortHandler = new ClientPortHandler(config, internalCommunication);
        var clientPortThread = new Thread(clientPortHandler);

        var serverPortHandler = new ServerPortHandler(config, internalCommunication);
        var serverPortThread = new Thread(serverPortHandler);

        clientPortThread.start();
        serverPortThread.start();
    }

    private static void startAsMaster(AppConfig config){
        var internalCommunication = new InternalCommunication(config);

        var serverPortHandler = new ServerPortHandler(config, internalCommunication);
        var serverPortThread = new Thread(serverPortHandler);
        serverPortThread.start();
    }
}
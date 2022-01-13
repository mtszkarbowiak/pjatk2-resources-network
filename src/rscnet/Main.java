package rscnet;

import rscnet.communication.ClientPortHandler;
import rscnet.communication.ServerPortHandler;
import rscnet.communication.UnreliableConnectionFactory;
import rscnet.data.AppConfig;

import java.net.*;

public class Main
{
    public static final boolean USE_UNRELIABLE_CONNECTION = true;

    public static void main(String[] args) throws UnknownHostException, SocketException {
        Tests.internalTests(false);

        System.out.println("---* PROGRAM START *---");

        var config = new AppConfig(args);
        var masterHostMode = config.isMasterHost();

        System.out.println(config);
        System.out.println(masterHostMode ? "Running as MASTER" : "Running as SLAVE");
        System.out.println();

        UnreliableConnectionFactory unreliableServerClient = null;

        if(USE_UNRELIABLE_CONNECTION){
            unreliableServerClient = new UnreliableConnectionFactory(50, config.getHostingPort() + 100);
            var unreliableServerClientThread = new Thread(unreliableServerClient);
            unreliableServerClientThread.start();
        }

        if(masterHostMode)
            startAsMaster(config, unreliableServerClient);
        else
            startAsSlave(config, unreliableServerClient);
    }

    private static void startAsSlave(AppConfig config, UnreliableConnectionFactory unreliableConnectionFactory) {
        var internalCommunication = new InternalCommunication();

        var clientPortHandler = new ClientPortHandler(config, internalCommunication, unreliableConnectionFactory);
        var clientPortThread = new Thread(clientPortHandler);

        var serverPortHandler = new ServerPortHandler(config, internalCommunication, unreliableConnectionFactory);
        var serverPortThread = new Thread(serverPortHandler);

        clientPortThread.start();
        serverPortThread.start();
    }

    private static void startAsMaster(AppConfig config, UnreliableConnectionFactory unreliableConnectionFactory){
        var internalCommunication = new InternalCommunication();

        var serverPortHandler = new ServerPortHandler(config, internalCommunication, unreliableConnectionFactory);
        var serverPortThread = new Thread(serverPortHandler);

        serverPortThread.start();
    }
}
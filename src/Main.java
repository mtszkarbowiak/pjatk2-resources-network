import java.net.*;

public class Main
{
    public static void main(String[] args) throws UnknownHostException {
        Tests.internalTests();

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
        var clientPortHandler = new ClientPortHandler(config);
        var clientPortThread = new Thread(clientPortHandler);
        clientPortThread.start();

        var serverPortHandler = ServerPortHandler.createSlaveServerPortHandler(config);
        var serverPortThread = new Thread(serverPortHandler);
        serverPortThread.start();
    }

    private static void startAsMaster(AppConfig config){
        var slaveRegistry = new SlaveRegistry();

        var serverPortHandler = ServerPortHandler.createMasterServerPortHandler(config, slaveRegistry);
        var serverPortThread = new Thread(serverPortHandler);
        serverPortThread.start();
    }
}
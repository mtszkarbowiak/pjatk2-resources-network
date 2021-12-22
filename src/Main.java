import java.net.*;

public class Main
{
    public static void main(String[] args) throws UnknownHostException {
        Tests.internalTests();

        System.out.println("---* PROGRAM START *---");

        var config = new AppConfig(args);
        var resourceRegistry = new ResourceRegistry(config.getResourcesSpaces());
        var masterHostMode = config.isMasterHost();

        System.out.println(config);
        System.out.println("MainHostMode: " + masterHostMode);
        System.out.println();
        System.out.println("Starting handling threads.");


        if(masterHostMode == false) {
            var clientPortHandler = new FreshClientPortHandler(
                    config.getGatewayAddress(),
                    config.getGatewayPort()
            );
            var clientPortThread = new Thread(clientPortHandler);
            clientPortThread.start();
        }

        var serverPortHandler = new ServerPortHandler(
                config
        );
        var serverPortThread = new Thread(serverPortHandler);
        serverPortThread.start();
    }
}
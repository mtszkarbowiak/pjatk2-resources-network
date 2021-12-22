import java.net.*;

public class Main
{
    public static void main(String[] args) throws UnknownHostException {
        Tests.internalTests();

        System.out.println("---* PROGRAM START *---");

        var config = new AppConfig(args);
        var resourceRegistry = new ResourceRegistry(config.getResourcesSpaces());
        var mainHostMode = config.isMainHost();

        System.out.println(config);
        System.out.println("MainHostMode: " + mainHostMode);
        System.out.println();
        System.out.println("Starting handling threads.");


        if(mainHostMode == false) {
            var clientPortHandler = new ClientPortHandler(
                    config.getGatewayAddress(),
                    config.getGatewayPort()
            );
            var clientPortThread = new Thread(clientPortHandler);
            clientPortThread.start();
        }

        var serverPortHandler = new ServerPortHandler(
                config.getHostingPort()
        );
        var serverPortThread = new Thread(serverPortHandler);
        serverPortThread.start();
    }
}
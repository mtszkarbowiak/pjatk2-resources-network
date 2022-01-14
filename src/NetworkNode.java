import rscnet.*;
import rscnet.communication.*;
import rscnet.data.AppConfig;

import java.net.*;
import java.util.ArrayList;

public class NetworkNode
{
    public static final boolean USE_UNRELIABLE_CONNECTION = true;
    public static final int COMPILATION_NO = 12;

    private static boolean keepAlive = true;
    private static void terminateApp(){ keepAlive = false; }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println("---* PROGRAM START (" + COMPILATION_NO+ ") *---");


        // Configuration

        var config = new AppConfig(args);
        var masterHostMode = config.isMasterHost();

        var terminationListeners = new ArrayList<TerminationListener>();
        var appTerminator = new TerminationListener(){
            @Override public void terminate() { terminateApp();  }
        };

        System.out.println(config);
        System.out.println(masterHostMode ? "Running as MASTER" : "Running as SLAVE");
        System.out.println(USE_UNRELIABLE_CONNECTION ? "UDP communication is turned ON." : "UDP communication is turned OFF.");
        System.out.println();


        // Start Communication

        UnreliableConnectionFactory unreliableConnectionFactory = null;
        if(USE_UNRELIABLE_CONNECTION){
            unreliableConnectionFactory = new UnreliableConnectionFactory(50, config.getHostingPort() + 100);
            terminationListeners.add(unreliableConnectionFactory);
            var unreliableServerClientThread = new Thread(unreliableConnectionFactory);
            unreliableServerClientThread.start();
        }

        var internalCommunication = new InternalCommunication();

        if (!masterHostMode) {
            var clientPortHandler = new ClientPortHandler(config, internalCommunication, unreliableConnectionFactory);
            var clientPortThread = new Thread(clientPortHandler);

            terminationListeners.add(clientPortHandler);
            clientPortThread.start();
        }

        var serverPortHandler = new ServerPortHandler(config, internalCommunication, unreliableConnectionFactory);
        var serverPortThread = new Thread(serverPortHandler);

        terminationListeners.add(serverPortHandler);
        serverPortThread.start();



        // Keep Application Alive

        while (keepAlive){
            try{
                //noinspection BusyWait
                Thread.sleep(50);
            }catch (InterruptedException ignored){}
        }

        for (var terminable : terminationListeners) {
            System.out.println("Terminating " + terminable.getClass().getName());
            terminable.terminate();
        }

        System.out.println("---* PROGRAM TERMINATED *---");
    }
}
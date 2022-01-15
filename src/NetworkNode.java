import rscnet.*;
import rscnet.communication.*;
import rscnet.data.AppConfig;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class NetworkNode
{
    public static final boolean USE_UNRELIABLE_CONNECTION = true;
    public static final int COMPILATION_NO = 13;
    public static final int MAX_APP_LIFETIME = 10000;

    private static boolean keepAlive = true;
    private static void terminateApp(){ keepAlive = false; }

    public static void main(String[] args) throws IOException {
        try{
            run(args);
        }catch (Exception ex){
            System.out.println("---* RUN INTERRUPTED *---");
            ex.printStackTrace();
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        }
    }

    public static void run(String[] args) throws UnknownHostException, SocketException{
        System.out.println("---* PROGRAM START (" + COMPILATION_NO+ ") *---");


        // Configuration

        var config = new AppConfig(args);
        var masterHostMode = config.isMasterHost();

        var terminationListeners = new ArrayList<TerminationListener>();
        var appTerminationRequestHandler = new TerminationListener(){
            @Override public void terminate() { terminateApp();  }
        };

        System.out.println(config);
        System.out.println(masterHostMode ? "Running as MASTER" : "Running as SLAVE");
        System.out.println(USE_UNRELIABLE_CONNECTION ? "UDP communication is turned ON." : "UDP communication is turned OFF.");
        System.out.println("Max Lifetime: " + MAX_APP_LIFETIME + " ms");
        System.out.println();


        // Start Communication

        UnreliableConnectionFactory unreliableConnectionFactory = null;
        Thread serverThread, unreliableConnectionsThread = null, clientThread = null;

        if(USE_UNRELIABLE_CONNECTION){
            unreliableConnectionFactory = new UnreliableConnectionFactory(50, config.getHostingPort() + 100);
            terminationListeners.add(unreliableConnectionFactory);
            unreliableConnectionsThread = new Thread(unreliableConnectionFactory);
            unreliableConnectionsThread.start();
        }

        var internalCommunication = new InternalCommunication();

        if (!masterHostMode) {
            var clientPortHandler = new ClientPortHandler(
                    config, internalCommunication, unreliableConnectionFactory);
            clientThread = new Thread(clientPortHandler);

            terminationListeners.add(clientPortHandler);
            clientThread.start();
        }

        var serverPortHandler = new ServerPortHandler(
                config, internalCommunication,
                unreliableConnectionFactory, appTerminationRequestHandler);
        serverThread = new Thread(serverPortHandler);

        terminationListeners.add(serverPortHandler);
        serverThread.start();



        // Keep Application Alive

        final int mainStep = 50;
        for (int t = 0; t < MAX_APP_LIFETIME && keepAlive; t+=mainStep) {
            try{
                //noinspection BusyWait
                Thread.sleep(mainStep);
            }catch (InterruptedException ignored){}
        }

        for (var terminable : terminationListeners) {
            System.out.println("Terminating " + terminable.getClass().getName());
            terminable.terminate();
        }

        try {
            serverThread.join();

            if(clientThread != null)
                clientThread.join();

            if(unreliableConnectionsThread != null)
                unreliableConnectionsThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("---* PROGRAM TERMINATED *---");
    }
}
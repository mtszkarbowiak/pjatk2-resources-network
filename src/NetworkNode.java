import rscnet.*;
import rscnet.communication.*;
import rscnet.data.*;
import rscnet.logic.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static rscnet.Constants.App.*;
import static rscnet.Constants.Async.*;

public class NetworkNode
{
    private static boolean keepAlive = true;
    private static void terminateApp(){
        keepAlive = false;
    }

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

        AppConfig config = new AppConfig(args);
        boolean masterHostMode = config.isMasterHost();

        ArrayList<TerminationListener> terminationListeners = new ArrayList<>();
        TerminationListener appTerminationRequestHandler = NetworkNode::terminateApp;

        System.out.println(config);
        System.out.println(masterHostMode ? "Running as MASTER" : "Running as SLAVE");
        System.out.println(USE_UNRELIABLE_CONNECTION ? "UDP communication is turned ON." : "UDP communication is turned OFF.");
        System.out.println("Max Lifetime: " + MAX_APP_LIFETIME + " ms");
        System.out.println();


        // Start Communication

        UnreliableConnectionFactory unreliableConnectionFactory = null;
        Thread serverThread, clientThread, unreliableConnectionsThread = null;
        NetworkStatus networkStatus = null;
        InternalCommunication internalCommunication = new InternalCommunication();

        if(USE_UNRELIABLE_CONNECTION){
            unreliableConnectionFactory = new UnreliableConnectionFactory(50, config.getHostingPort() + 100);
            terminationListeners.add(unreliableConnectionFactory);

            unreliableConnectionsThread = new Thread(unreliableConnectionFactory);
            unreliableConnectionsThread.setName("UnreliableChannel");
            unreliableConnectionsThread.start();
        }

        if (!masterHostMode) {
            ClientSubHostPortHandler clientPortHandler = new ClientSubHostPortHandler(
                    config, internalCommunication, unreliableConnectionFactory);
            clientThread = new Thread(clientPortHandler);
            terminationListeners.add(clientPortHandler);
        }else{
            networkStatus = new NetworkStatus();

            ClientMasterPortHandler clientPortHandler = new ClientMasterPortHandler(
                    config, internalCommunication, unreliableConnectionFactory,
                    networkStatus, appTerminationRequestHandler);
            clientThread = new Thread(clientPortHandler);
            terminationListeners.add(clientPortHandler);
        }
        clientThread.setName("Client");
        clientThread.start();

        ServerPortHandler serverPortHandler = new ServerPortHandler(
                config, internalCommunication,
                unreliableConnectionFactory, appTerminationRequestHandler,
                networkStatus);
        serverThread = new Thread(serverPortHandler);

        terminationListeners.add(serverPortHandler);
        serverThread.setName("Server");
        serverThread.start();



        // Keep Application Alive

        for (int t = 0; t < MAX_APP_LIFETIME && keepAlive; t+= THREAD_ASYNC_SLEEP_STEP) {
            try{
                //noinspection BusyWait
                Thread.sleep(THREAD_ASYNC_SLEEP_STEP);
            }catch (InterruptedException ignored){}
        }

        System.out.println("---* TERMINATING *---");

        for (TerminationListener terminable : terminationListeners) {
            System.out.println("Terminating " + terminable.getClass().getName());
            terminable.terminate();
        }

        try {
            serverThread.join();
            clientThread.join();

            if(unreliableConnectionsThread != null)
                unreliableConnectionsThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("---* PROGRAM TERMINATED *---");

        if(WAIT_AFTER_TERMINATION){
            try {
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
            } catch (IOException ignored) {}
        }
    }
}
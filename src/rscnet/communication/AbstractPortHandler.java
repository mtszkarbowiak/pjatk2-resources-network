package rscnet.communication;

import java.io.*;
import java.net.*;

public abstract class AbstractPortHandler implements Runnable {
    private boolean keepAlive = true;

    protected abstract Socket openConnection() throws IOException;
    protected abstract void useConnection(BufferedReader reader, BufferedWriter writer, ConnectionInfo connectionInfo) throws IOException;
    protected abstract String getLogPrefix();

    @Override
    public void run() {
        log("Starting loop. Init interval: " + getLoopInterval() + "ms", LogType.Config);

        while (keepAlive){
            try{
                final var socket = openConnection();
                final var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                final var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                useConnection(reader, writer, new ConnectionInfo(socket));
                socket.close();

                sleep(getLoopInterval());
            }
            catch (IOException exception){
                log("Error stopped update:", LogType.Problem);
                exception.printStackTrace();

                sleep(getReconnectionInterval());
            }
        }

        log("Loop terminated.", LogType.Config);
    }

    protected void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void log(String s, LogType logType){
        System.out.println("[" + getLogPrefix() + "] " + logType.getSymbol() + " " + s);
    }

    protected int getLoopInterval(){
        return 50;
    }

    protected int getReconnectionInterval(){
        return 2500;
    }

    public void stop(){
        keepAlive = false;
    }
}

class ConnectionInfo{
    private Socket socket;

    public ConnectionInfo(Socket socket) {
        this.socket = socket;
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public InetAddress getInetAddress(){
        return socket.getInetAddress();
    }

    public InetSocketAddress getInetSocketAddress(){
        return new InetSocketAddress(getInetAddress(), socket.getPort());
    }
}

enum LogType{
    Info("(i)"),
    Config("[#]"),
    In("* <-"),
    Out("* ->"),
    Problem("/!\\");

    String symbol;

    public String getSymbol(){
        return symbol;
    }

    LogType(String symbol) {
        this.symbol = symbol;
    }
}
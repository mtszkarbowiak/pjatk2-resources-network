package rscnet.communication;

import rscnet.logging.LogType;
import rscnet.logging.Logger;

import java.io.*;
import java.net.*;

public abstract class AbstractPortHandler extends Logger implements Runnable {
    private boolean keepAlive = true;

    protected abstract Connection openConnection() throws IOException;
    protected abstract void useConnection(Connection connection) throws IOException;


    @Override
    public void run() {
        log("Starting loop. Init interval: " + getLoopInterval() + "ms", LogType.Config);

        while (keepAlive){
            try{
                final var connection = openConnection();
                useConnection(connection);
                connection.close();

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
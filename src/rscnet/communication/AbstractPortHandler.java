package rscnet.communication;

import rscnet.TerminationListener;
import rscnet.logging.LogType;
import rscnet.logging.Logger;

import java.io.*;

import static rscnet.Constants.Communication.*;

public abstract class AbstractPortHandler extends Logger implements Runnable, TerminationListener {
    private boolean keepAlive = true;

    protected abstract Connection openConnection() throws IOException;
    protected abstract void useConnection(Connection connection) throws IOException;


    @Override
    public void run() {
        log("Starting loop.", LogType.Config);

        while (keepAlive){
            try{
                final var connection = openConnection();
                if(connection == null) continue;

                useConnection(connection);
                connection.close();
            }
            catch (IOException exception){
                log("Error stopped update:", LogType.Problem);
                exception.printStackTrace();

                sleep(RECONNECTION_INTERVAL);
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

    @Override
    public void terminate(){
        keepAlive = false;
    }

    protected boolean getKeepAlive(){
        return keepAlive;
    }
}
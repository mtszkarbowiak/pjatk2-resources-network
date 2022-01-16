package rscnet.communication;

import rscnet.TerminationListener;
import rscnet.logging.*;

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
            log("Update: " + getClass().getName(), LogType.Info);
            try{
                final Connection connection = openConnection();
                if(connection != null)
                {
                    useConnection(connection);
                    connection.close();
                }
            }
            catch (UnreliableConnectionTerminatedException exception)
            {
                log("Connections dumped - ending loop.", LogType.Info);
            }
            catch (IOException exception){
                log("Error stopped an update:", LogType.Problem);
                exception.printStackTrace();

                sleep();
            }
        }

        String type = getClass().getName();
        log("Loop ended: " + type, LogType.Config);
    }

    protected void sleep(){
        try {
            Thread.sleep(RECONNECTION_INTERVAL);
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
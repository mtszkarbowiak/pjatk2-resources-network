import java.io.IOException;

public abstract class AbstractPortHandler implements Runnable {
    protected void log(String s){
        System.out.println("[" + getLogPrefix() + "] " + s);
    }

    @Override
    public void run() {
        log("Starting loop.");

        while (keepAlive()){
            try{
                while (keepAlive()){
                    update();
                    Thread.sleep(100);
                }
            }catch (IOException | InterruptedException exception){
                log("Error stopped update:");
                exception.printStackTrace();
            }
        }

        log("Loop halted. Halting the handler.");
        onHalted();
        log("Handler halted. Stopping the thread.");
    }

    protected abstract void update() throws IOException;
    protected abstract String getLogPrefix();
    protected abstract void onHalted();
    protected abstract boolean keepAlive();
}

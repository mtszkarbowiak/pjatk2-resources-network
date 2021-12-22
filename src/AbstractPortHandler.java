import java.io.IOException;

public abstract class AbstractPortHandler implements Runnable {
    protected void log(String s, LogType logType){
        System.out.println("[" + getLogPrefix() + "] " + logType.getSymbol() + " " + s
        );
    }

    @Override
    public void run() {
        log("Starting loop.", LogType.Info);

        while (keepAlive()){
            try{
                while (keepAlive()){
                    update();
                    Thread.sleep(100);
                }
            }catch (IOException | InterruptedException exception){
                log("Error stopped update:", LogType.Problem);
                exception.printStackTrace();
            }
        }

        log("Loop halted. Halting the handler.", LogType.Info);
        onHalted();
        log("Handler halted. Stopping the thread.", LogType.Info);
    }

    protected abstract void update() throws IOException;
    protected abstract String getLogPrefix();
    protected abstract void onHalted();
    protected abstract boolean keepAlive();
}

enum LogType{
    Info("(i)"),
    Config("***"),
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
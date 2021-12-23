import java.io.IOException;

public abstract class AbstractPortHandler implements Runnable {
    protected void log(String s, LogType logType){
        System.out.println("[" + getLogPrefix() + "] " + logType.getSymbol() + " " + s
        );
    }

    @Override
    public void run() {
        log("Starting loop.", LogType.Config);

        while (keepAlive()){
            try{
                while (keepAlive()){
                    update();
                }
            }catch (IOException exception){
                log("Error stopped update:", LogType.Problem);
                exception.printStackTrace();
                sleep(1000);
            }
            sleep(500);
        }

        log("Loop halted. Halting the handler.", LogType.Config);
        onHalted();
        log("Handler halted. Stopping the thread.", LogType.Config);
    }

    protected void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected abstract void update() throws IOException;
    protected abstract String getLogPrefix();
    protected abstract void onHalted();
    protected abstract boolean keepAlive();
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
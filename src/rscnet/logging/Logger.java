package rscnet.logging;

public abstract class Logger {
    protected abstract String getLogPrefix();

    public void log(String s, LogType logType){
        System.out.println("[" + getLogPrefix() + "] " + logType.getSymbol() + " " + s);
    }
}
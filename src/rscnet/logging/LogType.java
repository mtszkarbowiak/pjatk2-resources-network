package rscnet.logging;

public enum LogType {
    Info("(i)"),
    Config("[#]"),
    In("* <-"),
    Out("* ->"),
    Problem("/!\\");

    final String symbol;

    public String getSymbol() {
        return symbol;
    }

    LogType(String symbol) {
        this.symbol = symbol;
    }
}

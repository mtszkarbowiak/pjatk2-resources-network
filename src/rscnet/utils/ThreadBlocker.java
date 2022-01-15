package rscnet.utils;

@FunctionalInterface
public interface ThreadBlocker {
    boolean keepBlocking();
}

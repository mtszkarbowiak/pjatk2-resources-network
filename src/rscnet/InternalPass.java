package rscnet;

import java.util.UUID;

public class InternalPass<T> {
    private T value;
    private boolean hasValue;
    private UUID passIdentifier;

    private final boolean nullifyArgumentAfterGetting;


    public InternalPass(boolean nullifyArgumentAfterGetting, T value){
        this(nullifyArgumentAfterGetting);
        this.value = value;
        this.hasValue = true;
    }

    public InternalPass(boolean nullifyArgumentAfterGetting) {
        this.nullifyArgumentAfterGetting = nullifyArgumentAfterGetting;
        this.hasValue = false;
    }

    public synchronized void pass(
            T allocationsRequest) {
        this.hasValue = true;
        this.value = allocationsRequest;
        this.passIdentifier = UUID.randomUUID();
    }

    public synchronized boolean hasValue(){
        return hasValue;
    }

    public synchronized T getValue() {
        if(nullifyArgumentAfterGetting)
            hasValue = false;

        return value;
    }

    public synchronized UUID getPassIdentifier(){
        return passIdentifier;
    }
}

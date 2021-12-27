package rscnet;

import rscnet.data.*;
import rscnet.logic.AllocationResults;

import java.util.UUID;

public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;


    public InternalCommunication(AppConfig config) {
        this.config = config;
    }

    public synchronized void markRegistered(){
        isRegistered = true;
    }

    public synchronized boolean isRegistered(){
        return isRegistered;
    }


    public final InternalPass<AllocationRequest> allocationRequestInternalPass = new InternalPass<>(true);
    public final InternalPass<String> allocationResponseInternalPass = new InternalPass<>(true);
}
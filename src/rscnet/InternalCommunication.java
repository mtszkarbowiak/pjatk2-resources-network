package rscnet;

import rscnet.data.*;

public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;

    private AllocationRequest allocationRequest;
    private String allocationRequestResponse;


    public InternalCommunication(AppConfig config) {
        this.config = config;
    }

    public synchronized void markRegistered(){
        isRegistered = true;
    }

    public synchronized boolean isRegistered(){
        return isRegistered;
    }



    public synchronized void passAllocationRequest(
            AllocationRequest allocationsRequest) {
        this.allocationRequest = allocationsRequest;
    }

    public synchronized boolean hasAllocationRequest(){
        return allocationRequest != null;
    }

    public synchronized AllocationRequest getAllocationRequest() {
        var result = allocationRequest;
        allocationRequest = null;
        return result;
    }

    public synchronized void passAllocationRequestResponse(
            String allocationRequestResponse){
        this.allocationRequestResponse = allocationRequestResponse;
    }

    public synchronized boolean hasAllocationRequestResponse() {
        return allocationRequestResponse != null;
    }

    public synchronized String getAllocationRequestResponse() {
        var result = allocationRequestResponse;
        allocationRequest = null;
        return result;
    }
}
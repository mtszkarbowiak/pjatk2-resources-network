package rscnet;

import rscnet.data.*;

public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;

    private AllocationRequest allocationRequest;
    private boolean nullifyAllocationRequest = true;
    private String allocationRequestResponse;
    private boolean nullifyAllocationRequestResponse = true;


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
        nullifyAllocationRequest = false;
        this.allocationRequest = allocationsRequest;
    }

    public synchronized boolean hasAllocationRequest(){
        return nullifyAllocationRequest == false;
    }

    public synchronized AllocationRequest getAllocationRequest() {
        nullifyAllocationRequest = true;
        return allocationRequest;
    }


    public synchronized void passAllocationRequestResponse(
            String allocationRequestResponse){
        nullifyAllocationRequestResponse = false;
        this.allocationRequestResponse = allocationRequestResponse;
    }

    public synchronized boolean hasAllocationRequestResponse() {
        return nullifyAllocationRequestResponse == false;
    }

    public synchronized String getAllocationRequestResponse() {
        nullifyAllocationRequestResponse = true;
        return allocationRequestResponse;
    }
}
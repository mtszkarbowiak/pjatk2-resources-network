import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;

    private Queue<AllocationRequest> pendingAllocationRequests;

    public InternalCommunication(AppConfig config) {
        this.config = config;
        this.pendingAllocationRequests = new ConcurrentLinkedQueue<>();
    }

    public void markRegistered(){
        isRegistered = true;
    }

    public void passAllocationRequest(AllocationRequest allocationRequest){
        pendingAllocationRequests.add(allocationRequest);
    }

    public boolean hasNextAllocationRequest(){
        return pendingAllocationRequests.isEmpty() == false;
    }

    public boolean isRegistered(){
        return isRegistered;
    }

    public boolean isIdle(){
        return isRegistered && hasNextAllocationRequest() == false;
    }
}
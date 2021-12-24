import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;

    public final Queue<AllocationRequest> pendingAllocationRequests;

    public InternalCommunication(AppConfig config) {
        this.config = config;
        this.pendingAllocationRequests = new ConcurrentLinkedQueue<>();
    }

    public void markRegistered(){
        isRegistered = true;
    }


    public boolean isRegistered(){
        return isRegistered;
    }

    public boolean isIdle(){
        return isRegistered && pendingAllocationRequests.isEmpty();
    }
}
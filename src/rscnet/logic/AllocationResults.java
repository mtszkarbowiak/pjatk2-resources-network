package rscnet.logic;

import rscnet.data.AllocationRequest;

import java.net.InetSocketAddress;
import java.util.*;

public class AllocationResults {
    private final AllocationRequest request;
    private final Map<String, Integer> pending;
    private final StringBuilder logger;

    public static AllocationResults allocate(AllocationRequest request, NetworkStatus networkStatus){
        var result = new AllocationResults(request);

        result.allocate(networkStatus);

        return result;
    }

    private AllocationResults(AllocationRequest request) {
        this.logger = new StringBuilder();
        this.request = request;
        this.pending = new HashMap<>();

        for (var resource : request.getDemandedResources()) {
            var quantity = request.getDemandedQuantity(resource);
            pending.put(resource, quantity);
        }

        removeZeros();
    }

    private void allocate(NetworkStatus network){
        for (var host : network.getHosts()) {
            allocate(host);
        }
    }

    private void allocate(HostStatus host){
        for (var rscName : pending.keySet()) {
            var amount = pending.get(rscName);
            var alloced = host.tryAllocate(rscName, request.getClientIdentifier(), amount);

            pending.replace(rscName, amount - alloced);

            logger.append(rscName);
            logger.append(':');
            logger.append(alloced);
            logger.append(':');

            var address = host.getMetadata().getSocketAddress();
            logger.append(address.getAddress().getHostName());
            logger.append(':');
            logger.append(address.getPort());
            logger.append('\n');
        }

        removeZeros();
    }

    private void removeZeros(){
        pending.entrySet().removeIf(entry -> entry.getValue().equals(0));
    }

    public boolean isEmpty(){
        return pending.isEmpty();
    }

    @Override
    public String toString() {
        return isEmpty() ? logger.toString() : "FAILED";
    }
}
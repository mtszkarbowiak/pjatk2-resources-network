package rscnet.logic;

import rscnet.data.AllocationRequest;

import java.util.*;

public class AllocationResults {
    private final AllocationRequest request;
    private final Map<String, Integer> pending;
    private final StringBuilder logger;

    public static AllocationResults tryAllocate(AllocationRequest request, NetworkStatus networkStatus){

        boolean requestPossible = true;
        for (var name : request.getDemandedResources()) {
            var quantity = request.getDemandedQuantity(name);
            var freeSpace = networkStatus.getFreeResourceSpace(name);

            System.out.println("Req: " + name + "Q: " + quantity + " S: " + freeSpace);

            if(freeSpace < quantity){
                requestPossible = false;
                break;
            }
        }

        if(requestPossible)
        {
            var logger = new StringBuilder();
            var pending = new HashMap<String, Integer>();
            var result = new AllocationResults(request, pending, logger);

            for (var resource : request.getDemandedResources()) {
                var quantity = request.getDemandedQuantity(resource);
                pending.put(resource, quantity);
            }

            result.removeZeros();
            result.allocate(networkStatus);

            return result;
        }
        else
        {
            return new AllocationResults(request, new HashMap<>(), new StringBuilder("FAILED"));
        }
    }

    public AllocationResults(AllocationRequest request, Map<String, Integer> pending, StringBuilder logger) {
        this.request = request;
        this.pending = pending;
        this.logger = logger;
    }

    private void allocate(NetworkStatus network){
        for (var host : network.getHosts()) {
            allocate(host);
        }
    }

    private void allocate(HostStatus host){
        for (var rscName : pending.keySet()) {
            var amount = pending.get(rscName);
            var alloced = host.allocate(rscName, request.getClientIdentifier(), amount);

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

    @Override
    public String toString() {
        return logger.toString();
    }
}
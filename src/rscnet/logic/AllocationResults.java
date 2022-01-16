package rscnet.logic;

import rscnet.data.AllocationRequest;

import java.net.InetSocketAddress;
import java.util.*;

public class AllocationResults {
    private final AllocationRequest request;
    private final Map<String, Integer> pending;
    private final StringBuilder logger;

    public static AllocationResults tryAllocate(AllocationRequest request, NetworkStatus networkStatus){

        boolean requestPossible = true;
        for (String name : request.getDemandedResources()) {
            int quantity = request.getDemandedQuantity(name);
            int freeSpace = networkStatus.getFreeResourceSpace(name);

            System.out.println("Req: " + name + "Q: " + quantity + " S: " + freeSpace);

            if(freeSpace < quantity){
                requestPossible = false;
                break;
            }
        }

        if(requestPossible)
        {
            StringBuilder logger = new StringBuilder();
            HashMap<String, Integer> pending = new HashMap<>();
            AllocationResults result = new AllocationResults(request, pending, logger);

            for (String resource : request.getDemandedResources()) {
                int quantity = request.getDemandedQuantity(resource);
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
        for (HostStatus host : network.getHosts()) {
            allocate(host);
        }
    }

    private void allocate(HostStatus host){
        for (String rscName : pending.keySet()) {
            Integer amount = pending.get(rscName);
            int allocated = host.allocate(rscName, request.getClientIdentifier(), amount);

            pending.replace(rscName, amount - allocated);

            if(allocated == 0) continue;

            logger.append(rscName);
            logger.append(':');
            logger.append(allocated);
            logger.append(':');

            InetSocketAddress address = host.getMetadata().getSocketAddress();
            logger.append(address.getAddress().getHostAddress());
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
package rscnet.logic;

import java.util.*;

public class NetworkStatus {
    private final Set<HostStatus> hosts;

    public NetworkStatus() {
        this.hosts = new HashSet<>(128);
    }

    public boolean tryRegister(HostMetadata newHostMetadata){
        for (HostStatus host : hosts) {
            HostMetadata metaData = host.getMetadata();
            if(metaData.getIdentifier() == newHostMetadata.getIdentifier())
                return false;
        }

        HostStatus newStatus = new HostStatus(newHostMetadata);
        hosts.add(newStatus);

        return true;
    }

    public Set<HostStatus> getHosts() {
        return hosts;
    }

    public int getFreeResourceSpace(String rscName){
        int result = 0;

        for (HostStatus host : hosts) {
            result += host.getFreeResourceSpace(rscName);
        }

        return result;
    }
}

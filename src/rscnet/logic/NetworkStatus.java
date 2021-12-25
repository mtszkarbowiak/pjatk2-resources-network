package rscnet.logic;

import java.util.*;

public class NetworkStatus {
    private final Set<HostStatus> hosts;

    public NetworkStatus() {
        this.hosts = new HashSet<>(128);
    }

    public boolean tryRegister(HostMetadata newHostMetadata){
        for (var host : hosts) {
            var metaData = host.getMetadata();
            if(metaData.getIdentifier() == newHostMetadata.getIdentifier())
                return false;
        }

        var newStatus = new HostStatus(newHostMetadata);
        hosts.add(newStatus);

        return true;
    }

    public Set<HostStatus> getHosts() {
        return hosts;
    }
}

package rscnet.logic;

import java.net.*;
import java.util.*;

public class HostMetadata {
    private final InetSocketAddress socketAddress;
    private final int identifier;
    private final Map<String, Integer> space;

    public HostMetadata(
            InetSocketAddress socketAddress,
            int identifier,
            Map<String, Integer> space) {
        this.socketAddress = socketAddress;
        this.identifier = identifier;
        this.space = space;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public int getIdentifier() {
        return identifier;
    }

    public int getResourceSpace(String rscName){
        return space.getOrDefault(rscName, 0);
    }

    public Set<String> getStoredResourcesNames(){
        return space.keySet();
    }
}
package rscnet.logic;

import java.net.*;
import java.util.*;

public class HostMetadata {
    private final InetSocketAddress tcpSocketAddress;
    private final InetSocketAddress udpSocketAddress;
    private final int identifier;
    private final Map<String, Integer> space;

    public HostMetadata(
            InetSocketAddress tcpSocketAddress,
            InetSocketAddress udpSocketAddress,
            int identifier,
            Map<String, Integer> space) {
        this.tcpSocketAddress = tcpSocketAddress;
        this.udpSocketAddress = udpSocketAddress;
        this.identifier = identifier;
        this.space = space;
    }

    public InetSocketAddress getTcpSocketAddress() {
        return tcpSocketAddress;
    }

    public InetSocketAddress getUdpSocketAddress() {
        return udpSocketAddress;
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
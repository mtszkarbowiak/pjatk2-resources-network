import java.net.*;
import java.util.*;

public class SlaveRegistry {
    private Map<Integer, SlaveInfo> slaves;

    public SlaveRegistry(){
        slaves = new HashMap<>(512);
    }

    public boolean tryRegister(int identifier, SocketAddress socketAddress){
        if(slaves.containsKey(identifier))
            return false;

        var slaveInfo = new SlaveInfo(socketAddress, identifier);
        slaves.put(identifier, slaveInfo);

        return true;
    }

    public int countEntries(){
        return slaves.size();
    }
}

class SlaveInfo {
    private SocketAddress slaveServer;
    private int identifier;

    public SlaveInfo(SocketAddress slaveServer, int identifier) {
        this.slaveServer = slaveServer;
        this.identifier = identifier;
    }

    public SocketAddress getSlaveServer() {
        return slaveServer;
    }

    public int getIdentifier() {
        return identifier;
    }
}
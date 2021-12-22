import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class SlaveRegistry {
    private Map<Integer, SlaveInfo> slaves;

    public SlaveRegistry(){
        slaves = new HashMap<>(512);
    }

    public boolean tryRegister(int identifier, InetSocketAddress socketAddress){
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
    private InetSocketAddress slaveServer;
    private int identifier;

    public SlaveInfo(InetSocketAddress slaveServer, int identifier) {
        this.slaveServer = slaveServer;
        this.identifier = identifier;
    }

    public InetSocketAddress getSlaveServer() {
        return slaveServer;
    }

    public int getIdentifier() {
        return identifier;
    }
}
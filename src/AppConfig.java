import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class AppConfig {
    private  int identifier = -1;
    private int hostingPort = -1;
    private InetAddress gatewayAddress = null;
    private int gatewayPort = -1;
    private final Map<String, Integer> resourcesSpaces;

    public AppConfig(String[] args) throws UnknownHostException
    {
        resourcesSpaces = new HashMap<>();
        var expectation = ArgExpectation.Resource;

        for (var arg : args) {
            if(expectation == ArgExpectation.Resource){
                expectation =  switch(arg){
                    case "-ident" -> ArgExpectation.Identifier;
                    case "-tcpport" -> ArgExpectation.Port;
                    case "-gateway" -> ArgExpectation.Gateway;
                    default -> ArgExpectation.Resource;
                };

                if(expectation != ArgExpectation.Resource) {
                    continue;
                }
            }

            switch (expectation) {
                case Identifier -> identifier = Integer.parseInt(arg);
                case Port -> hostingPort = Integer.parseInt(arg);
                case Gateway -> {
                    var subArgs = arg.split(":");
                    var address = InetAddress.getByName(subArgs[0]);
                    var port = Integer.parseInt(subArgs[1]);

                    gatewayAddress = address;
                    gatewayPort = port;
                }
                case Resource -> {
                    var subArgs = arg.split(":");
                    var id = subArgs[0];
                    var value = Integer.parseInt(subArgs[1]);

                    resourcesSpaces.put(id, value);
                }
                default -> throw new IllegalStateException("Unexpected value: " + expectation);
            }
            expectation = ArgExpectation.Resource;
        }
    }

    public AppConfig(
            int identifier,
            int hostingPort,
            InetAddress gatewayAddress,
            Map<String, Integer> resourcesSpaces)
    {
        this.identifier = identifier;
        this.hostingPort = hostingPort;
        this.gatewayAddress = gatewayAddress;
        this.resourcesSpaces = resourcesSpaces;
    }

    public int getIdentifier() {
        return identifier;
    }

    public int getHostingPort() {
        return hostingPort;
    }

    public InetAddress getGatewayAddress() {
        return gatewayAddress;
    }

    public int getGatewayPort() {
        return gatewayPort;
    }

    public Map<String, Integer> getResourcesSpaces() {
        return resourcesSpaces;
    }

    public boolean isMainHost(){
        return gatewayAddress == null;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("AppConfig{ ID=");
        str.append(identifier);
        str.append(" HostingPort=");
        str.append(hostingPort);

        if(gatewayAddress != null)
        {
            str.append(" GatewayAddress=");
            str.append(gatewayAddress);
            str.append(" GatewayPort=");
            str.append(gatewayPort);
        }
        else{
            str.append(" NoGateway");
        }


        for (var entry : resourcesSpaces.entrySet()) {
            str.append(' ');
            str.append(entry.getKey());
            str.append(":");
            str.append(entry.getValue());
        }

        str.append(" }");

        return str.toString();
    }
}

enum ArgExpectation{
    Resource,
    Identifier,
    Port,
    Gateway
}
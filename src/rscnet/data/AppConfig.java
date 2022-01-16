package rscnet.data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class AppConfig {
    private int identifier = -1;
    private int hostingPort = -1;
    private InetAddress gatewayAddress = null;
    private int gatewayPort = -1;
    private final Map<String, Integer> resourcesSpaces;

    public AppConfig(String[] args) throws UnknownHostException
    {
        resourcesSpaces = new HashMap<>();
        ArgExpectation expectation = ArgExpectation.Resource;

        for (String arg : args) {
            if(expectation == ArgExpectation.Resource){
                 switch(arg){
                     case "-ident": expectation = ArgExpectation.Identifier; break;
                     case "-tcpport": expectation = ArgExpectation.Port; break;
                     case "-gateway": expectation = ArgExpectation.Gateway; break;
                     default: break;
                }

                if(expectation != ArgExpectation.Resource) {
                    continue;
                }
            }

            switch (expectation) {
                case Identifier: identifier = Integer.parseInt(arg); break;
                case Port: hostingPort = Integer.parseInt(arg); break;
                case Gateway: {
                    String[] subArgs = arg.split(":");
                    InetAddress address = InetAddress.getByName(subArgs[0]);
                    int port = Integer.parseInt(subArgs[1]);

                    gatewayAddress = address;
                    gatewayPort = port;
                } break;
                case Resource: {
                    String[] subArgs = arg.split(":");
                    String id = subArgs[0];
                    int value = Integer.parseInt(subArgs[1]);

                    resourcesSpaces.put(id, value);
                } break;
                default: throw new IllegalStateException("Unexpected value: " + expectation);
            }
            expectation = ArgExpectation.Resource;
        }
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

    public boolean isMasterHost(){
        return gatewayAddress == null;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("AppConfig( ID=");
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


        for (Map.Entry<String, Integer> entry : resourcesSpaces.entrySet()) {
            str.append(' ');
            str.append(entry.getKey());
            str.append(":");
            str.append(entry.getValue());
        }

        str.append(" )");

        return str.toString();
    }
}

enum ArgExpectation{
    Resource,
    Identifier,
    Port,
    Gateway
}
package rscnet.data;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static rscnet.Constants.App.NULL_PORT;

public class AppConfig {
    private int identifier = -1;
    private int hostingTcpPort = NULL_PORT;
    private int hostingUdpPort = NULL_PORT;
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
                     case "-tcpport": expectation = ArgExpectation.TcpPort; break;
                     case "-udpport": expectation = ArgExpectation.UdpPort; break;
                     case "-gateway": expectation = ArgExpectation.Gateway; break;
                     default: break;
                }

                if(expectation != ArgExpectation.Resource) {
                    continue;
                }
            }

            switch (expectation) {
                case Identifier: identifier = Integer.parseInt(arg); break;
                case TcpPort: hostingTcpPort = Integer.parseInt(arg); break;
                case UdpPort: hostingUdpPort = Integer.parseInt(arg); break;
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

    public int getHostingTcpPort() {
        return hostingTcpPort;
    }

    public boolean hasHostingUdpPort(){
        return hostingUdpPort != NULL_PORT;
    }

    public int getHostingUdpPort(){
        return hostingUdpPort;
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
        str.append(" HostingTcpPort=");
        str.append(hostingTcpPort);

        if(hasHostingUdpPort()){
            str.append(" HostingUdpPort=");
            str.append(hostingUdpPort);
        }else{
            str.append(" NoUdpHostingPort");
        }

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
            str.append(':');
            str.append(entry.getValue());
        }

        str.append(" )");

        return str.toString();
    }
}

enum ArgExpectation{
    Resource,
    Identifier,
    TcpPort,
    UdpPort,
    Gateway
}
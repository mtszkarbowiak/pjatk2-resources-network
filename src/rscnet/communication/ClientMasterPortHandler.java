package rscnet.communication;

import rscnet.Constants;
import rscnet.InternalCommunication;
import rscnet.TerminationListener;
import rscnet.data.AppConfig;
import rscnet.logging.LogType;
import rscnet.logic.HostMetadata;
import rscnet.logic.HostStatus;
import rscnet.logic.NetworkStatus;
import rscnet.utils.ThreadBlocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

@SuppressWarnings("PointlessBooleanExpression")
public class ClientMasterPortHandler extends AbstractPortHandler
{
    private final AppConfig config;
    private final InternalCommunication internalCommunication;
    private final UnreliableConnectionFactory unreliableConnectionFactory;
    private final NetworkStatus networkStatus;
    private final TerminationListener appTerminationRequestHandler;
    private Queue<HostStatus> collapseQueue;
    private HostStatus collapsingHost;

    public ClientMasterPortHandler(
            AppConfig config,
            InternalCommunication internalCommunication,
            UnreliableConnectionFactory unreliableConnectionFactory,
            NetworkStatus networkStatus,
            TerminationListener appTerminationRequestHandler) {
        this.config = config;
        this.internalCommunication = internalCommunication;
        this.unreliableConnectionFactory = unreliableConnectionFactory;
        this.networkStatus = networkStatus;
        this.appTerminationRequestHandler = appTerminationRequestHandler;
    }

    @Override
    protected Connection openConnection() throws IOException {
        ThreadBlocking.wait(
                () -> internalCommunication.collapseNetworkInternalPass.hasValue() == false
                && getKeepAlive(), this);

        if(collapseQueue == null){
            collapseQueue = new LinkedList<>();
            for (HostStatus host : networkStatus.getHosts()) {
                if(host.getMetadata().getIdentifier() == config.getIdentifier()) continue;

                collapseQueue.add(host);
            }
        }

        if(collapseQueue.isEmpty() == false)
        {
            collapsingHost = collapseQueue.remove();
            HostMetadata hostMetadata = collapsingHost.getMetadata();

            if(unreliableConnectionFactory == null) {
                Socket socket = new Socket();
                InetSocketAddress tcpSocketAddress = hostMetadata.getTcpSocketAddress();
                log("Collapsing: " + collapsingHost.getMetadata().getIdentifier() + " at (TCP)" + tcpSocketAddress, LogType.Info);

                socket.connect(tcpSocketAddress);

                return new ReliableConnection(socket);
            }else{
                InetSocketAddress udpSocketAddress = hostMetadata.getUdpSocketAddress();
                log("Collapsing: " + collapsingHost.getMetadata().getIdentifier() + " at (UDP)" + udpSocketAddress, LogType.Info);

                return unreliableConnectionFactory.openUnreliableConnection(
                        udpSocketAddress);
            }
        }
        else /*if(collapseQueue.isEmpty())*/
        {
            log("Collapse queue empty.", LogType.Info);
            internalCommunication.collapseNetworkInternalPass.getValue();
            appTerminationRequestHandler.terminate();
            return null;
        }
    }

    @Override
    protected void useConnection(Connection connection) throws IOException {
        log("Sending collapse request...", LogType.Out);
        connection.send(Constants.NetCommands.COLLAPSE_REQUEST);

        collapsingHost = null;
    }

    @Override
    protected String getLogPrefix() { return "Master >"; }
}
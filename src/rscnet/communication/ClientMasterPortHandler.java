package rscnet.communication;

import rscnet.Constants;
import rscnet.InternalCommunication;
import rscnet.TerminationListener;
import rscnet.data.AppConfig;
import rscnet.logging.LogType;
import rscnet.logic.HostStatus;
import rscnet.logic.NetworkStatus;
import rscnet.utils.ThreadBlocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

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
            for (var host : networkStatus.getHosts()) {
                if(host.getMetadata().getIdentifier() == config.getIdentifier()) continue;

                collapseQueue.add(host);
            }
        }

        if(collapseQueue.isEmpty() == false)
        {
            collapsingHost = collapseQueue.remove();

            var address = collapsingHost.getMetadata().getSocketAddress();

            log("Collapsing: " + collapsingHost.getMetadata().getIdentifier() + " at " + address, LogType.Info);

            if(unreliableConnectionFactory == null) {
                var socket = new Socket();
                socket.connect(address);
                return new ReliableConnection(socket);
            }else{
                var unreliableMasterSocketAddress = new InetSocketAddress(
                        address.getAddress(), address.getPort() + 100);
                return unreliableConnectionFactory.openUnreliableConnection(unreliableMasterSocketAddress);
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
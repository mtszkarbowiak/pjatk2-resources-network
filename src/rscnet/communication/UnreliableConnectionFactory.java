package rscnet.communication;

import rscnet.TerminationListener;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import static rscnet.communication.UnreliableCommunicationUtils.*;
import static rscnet.Constants.UnreliableCommunication.*;


public class UnreliableConnectionFactory implements Runnable, TerminationListener {
    private final DatagramSocket socket;
    private final ConcurrentLinkedQueue<UnreliablePacketWrapper> outgoingPacketQueue;
    private final ConcurrentHashMap<Long, UnreliableConnection> openConnections;
    private final ConcurrentLinkedQueue<UnreliablePacketMeta> waitingConnections;
    private boolean keepAlive = true;

    public UnreliableConnectionFactory(int receiveTimeout, int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.outgoingPacketQueue = new ConcurrentLinkedQueue<>();
        this.openConnections = new ConcurrentHashMap<>();
        this.waitingConnections = new ConcurrentLinkedQueue<>();

        socket.setSoTimeout(receiveTimeout);

        log("Datagram socket open: " + port);
    }

    public Connection openUnreliableConnection(InetSocketAddress target){
        if(target == null) throw new NullPointerException("Address can not be null");

        UnreliableConnection unreliableConnection = new UnreliableConnection(target, this, getNextRandomlyUniqueConnectionID());
        openConnections.put(unreliableConnection.getId(), unreliableConnection);
        return unreliableConnection;
    }

    public Connection acceptUnreliableConnectionOrNull(int timeout){
        // Handle already open connections
        if(!openConnections.isEmpty()){
            Long any = openConnections.keys().nextElement();
            return openConnections.get(any);
        }

        // Accept new, not yet opened connections
        if (waitingConnections.isEmpty()){
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ignored) {}
        }

        if(waitingConnections.isEmpty()) return null;
        UnreliablePacketMeta incomingPacketMeta = waitingConnections.remove();

        InetSocketAddress address = incomingPacketMeta.address;
        UnreliablePacketWrapper packet = incomingPacketMeta.packet;

        if(address == null) throw new NullPointerException("Address can not be null");

        UnreliableConnection result = new UnreliableConnection(address, this, packet.getConnectionId());
        openConnections.put(packet.getConnectionId(), result);
        return result;
    }

    @Override
    public void terminate(){
        keepAlive = false;
    }

    @Override
    public void run() {
        log("Unreliable connection factory started.");

        while (keepAlive){
            try{
                // Receiving 1 packet.
                try{
                    byte[] buffer = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String message = new String(packet.getData(), StandardCharsets.UTF_8);
                    UnreliablePacketWrapper wrappedPacket = UnreliablePacketWrapper.parse(message);
                    long connectionId = wrappedPacket.getConnectionId();

                    log("Packet arrived: " + wrappedPacket);

                    if(openConnections.containsKey(connectionId)){
                        UnreliableConnection connection = openConnections.get(connectionId);
                        connection.passIncomingPacket(wrappedPacket);
                    }
                    else if(wrappedPacket.getMessageId() < 2){
                        InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
                        UnreliableConnection connection = new UnreliableConnection(address, this, wrappedPacket.getConnectionId());
                        openConnections.put(wrappedPacket.getConnectionId(), connection);
                    }else{
                        log("WARNING! Incoming message not corresponding to any opened connection: " +  wrappedPacket);
                    }
                }
                catch (SocketTimeoutException ignored){}

                for (int i = 0; i < MAX_OUT_TO_IN_PACKET_RATIO && !outgoingPacketQueue.isEmpty(); i++) {
                    UnreliablePacketWrapper outgoingPacket = outgoingPacketQueue.remove();
                    InetSocketAddress address = openConnections.get(outgoingPacket.getConnectionId()).getRemoteSocketAddress();
                    byte[] data = outgoingPacket.toString().getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, address);

                    socket.send(packet);
                    log("Packet sent (to " + address + "): " + outgoingPacket);
                }
            }catch(IOException ioException){
                log("Exception interrupted loop cycle: ");

                ioException.printStackTrace();
            }
        }

        log("Unreliable communication loop ended.");
    }

    public void passOutgoingPacket(UnreliablePacketWrapper outgoingPacket) {
        outgoingPacketQueue.add(outgoingPacket);
    }


    private static long getNextRandomlyUniqueConnectionID(){
        long result = 0;
        for (int i = 0; i < 63; i++) {
            result <<= 1;

            if(Math.random() > 0.5){
                result = result | 0b1;
            }
        }
        return result;
    }

    public void markAsClosed(long id) {
        openConnections.remove(id);
    }

    public boolean isAlive(){
        return keepAlive;
    }
}


@SuppressWarnings("BusyWait")
class UnreliableConnection implements Connection {
    private final InetSocketAddress targetAddress;
    private final long id;
    private final UnreliableConnectionFactory server;
    private final ConcurrentLinkedDeque<UnreliablePacketWrapper> incomingValuePackets = new ConcurrentLinkedDeque<>(); // Only message packets.
    private int lastMessageId;
    private int lastConfirmedMessageId;
    private UnreliablePacketMessageBufferedReader messageBufferedReader;

    public UnreliableConnection(InetSocketAddress target, UnreliableConnectionFactory server, long id) {
        if(target == null) throw new NullPointerException("Target port address can not be null.");

        this.targetAddress = target;
        this.id = id;
        this.server = server;
    }

    @Override
    public void send(String message) throws IOException {
        messageBufferedReader = null;

        lastMessageId++;
        UnreliablePacketWrapper sentMessage = new UnreliablePacketWrapper(
                id, lastMessageId, 0, UnreliablePacketType.Message,
                message.replace("\n", LL_LINE_REPRESENTATION));
        for (int attempt = 0; attempt < SEND_ATTEMPTS; attempt++) {
            server.passOutgoingPacket(sentMessage);

            try{ Thread.sleep(SEND_ATTEMPTS_INTERVAL); }
            catch (InterruptedException ignored){}

            if(lastMessageId == lastConfirmedMessageId) {
                return;
            }

            sentMessage = sentMessage.createNextAttempt();
        }
        throw new IOException("Confirmation has not arrived.");
    }

    @Override
    public String receive() throws IOException {
        if(messageBufferedReader != null){
            return messageBufferedReader.getNextLineOrNull();
        }

        while (server.isAlive()){
            try {
                Thread.sleep(RECEIVE_ATTEMPT_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(incomingValuePackets.isEmpty()) continue;

            UnreliablePacketWrapper packet = incomingValuePackets.remove();
            messageBufferedReader = new UnreliablePacketMessageBufferedReader(packet.getMessageValue());
            return messageBufferedReader.getNextLineOrNull();
        }

        throw new UnreliableConnectionTerminatedException("No packet can be received when the server is being shut down.");
    }

    @Override public InetSocketAddress getRemoteSocketAddress() {
        return targetAddress;
    }
    @Override public void close() { server.markAsClosed(id); }

    public long getId() { return id;  }

    public void passIncomingPacket(UnreliablePacketWrapper wrappedPacket) {
        if(wrappedPacket.getConnectionId() != id)
            throw new IllegalArgumentException("This packet should not be handled by this connection.");

        switch (wrappedPacket.getPacketType()){
            case Message: {
                incomingValuePackets.add(wrappedPacket);
                log("Value packet arrived.");
                server.passOutgoingPacket(wrappedPacket.createConfirmation());
            } break;

            case Acknowledgement: {
                if(wrappedPacket.getMessageId() == lastMessageId) {
                    log("Packet arrival confirmed.");
                    lastConfirmedMessageId = Math.max(lastMessageId, lastConfirmedMessageId);
                }else{
                    log("Obsolete confirmation arrived. Ignoring.");
                }
            } break;

            default: throw new IllegalArgumentException("Different packet types not yet implemented.");
        }
    }
}


class UnreliablePacketMessageBufferedReader{
    private final String[] lines;
    private int index;

    public UnreliablePacketMessageBufferedReader(String original) {
        lines = original.split("::");
        index = 0;
    }

    public String getNextLineOrNull(){
        if(index >= lines.length)
            return null;
        else
            return lines[index++];
    }
}


class UnreliablePacketMeta{
    public final UnreliablePacketWrapper packet;
    public final InetSocketAddress address;

    public UnreliablePacketMeta(UnreliablePacketWrapper packet, InetSocketAddress address) {
        this.packet = packet;
        this.address = address;
    }
}


class UnreliablePacketWrapper{
    private final long connectionId;
    private final int messageId;
    private final int attemptId;
    private final UnreliablePacketType packetType;
    private final String messageValue;
    private final String toStringCached;

    public UnreliablePacketWrapper(long connectionId, int messageId, int attemptId, UnreliablePacketType packetType, String messageValue){
        this.connectionId = connectionId;
        this.messageId = messageId;
        this.attemptId = attemptId;
        this.packetType = packetType;
        this.messageValue = messageValue;

        toStringCached =
            connectionId + // 0
            PROTOCOL_SEPARATOR +
            messageId + // 1
            PROTOCOL_SEPARATOR +
            attemptId + // 2
            PROTOCOL_SEPARATOR +
            packetType.toString() + // 3
            PROTOCOL_SEPARATOR +
            messageValue + // 4
            PROTOCOL_SEPARATOR;
    }

    public static UnreliablePacketWrapper parse(String text){
        String[] elements = text.split(PROTOCOL_SEPARATOR);

        long connectionId = Long.parseLong(elements[0]);
        int messageId = Integer.parseInt(elements[1]);
        int attemptId = Integer.parseInt(elements[2]);
        UnreliablePacketType type = UnreliablePacketType.valueOf(elements[3]);
        String messageValue = elements[4];

        return new UnreliablePacketWrapper(connectionId, messageId, attemptId, type, messageValue);
    }


    @Override
    public String toString() {
        return toStringCached;
    }



    public long getConnectionId() {
        return connectionId;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public UnreliablePacketType getPacketType() {
        return packetType;
    }

    public String getMessageValue() {
        return messageValue;
    }



    public UnreliablePacketWrapper createNextAttempt(){
        return new UnreliablePacketWrapper(connectionId, messageId, attemptId + 1, packetType, messageValue);
    }

    public UnreliablePacketWrapper createConfirmation(){
        return new UnreliablePacketWrapper(connectionId, messageId, 0, UnreliablePacketType.Acknowledgement, "EMPTY");
    }
}


enum UnreliablePacketType{
    Message,
    Acknowledgement,
}


class UnreliableCommunicationUtils{
    public static void log(String msg){
        System.out.println("<UC> " + msg);
    }
}
package rscnet.communication;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

public class UnreliableConnectionFactory implements Runnable{
    private DatagramSocket socket;
    private ConcurrentLinkedQueue<UnreliablePacketWrapper> outgoingPacketQueue;
    private ConcurrentHashMap<Long, UnreliableConnection> openConnections;
    private ConcurrentLinkedQueue<UnreliablePacketMeta> waitingConnections;

    public UnreliableConnectionFactory(int receiveTimeout, int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.outgoingPacketQueue = new ConcurrentLinkedQueue<>();
        this.openConnections = new ConcurrentHashMap<>();
        this.waitingConnections = new ConcurrentLinkedQueue<>();

        socket.setSoTimeout(receiveTimeout);

        System.out.println("[[US]] Datagram socket open: " + port);
    }

    public Connection openUnreliableConnection(InetSocketAddress target){
        if(target == null) throw new NullPointerException("Address can not be null");

        var unreliableConnection = new UnreliableConnection(target, this, getNextRandomlyUniqueConnectionID());
        openConnections.put(unreliableConnection.getId(), unreliableConnection);
        return unreliableConnection;
    }

    public Connection acceptUnreliableConnectionOrNull(int timeout){
        // Handle already open connections
        if(!openConnections.isEmpty()){
            var any = openConnections.keys().asIterator().next();
            return openConnections.get(any);
        }

        // Accept new, not yet opened connections
        if (waitingConnections.isEmpty()){
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ignored) {}
        }

        if(waitingConnections.isEmpty()) return null;
        var incomingPacketMeta = waitingConnections.remove();

        var address = incomingPacketMeta.address;
        var packet = incomingPacketMeta.packet;

        if(address == null) throw new NullPointerException("Address can not be null");

        var result = new UnreliableConnection(address, this, packet.getConnectionId());
        openConnections.put(packet.getConnectionId(), result);
        return result;
    }

    @Override
    public void run() {
        System.out.println("[[US]] Unreliable connection factory started.");

        while (true){
            try{
                // Receiving 1 packet.
                try{
                    var buffer = new byte[2048];
                    var packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    var message = new String(packet.getData(), StandardCharsets.UTF_8);
                    var wrappedPacket = UnreliablePacketWrapper.parse(message);
                    var connectionId = wrappedPacket.getConnectionId();

                    System.out.println("[[US]] Packet arrived: " + wrappedPacket);

                    if(openConnections.containsKey(connectionId)){
                        var connection = openConnections.get(connectionId);
                        connection.passIncomingPacket(wrappedPacket);
                    }
                    else if(wrappedPacket.getMessageId() < 2){
                        var address = new InetSocketAddress(packet.getAddress(), packet.getPort());
                        var connection = new UnreliableConnection(address, this, wrappedPacket.getConnectionId());
                        openConnections.put(wrappedPacket.getConnectionId(), connection);
                    }else{
                        System.out.println("[[US]] WARNING! " +
                                "Incoming message not corresponding to any opened connection: " +
                                wrappedPacket);
                    }
                }
                catch (SocketTimeoutException ignored){}

                // Sending up to 3 packets.
                for (int i = 0; i < 3 && !outgoingPacketQueue.isEmpty(); i++) {
                    var outgoingPacket = outgoingPacketQueue.remove();
                    var address = openConnections.get(outgoingPacket.getConnectionId()).getRemoteSocketAddress();
                    var data = outgoingPacket.toString().getBytes(StandardCharsets.UTF_8);
                    var packet = new DatagramPacket(data, data.length, address);

                    socket.send(packet);
                    System.out.println("[[US]] Packet sent (to " + address + "): " + outgoingPacket.toString());
                }
            }catch(IOException ioException){
                System.out.println("[[US]] Exception interrupted loop cycle:");
                ioException.printStackTrace();
            }
        }
    }

    public void passOutgoingPacket(UnreliablePacketWrapper outgoingPacket) throws IOException {
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
}

class UnreliableConnection implements Connection {
    private final InetSocketAddress targetAddress;
    private final long id;
    private final UnreliableConnectionFactory server;
    private final UnreliableConnectionSettings settings = new UnreliableConnectionSettings();
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
        final var originalMessage = new UnreliablePacketWrapper(
                id, lastMessageId, 0, UnreliablePacketType.Message,
                message.replace("\n","::"));
        var sentMessage = originalMessage;
        for (int attempt = 0; attempt < settings.getSendAttempts(); attempt++) {
            server.passOutgoingPacket(sentMessage);

            try{ Thread.sleep(settings.getSendAttemptInterval()); }
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

        while (true){
            try {
                Thread.sleep(settings.getReceiveInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(incomingValuePackets.isEmpty()) continue;

            var packet = incomingValuePackets.remove();
            messageBufferedReader = new UnreliablePacketMessageBufferedReader(packet.getMessageValue());
            return messageBufferedReader.getNextLineOrNull();
        }
    }

    @Override public InetSocketAddress getRemoteSocketAddress() {
        return targetAddress;
    }
    @Override public void close() throws IOException { server.markAsClosed(id); }

    public long getId() { return id;  }

    public void passIncomingPacket(UnreliablePacketWrapper wrappedPacket) throws IOException {
        if(wrappedPacket.getConnectionId() != id)
            throw new IllegalArgumentException("This packet should not be handled by this connection.");

        switch (wrappedPacket.getPacketType()){
            case Message -> {
                incomingValuePackets.add(wrappedPacket);
                System.out.println("[[US]] Value packet arrvied.");
                server.passOutgoingPacket(wrappedPacket.createConfirmation()); // ONLY 1 CONFIRMATION
            }
            case Acknowledgement -> {
                if(wrappedPacket.getMessageId() == lastMessageId) {
                    System.out.println("[[US]] Packet arrival confirmed.");
                    lastConfirmedMessageId = lastMessageId;
                }else{
                    System.out.println("[[US]] Obsolete confirmation arrived. Ignoring.");
                }
            }
            default -> {
                throw new IllegalArgumentException("Different packet types not yet implemented.");
            }
        }
    }
}

class UnreliablePacketMessageBufferedReader{
    private String[] lines;
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
    private static final String SEPARATOR = "::::";
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

        var s = new StringBuilder();

        s.append(connectionId); // 0
        s.append(SEPARATOR);
        s.append(messageId); // 1
        s.append(SEPARATOR);
        s.append(attemptId); // 2
        s.append(SEPARATOR);
        s.append(packetType.toString()); // 3
        s.append(SEPARATOR);
        s.append("EMPTY"); // 4
        s.append(SEPARATOR);
        s.append("EMPTY"); // 5
        s.append(SEPARATOR);
        s.append(messageValue); // 6
        s.append(SEPARATOR);
        // Seperating empty space

        toStringCached = s.toString();
    }

    public static UnreliablePacketWrapper parse(String text){
        var elements = text.split(SEPARATOR);

        var connectionId = Long.parseLong(elements[0]);
        var messageId = Integer.parseInt(elements[1]);
        var attemptId = Integer.parseInt(elements[2]);
        var type = UnreliablePacketType.valueOf(elements[3]);
        var messageValue = elements[6];

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

    public UnreliablePacketWrapper createNextMessage(String newMessageValue){
        return new UnreliablePacketWrapper(connectionId, messageId + 1, 0, packetType, newMessageValue);
    }

    public UnreliablePacketWrapper createConfirmation(){
        return new UnreliablePacketWrapper(connectionId, messageId, 0, UnreliablePacketType.Acknowledgement, "EMPTY");
    }
}

enum UnreliablePacketType{
    Message,
    Acknowledgement,
    HoldOn,
}

class UnreliableConnectionSettings{
    public int getSendAttempts(){
        return 5;
    }

    public int getSendAttemptInterval(){
        return 350;
    }

    public int getReceiveInterval(){
        return 50;
    }
}
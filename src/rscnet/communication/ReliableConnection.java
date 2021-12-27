package rscnet.communication;

import com.sun.source.tree.BreakTree;

import java.io.*;
import java.net.*;

public class ReliableConnection implements Connection {
    private final Socket socket;
    private final BufferedWriter bufferedWriter;
    private final BufferedReader bufferedReader;


    public ReliableConnection(Socket socket) throws IOException {
        this.socket = socket;

        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    @Override
    public void send(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    @Override
    public String receive() throws IOException{
        return bufferedReader.readLine();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
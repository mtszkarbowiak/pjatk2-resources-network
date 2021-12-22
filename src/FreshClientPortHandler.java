import java.io.*;
import java.net.*;

public class FreshClientPortHandler extends AbstractPortHandler{
    @Override protected String getLogPrefix() { return "Client"; }
    @Override protected boolean keepAlive() { return true; }

    // You can ask a friend where is the master.
    private InetSocketAddress friendInetSocketAddress;
    private InetSocketAddress masterInetSocketAddress;
    private Socket currentSocket;

    public FreshClientPortHandler(InetAddress gatewayAddress, int gatewayPort) {
        this.friendInetSocketAddress = new InetSocketAddress(gatewayAddress, gatewayPort);
    }

    @Override
    protected void update() throws IOException {
        if(masterInetSocketAddress == null){
            if(currentSocket == null || currentSocket.isConnected() == false){
                log("Establishing connection to the friend.", LogType.Info);
                currentSocket = new Socket();
                currentSocket.connect(friendInetSocketAddress);

                log("Connection established.", LogType.Info);
                var writer = new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream()));
                var reader = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

                log("Asking for the master...", LogType.Out);
                writer.write(NetCommandFormatting.HeadRequest);
                writer.newLine();
                writer.flush();

                var responseAboutMaster = reader.readLine();
                log("Friend response: " + responseAboutMaster, LogType.In);

                var args = responseAboutMaster.split(" ");
                switch (args[0]) {
                    case NetCommandFormatting.HeadResponseAboutMaster -> {
                        var masterAddress = InetAddress.getByName(args[1]);
                        var masterPort = Integer.parseInt(args[2]);
                        masterInetSocketAddress = new InetSocketAddress(masterAddress, masterPort);
                        log("Master aknowledged: " + masterInetSocketAddress, LogType.In);
                        currentSocket.close();
                    }

                    case NetCommandFormatting.HeadResponseMeMaster -> {
                        masterInetSocketAddress = friendInetSocketAddress;
                        log("Friend is the master!", LogType.In);
                    }

                    case NetCommandFormatting.HeadResponseFail ->
                            log("Friend does not know any master.", LogType.In);
                }
            }
        }
    }

    @Override
    protected void onHalted() {
        try {
            currentSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

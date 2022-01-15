package rscnet.utils;

import rscnet.communication.*;
import java.io.IOException;

import rscnet.Constants.NetCommands;

public class ConnectionUtils {
    public static String receiveMultiline(Connection connection) throws IOException {
        var totalResponse = new StringBuilder();
        String line;
        while ((line = connection.receive()) != null){
            totalResponse.append(line);
            totalResponse.append(NetCommands.HL_LINE_REPLACER);
        }
        return totalResponse.toString();
    }

    public static String translateResponse(String originalResponse)
    {
        return originalResponse.replace(NetCommands.HL_LINE_REPLACER,"\n");
    }
}

package rscnet.utils;

import rscnet.communication.*;
import java.io.IOException;

public class ConnectionUtils {
    public static String receiveMultiline(Connection connection) throws IOException {
        var totalResponse = new StringBuilder();
        String line;
        while ((line = connection.receive()) != null){
            totalResponse.append(line);
            totalResponse.append(NetCommands.NewLineReplacer);
        }
        return totalResponse.toString();
    }
}

import java.net.*;
import java.util.*;

public class Main
{
    public static void main(String[] args) throws UnknownHostException {
        Tests.internalTests();

        System.out.println("---* PROGRAM START *---");

        var config = new AppConfig(args);
        var resourceRegistry = new ResourceRegistry(config.getResourcesSpaces());

        System.out.println(config);
        System.out.println("MainHostMode: " + config.isMainHost());
        System.out.println("");
        System.out.println("Opening ports...");


    }
}
package rscnet.data;

import java.util.*;

public class AllocationRequest {
    private final int clientIdentifier;
    private final Map<String,Integer> resources;
    private final String text;

    public AllocationRequest(String format){
        String[] args = format.split(" ");

        this.resources = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            String[] subArgs = args[i].split(":");
            String key = subArgs[0];
            int value = Integer.parseInt(subArgs[1]);

            resources.put(key,value);
        }

        this.clientIdentifier = Integer.parseInt(args[0]);
        this.text = getText();
    }

    private String getText(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(clientIdentifier);

        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            stringBuilder.append(' ');
            stringBuilder.append(entry.getKey());
            stringBuilder.append(':');
            stringBuilder.append(entry.getValue());
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return text;
    }

    public int getClientIdentifier() {
        return clientIdentifier;
    }

    public Set<String> getDemandedResources(){
        return resources.keySet();
    }

    public int getDemandedQuantity(String name){
        return resources.getOrDefault(name, 0);
    }
}

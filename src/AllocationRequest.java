import java.util.HashMap;
import java.util.Map;

public class AllocationRequest {
    private final int clientIdentifier;
    private final Map<String,Integer> resources;
    private final String text;

    public AllocationRequest(int clientIdentifier, Map<String, Integer> resources) {
        this.clientIdentifier = clientIdentifier;
        this.resources = resources;
        this.text = getText();
    }

    public AllocationRequest(String format){
        var args = format.split(" ");

        this.resources = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            var subArgs = args[i].split(":");
            var key = subArgs[0];
            var value = Integer.parseInt(subArgs[1]);

            resources.put(key,value);
        }

        this.clientIdentifier = Integer.parseInt(args[0]);
        this.text = getText();
    }

    private String getText(){
        var stringBuilder = new StringBuilder();
        stringBuilder.append(clientIdentifier);

        for (var entry : resources.entrySet()) {
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

    public Map<String, Integer> getResources() {
        return resources;
    }

    public AllocationRequest reduce(String resourceName, int amount){
        var newMap = new HashMap<>(this.resources);

        var prev = newMap.get(resourceName);
        var newVal = prev - amount;

        if(newVal > 0)
            newMap.put(resourceName, newVal);
        else if(newVal == 0)
            newMap.remove(resourceName);
        else{
            newMap.remove(resourceName);
            System.out.println("DATA INTEGRITY PROBLEM");
        }

        return new AllocationRequest(this.getClientIdentifier(), newMap);
    }
}

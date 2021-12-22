import java.util.*;

public class HostResourceRegistry {
    private Map<String, ResourceRegistry> subregistries;

    public HostResourceRegistry(Map<String, Integer> spaces){
        subregistries = new HashMap<>(spaces.entrySet().size());

        for (var spaceRecord : spaces.entrySet()) {
            var newRegistry = new ResourceRegistry(spaceRecord.getKey(), spaceRecord.getValue());
            subregistries.put(spaceRecord.getKey(), newRegistry);
        }
    }

    public int tryAlloc(String resourceName, int identifier, int requiredAllocs)
    {
        if(subregistries.containsKey(resourceName) == false)
            return 0; // No registry is equivalent of space 0.

        var subregistry = subregistries.get(resourceName);
        return subregistry.tryAllocate(identifier, requiredAllocs);
    }

    public int getValue(String resourceName, int identifier){
        var subregistry = subregistries.getOrDefault(resourceName, null);

        if(subregistry == null) return 0;

        return subregistry.getSpaceTakenBy(identifier);
    }
}

class ResourceRegistry{
    private final String name;
    private Map<Integer, Integer> sources;
    private int capacity;


    public ResourceRegistry(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;

        sources = new HashMap<>(capacity);
    }

    public int getTotalTakenSpace(){
        int result = 0;
        for (var value : sources.values())
            result += value;
        return result;
    }

    public int getFreeSpace(){
        int result = capacity - getTotalTakenSpace();
        if(result < 0) System.out.println("INTERNAL DATA INTEGRITY ERROR! " +
                "(HostResourceRegistry.ResourceRegistry.getFreeSpace())");
        return result;
    }

    public int tryAllocate(int identifier, int requiredAllocs){
        var space = getFreeSpace();
        if(space == 0) return 0;

        var result = Math.min(requiredAllocs, space);
        var previousVal = sources.getOrDefault(identifier, 0);
        sources.put(identifier, previousVal + result);

        return result;
    }

    public int getSpaceTakenBy(int identifier) {
        return sources.getOrDefault(identifier, 0);
    }
}
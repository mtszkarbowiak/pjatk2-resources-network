import java.util.*;

public class ResourceRegistry {
    private final Map<String, ResourceSubRegistry> subRegistries;

    public ResourceRegistry(Map<String, Integer> spaces){
        subRegistries = new HashMap<>(spaces.entrySet().size());

        for (var spaceRecord : spaces.entrySet()) {
            var newRegistry = new ResourceSubRegistry(spaceRecord.getValue());
            subRegistries.put(spaceRecord.getKey(), newRegistry);
        }
    }

    public int tryAlloc(String resourceName, int identifier, int requiredAllocs)
    {
        if(subRegistries.containsKey(resourceName) == false)
            return 0; // No registry is equivalent of space 0.

        var subregistry = subRegistries.get(resourceName);
        return subregistry.tryAllocate(identifier, requiredAllocs);
    }

    public int getValue(String resourceName, int identifier){
        var subregistry = subRegistries.getOrDefault(resourceName, null);

        if(subregistry == null) return 0;

        return subregistry.getSpaceTakenBy(identifier);
    }
}

class ResourceSubRegistry {
    private final Map<Integer, Integer> sources;
    private final int capacity;


    public ResourceSubRegistry(int capacity) {
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
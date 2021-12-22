import java.util.*;

public class ResourceRegistry {

    private Map<String, Integer> allocs;
    private Map<String, Integer> spaces;

    public ResourceRegistry(Map<String, Integer> spaces){
        this.spaces = spaces;
        allocs = new HashMap<>();

        for (var spaceRecord : spaces.entrySet()) {
            allocs.put(spaceRecord.getKey(), 0);
        }
    }

    public int tryAlloc(String resourceName, int requiredAllocs)
    {
        var space = spaces.getOrDefault(resourceName, 0);
        var alloc = allocs.getOrDefault(resourceName, 0);

        var free = space - alloc;

        if(free < 0) System.out.println("INTERNAL DATA INTEGRITY PROBLEM!");

        var result = Math.min(free, requiredAllocs);

        allocs.put(resourceName, alloc + result);

        return result;
    }
}

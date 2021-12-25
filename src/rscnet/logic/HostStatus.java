package rscnet.logic;

import java.util.HashMap;
import java.util.Map;

public class HostStatus {
    private final HostMetadata metadata;
    private final Map<String, ResourceAllocsReg> resourceAllocsRegMap;

    public HostStatus(HostMetadata metadata) {
        this.metadata = metadata;
        this.resourceAllocsRegMap = new HashMap<>();

        for (var rscName : metadata.getStoredResourcesNames()){
            var space = metadata.getResourceSpace(rscName);
            var resourceAllocRegistry = new ResourceAllocsReg(space);

            resourceAllocsRegMap.put(rscName, resourceAllocRegistry);
        }
    }

    public HostMetadata getMetadata() {
        return metadata;
    }

    public int allocate(String rscName, int identifier, int amount){
        var allocsReg = resourceAllocsRegMap.getOrDefault(rscName, null);

        if(allocsReg == null) return 0;

        return allocsReg.allocate(identifier, amount);
    }

    public int getFreeResourceSpace(String rscName) {
        var reg = resourceAllocsRegMap.getOrDefault(rscName, null);

        if(reg == null) return 0;

        return reg.getFreeSpace();
    }
}

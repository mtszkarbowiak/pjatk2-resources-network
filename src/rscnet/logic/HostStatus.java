package rscnet.logic;

import java.util.*;

public class HostStatus {
    private final HostMetadata metadata;
    private final Map<String, ResourceAllocationsReg> resourceAllocationsRegMap;

    public HostStatus(HostMetadata metadata) {
        this.metadata = metadata;
        this.resourceAllocationsRegMap = new HashMap<>();

        for (String rscName : metadata.getStoredResourcesNames()){
            int space = metadata.getResourceSpace(rscName);
            ResourceAllocationsReg resourceAllocRegistry = new ResourceAllocationsReg(space);

            resourceAllocationsRegMap.put(rscName, resourceAllocRegistry);
        }
    }

    public HostMetadata getMetadata() {
        return metadata;
    }

    public int allocate(String rscName, int identifier, int amount){
        ResourceAllocationsReg allocationsReg = resourceAllocationsRegMap.getOrDefault(rscName, null);

        if(allocationsReg == null) return 0;

        return allocationsReg.allocate(identifier, amount);
    }

    public int getFreeResourceSpace(String rscName) {
        ResourceAllocationsReg reg = resourceAllocationsRegMap.getOrDefault(rscName, null);

        if(reg == null) return 0;

        return reg.getFreeSpace();
    }

    public List<FullAllocRecord> getFullAllocInfo(){
        ArrayList<FullAllocRecord> result = new ArrayList<>();
        for (Map.Entry<String, ResourceAllocationsReg> rscAllocReg : resourceAllocationsRegMap.entrySet()) {
            String key = rscAllocReg.getKey();
            for (Map.Entry<Integer, Integer> recordKeyVal : rscAllocReg.getValue().getRecordsReadOnly().entrySet()) {
                result.add(new FullAllocRecord(key, recordKeyVal.getKey(), recordKeyVal.getValue()));
            }
        }
        return result;
    }
}

package rscnet.logic;

import java.util.*;

public class ResourceAllocationsReg {
    private final int totalSpace;
    private final Map<Integer, Integer> records;

    public ResourceAllocationsReg(int totalSpace) {
        this.totalSpace = totalSpace;
        this.records = new HashMap<>();
    }

    public int getTakenSpace(){
        int result = 0;

        for (Integer value : records.values()) {
            result += value;
        }

        return result;
    }

    public int getFreeSpace(){
        return totalSpace - getTakenSpace();
    }

    public int allocate(int identifier, int amount){
        int union = Math.min(getFreeSpace(), amount);

        if(union == 0) return 0;

        Integer previousIdAllocations = records.getOrDefault(identifier, 0);
        records.put(identifier, previousIdAllocations + union);

        return union;
    }

    public Map<Integer, Integer> getRecordsReadOnly() {
        return Collections.unmodifiableMap(records);
    }
}

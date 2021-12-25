package rscnet.logic;

import java.util.*;

public class ResourceAllocsReg {
    private final int totalSpace;
    private final Map<Integer, Integer> records;

    public ResourceAllocsReg(int totalSpace) {
        this.totalSpace = totalSpace;
        this.records = new HashMap<>();
    }

    public int getTakenSpace(){
        var result = 0;

        for (var value : records.values()) {
            result += value;
        }

        return result;
    }

    public int getFreeSpace(){
        return totalSpace - getTakenSpace();
    }

    public int allocate(int identifier, int amount){
        var union = Math.min(getFreeSpace(), amount);

        if(union == 0) return 0;

        var previousIdAllocs = records.getOrDefault(identifier, 0);
        records.put(identifier, previousIdAllocs + union);

        return union;
    }
}

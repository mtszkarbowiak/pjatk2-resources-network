package rscnet.logic;

import java.util.*;

public class ResourceAllocsReg {
    private final int totalSpace;
    private final Map<Integer, Integer> records;

    public ResourceAllocsReg(int totalSpace) {
        this.totalSpace = totalSpace;
        this.records = new HashMap<>();
    }

    public int tryAllocate(int identifier, int amount){
        var union = Math.min(totalSpace, amount);

        if(union == 0) return 0;

        var previousIdAllocs = records.getOrDefault(identifier, 0);
        records.put(identifier, previousIdAllocs + union);

        return union;
    }
}

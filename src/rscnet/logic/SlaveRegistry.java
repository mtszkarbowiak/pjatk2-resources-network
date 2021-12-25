package rscnet.logic;

import rscnet.AllocationRequest;

import java.net.*;
import java.util.*;

public class SlaveRegistry {
    private Map<Integer, SlaveInfo> slaveInfoMap;
    private Map<Integer, SlaveStatus> slaveStatusMap;

    public SlaveRegistry(){
        slaveInfoMap = new HashMap<>(512);
        slaveStatusMap = new HashMap<>(512);
    }

    public boolean tryRegister(int identifier, InetSocketAddress socketAddress, Map<String,Integer> space){
        if(slaveInfoMap.containsKey(identifier))
            return false;

        var slaveInfo = new SlaveInfo(socketAddress, identifier, space);
        slaveInfoMap.put(identifier, slaveInfo);
        slaveStatusMap.put(identifier, new SlaveStatus(slaveInfo));

        return true;
    }

    public int countEntries(){
        return slaveInfoMap.size();
    }

    public AllocationRequest tryAllocate(AllocationRequest allocationsRequest, AllocationResultsBuilder resultsBuilder) {
        AllocationRequest result = allocationsRequest;
        for (var slaveStatus : slaveStatusMap.values()){
            result = slaveStatus.tryAllocate(result, resultsBuilder);
        }
        return result;
    }
}

class SlaveStatus {
    private final SlaveInfo slaveInfo;
    private final Map<String, Map<Integer,Integer>> resourcesRecords; // RscName x (Identifier x Amount)

    public SlaveStatus(SlaveInfo slaveInfo) {
        this.slaveInfo = slaveInfo;
        resourcesRecords = new HashMap<>();

        for (var resource : slaveInfo.getSpace().entrySet()) {
            var identifiers = new HashMap<Integer, Integer>();
            resourcesRecords.put(resource.getKey(), identifiers);
        }
    }

    public AllocationRequest tryAllocate(AllocationRequest allocationRequest, AllocationResultsBuilder resultsBuilder){
        var result = allocationRequest;
        for (var resourceRequest : allocationRequest.getResources().entrySet()) {
            var name = resourceRequest.getKey();
            var requirement = resourceRequest.getValue();
            var free = getResourceFreeSpace(name);
            var resultAmount = Math.min(requirement, free);

            if(resultAmount < 1) continue;

            addResource(name, resultAmount, allocationRequest.getClientIdentifier());
            resultsBuilder.append(name, resultAmount, slaveInfo.getInetSocketAddress());

            result = allocationRequest.reduce(name, resultAmount);
        }

        return result;
    }

    private int getResourceTakenSpace(String rscName){
        var resource = resourcesRecords.getOrDefault(rscName, null);

        if(resource == null) return 0;

        var result = 0;
        for (var takenSpaceById : resource.values()) {
            result += takenSpaceById;
        }
        return result;
    }

    private int getResourceTotalSpace(String rscName){
        return slaveInfo.getSpace().getOrDefault(rscName, 0);
    }

    private int getResourceFreeSpace(String rscName){
        var taken = getResourceTakenSpace(rscName);
        var total = getResourceTotalSpace(rscName);

        return total - taken;
    }

    private void addResource(String rscName, int amount, int identifier){
        var record = resourcesRecords.getOrDefault(rscName, null);

        if(record == null){
            record = new HashMap<>();
            record.put(identifier, amount);
            resourcesRecords.put(rscName, record);
        }else{
            var prev = record.getOrDefault(identifier, 0);
            var newVal = prev + amount;
            record.put(identifier, newVal);
        }
    }
}

class SlaveInfo {
    private InetSocketAddress inetSocketAddress;
    private Map<String, Integer> space;
    private int identifier;

    public SlaveInfo(InetSocketAddress inetSocketAddress, int identifier, Map<String,Integer> space) {
        this.inetSocketAddress = inetSocketAddress;
        this.identifier = identifier;
        this.space = space;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public int getIdentifier() {
        return identifier;
    }

    public Map<String, Integer> getSpace() {
        return space;
    }
}
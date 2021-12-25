package rscnet.logic;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AllocationResultsBuilder {
    private List<Unit> units;
    private boolean failed;

    public AllocationResultsBuilder() {
        this.units = new ArrayList<>(16);
    }

    public void append(String resourceName, int amount, InetSocketAddress socketAddress){
        units.add(new Unit(resourceName, amount, socketAddress));
    }

    public void markFailed(){
        failed = true;
    }

    @Override
    public String toString() {
        if(failed) return "FAILED";

        var strBuilder = new StringBuilder();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if(i != 0) {
                strBuilder.append('\n');
            }
            strBuilder.append(unit.toString());
        }
        return strBuilder.toString();
    }
}

class Unit{
    public final String resourceName;
    public final int amount;
    public final InetSocketAddress socketAddress;

    public Unit(String resourceName, int amount, InetSocketAddress socketAddress) {
        this.resourceName = resourceName;
        this.amount = amount;
        this.socketAddress = socketAddress;
    }

    @Override
    public String toString() {
        return resourceName + ":" + amount + ":" + socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
    }
}
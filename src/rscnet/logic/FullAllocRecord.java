package rscnet.logic;

public class FullAllocRecord {
    public final String resource;
    public final int identifier;
    public final int quantity;

    public FullAllocRecord(String resource, int identifier, int quantity) {
        this.resource = resource;
        this.identifier = identifier;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return resource + '.' + identifier + '.' + quantity;
    }
}

import java.util.HashMap;

public class Tests {
    public static void internalTests(){
        System.out.println("---* TESTS START *---");

        var spaces = new HashMap<String,Integer>();
        spaces.put("A",5);
        var resourceRegistry = new ResourceRegistry(spaces);

        assertInt(2, resourceRegistry.tryAlloc("A", 2), "Alloc1");
        assertInt(2, resourceRegistry.tryAlloc("A", 2), "Alloc2");
        assertInt(1, resourceRegistry.tryAlloc("A", 2), "Alloc3");
        assertInt(0, resourceRegistry.tryAlloc("B", 1), "Alloc4-Neg");

        System.out.println("---* TESTS END *---");
        System.out.println("");
        System.out.println("");
    }

    private static void assertInt(int expected, int actual, String msg){

        var pass = expected == actual;

        if(pass)
            System.out.println(msg + " - PASS");
        else
            System.out.println(msg + " - FAIL: " + expected + " != " + actual);
    }
}

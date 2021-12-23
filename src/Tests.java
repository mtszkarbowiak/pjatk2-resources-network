import java.util.HashMap;

public class Tests {
    public static void internalTests(boolean showPass){
        if(showPass) System.out.println("---* TESTS START *---");

        testResourceRegistry(showPass);

        testAllocationRequest(showPass);

        if(showPass){
            System.out.println("---* TESTS END *---");
            System.out.println();
        }
    }

    private static void testAllocationRequest(boolean showPass) {
        var requestText = "6969 A:420 B:7";
        var requestObj = new AllocationRequest(requestText);

        assertInt(showPass, 6969, requestObj.getClientIdentifier(), "ClientID");
        assertInt(showPass, 420, requestObj.getResources().get("A"), "Rsc_A");
        assertInt(showPass, 7, requestObj.getResources().get("B"), "Rsc_B");
    }

    private static void testResourceRegistry(boolean showPass) {
        var spaces = new HashMap<String,Integer>();
        spaces.put("A",5);
        var resourceRegistry = new ResourceRegistry(spaces);

        assertInt(showPass, 2, resourceRegistry.tryAlloc("A", 61, 2), "Alloc1");
        assertInt(showPass, 2, resourceRegistry.tryAlloc("A", 62, 2), "Alloc2");
        assertInt(showPass, 1, resourceRegistry.tryAlloc("A", 63, 2), "Alloc3");
        assertInt(showPass, 0, resourceRegistry.tryAlloc("B", 64, 1), "Alloc4-Neg");

        assertInt(showPass, 2, resourceRegistry.getValue("A", 62), "Alloc2-Ret");
    }

    private static void assertInt(boolean showPass, int expected, int actual, String msg){

        var pass = expected == actual;

        if(pass) {
            if(showPass) System.out.println(msg + " - PASS");
        }
        else {
            System.out.println(msg + " - FAIL: " + expected + " != " + actual);
        }
    }
}

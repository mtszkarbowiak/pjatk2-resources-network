package rscnet;

public class Tests {
    public static void internalTests(boolean showPass){
        if(showPass) System.out.println("---* TESTS START *---");

        if(showPass){
            System.out.println("---* TESTS END *---");
            System.out.println();
        }
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

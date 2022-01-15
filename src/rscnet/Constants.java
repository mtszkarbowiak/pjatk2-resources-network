package rscnet;

public class Constants {
    public static class App{
        public static final boolean USE_UNRELIABLE_CONNECTION = true;
        public static final int MAX_APP_LIFETIME = 30_000;
        public static final int COMPILATION_NO = 17;
        public static final boolean WAIT_AFTER_TERMINATION = false;
    }

    public static class Async{
        public static final int THREAD_ASYNC_SLEEP_STEP = 50;
    }

    public static class Communication{
        public static final int RECONNECTION_INTERVAL = 2500;
    }

    public static class NetCommands {
        public static final String HEAD_REQUEST = "SHOW_MASTER";
        public static final String HEAD_RESPONSE_ABOUT_MASTER = "ASK_HIM";
        public static final String HEAD_RESPONSE_I_AM_MASTER = "IM_MASTER";
        public static final String HEAD_RESPONSE_FAIL = "UNKNOWN_MASTER";

        public static final String REGISTRATION_REQUEST = "REG_SLAVE";
        public static final String REGISTRATION_RESPONSE_DENY = "REG_SLAVE_DENY";
        public static final String REGISTRATION_RESPONSE_SUCCESS = "REG_SLAVE_OK";

        public static final String TERMINATION_REQUEST = "TERMINATE";
        public static final String COLLAPSE_REQUEST = "COLLAPSE";

        public static final String HL_LINE_REPLACER = ":::";
    }
}

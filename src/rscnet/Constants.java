package rscnet;

public class Constants {
    public static class App{
        public static final boolean USE_UNRELIABLE_CONNECTION = true;
        public static final int MAX_APP_LIFETIME = 30_000;
        public static final int COMPILATION_NO = 20;
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

        public static final String HL_LINE_REPRESENTATION = ":::";
    }

    public static class UnreliableCommunication{
        public static final int SEND_ATTEMPTS = 5;
        public static final int SEND_ATTEMPTS_INTERVAL = 350;
        public static final int RECEIVE_ATTEMPT_INTERVAL = 50;
        public static final int MAX_OUT_TO_IN_PACKET_RATIO = 3;

        public static final String LL_LINE_REPRESENTATION = "::";
        public static final String PROTOCOL_SEPARATOR = "::::";
    }
}

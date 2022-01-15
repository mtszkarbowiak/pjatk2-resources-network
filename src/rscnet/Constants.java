package rscnet;

public class Constants {
    public class App{
        public static final boolean USE_UNRELIABLE_CONNECTION = true;
        public static final int MAX_APP_LIFETIME = 45_000;
        public static final int COMPILATION_NO = 14;
    }

    public class Async{
        public static final int THREAD_ASYNC_SLEEP_STEP = 50;
    }

    public class Communication{
        public static final int RECONNECTION_INTERVAL = 2500;
    }
}

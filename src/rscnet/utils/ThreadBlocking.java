package rscnet.utils;

import rscnet.logging.*;

import static rscnet.Constants.Async.THREAD_ASYNC_SLEEP_STEP;

public class ThreadBlocking {
    @SuppressWarnings("BusyWait")
    public static void wait(ThreadBlocker threadBlocker, Logger logger){
        int interval = 100;
        final int logDisplayNo = 5;
        int totalSleep = 0;
        int logIndex = 1;

        while (threadBlocker.keepBlocking()){
            int currentSleepTime = interval;

            try {
                Thread.sleep(currentSleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            totalSleep += currentSleepTime;
            interval += THREAD_ASYNC_SLEEP_STEP;

            if(logIndex++ % logDisplayNo == 0) {
                if(logger != null)
                    logger.log("Waiting idle for " + (totalSleep / 1000) + " sec.", LogType.Info);
            }
        }
    }
}
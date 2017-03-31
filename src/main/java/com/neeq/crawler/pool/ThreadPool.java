package com.neeq.crawler.pool;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by kidbei on 16/5/7.
 */
public class ThreadPool {

    private static final ForkJoinPool pool = new ForkJoinPool(20);

    private static final ForkJoinPool webClientPool = new ForkJoinPool(1);

    private static final ScheduledExecutorService schedulePool = Executors.newScheduledThreadPool(4);



    public static ForkJoinPool getPool () {

        return pool;
    }


    public static ScheduledExecutorService getSchedulePool() {

        return schedulePool;
    }

    public static ForkJoinPool getWebClientPool() {
        return webClientPool;
    }
}

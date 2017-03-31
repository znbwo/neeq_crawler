package com.neeq.crawler.task;

import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/19.
 */
public class TaskOptions {
    private TimeUnit timeUnit;
    private long    period;

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public TaskOptions setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public long getPeriod() {
        return period;
    }

    public TaskOptions setPeriod(long period) {
        this.period = period;
        return this;
    }
}

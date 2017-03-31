package com.neeq.crawler.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kidbei on 16/5/19.
 */
public class CrawlerOptions {

    private List<CrawlerTask> tasks = new ArrayList<>();
    private int scheduleThreads = 4;

    public CrawlerOptions addTask(CrawlerTask task) {
        this.tasks.add(task);
        return this;
    }

    public List<CrawlerTask> getTasks() {
        return tasks;
    }

    public int getScheduleThreads() {
        return scheduleThreads;
    }

    public CrawlerOptions setScheduleThreads(int scheduleThreads) {
        this.scheduleThreads = scheduleThreads;
        return this;
    }
}

package com.neeq.crawler.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by kidbei on 16/5/19.
 */
public class Crawler {

    private final Logger log = LoggerFactory.getLogger(Crawler.class);

    private CrawlerOptions options;
    private Map<String,CrawlerTaskRunner> runnerMap = new HashMap<>();
    private ScheduledExecutorService scheduledService;

    public Crawler(CrawlerOptions options) {
        this.options = options;
        this.scheduledService = Executors.newScheduledThreadPool(options.getScheduleThreads());
    }



    public void start() {
        List<CrawlerTask> tasks = options.getTasks();
        for (CrawlerTask task : tasks) {
            CrawlerTaskRunner runner = start(task);
            runnerMap.put(task.taskId(),runner);
        }
    }






    public CrawlerTaskRunner start(CrawlerTask task) {
        CrawlerTaskRunner runner = new CrawlerTaskRunner(task,this);
        scheduledService
                .scheduleWithFixedDelay(runner,0,task.options().getPeriod(),task.options().getTimeUnit());
        return runner;
    }



    public void stopTask(String taskId) {
        CrawlerTaskRunner runner = runnerMap.remove(taskId);
        if (runner != null) {
            log.info("stop task {} ......",runner);
            runner.stopTask();
            log.info("task {} is stopped");
        }
    }


    public ScheduledExecutorService getScheduledService() {
        return scheduledService;
    }
}

package com.neeq.crawler.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 重复执行定时任务
 * Created by kidbei on 16/5/23.
 */
public class RunRepeatCrawlerTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RunRepeatCrawlerTask.class);

    private CrawlerTask crawlerTask;
    private Crawler crawler;


    public RunRepeatCrawlerTask(CrawlerTask crawlerTask) {
        this.crawlerTask = crawlerTask;
    }


    public RunRepeatCrawlerTask setCrawler(Crawler crawler) {
        this.crawler = crawler;
        return this;
    }


    @Override
    public void run() {
        crawlerTask.taskAgain();
        log.info("task {} is ready to run again",crawlerTask.taskId());
        crawler.start(crawlerTask);
    }
}

package com.neeq.crawler.task;

import com.neeq.crawler.pool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/19.
 */
public class CrawlerTaskRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskRunner.class);


    private CrawlerTask task;
    private boolean isStop;
    private volatile boolean isRunning;
    private Crawler crawler;


    public CrawlerTaskRunner(CrawlerTask task,Crawler crawler) {
        this.task = task;
        this.crawler = crawler;
        task.preStart();
        log.info("task {} is started",task);
    }



    @Override
    public void run() {
        if (isRunning) {
            log.warn("task {} is running in schedule",task);
            return;
        }
        this.isRunning = true;
        try{
            if (isStop || task.willStop()) {
                log.info("task {} is ready to stop",task);
                task.preStop();
                log.info("task {} is stopped success",task);

                if (task.willStop()) {
                    if (task.repeat()) {
                        log.info("task {} is needed to run repeat,will run again after {} mill",task.taskId(),task.repeatAfterTime());
                        RunRepeatCrawlerTask repeatCrawlerTask = new RunRepeatCrawlerTask(task).setCrawler(crawler);
                        crawler.getScheduledService().schedule(repeatCrawlerTask,task.repeatAfterTime(),TimeUnit.MILLISECONDS);
                    }
                }

                throw new StopException();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("task {} is ready to go next step",task);
                }
                ThreadPool.getPool().execute(() -> {
                    try{
                        task.next();
                    } catch(Exception e) {
                        log.error("task {} execute next step got an error",task.taskId(),e);
                    } finally {
                        this.isRunning = false;
                    }
                });
            }
        } catch(Exception e) {
            if (e instanceof StopException) {
                throw e;
            } else {
                e.printStackTrace();
                log.error("task {} got an error",task,e);
            }
        }
    }


    public synchronized void stopTask() {
        this.isStop = true;
    }


    private class StopException extends RuntimeException{

    }
}

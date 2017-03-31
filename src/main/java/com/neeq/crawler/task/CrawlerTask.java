package com.neeq.crawler.task;

/**
 * Created by kidbei on 16/5/19.
 */
public interface CrawlerTask {

    void preStart();


    void preStop();

    void next();

    String taskId();

    boolean willStop();

    TaskOptions options();

    /**
     * 是否重复执行
     * @return
     */
    boolean repeat();

    /**
     * 重复执行时触发
     */
    void taskAgain();

    /**
     * 在多少秒之后重复执行
     * @return
     */
    long  repeatAfterTime();

    /**
     * 是否使用代理
     * @return
     */
    boolean userProxy();
}

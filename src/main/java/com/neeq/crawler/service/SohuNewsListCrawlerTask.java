package com.neeq.crawler.service;

import com.neeq.crawler.task.TaskOptions;

import java.util.concurrent.TimeUnit;

/**
 *
 * 搜狐新三板资讯
 * Created by kidbei on 16/5/31.
 */
public class SohuNewsListCrawlerTask extends BasicClientCrawlerTask {





    @Override
    public void next() {

    }

    @Override
    public String taskId() {
        return "搜狐新三板资讯";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 5).setTimeUnit(TimeUnit.MILLISECONDS);
    }
}

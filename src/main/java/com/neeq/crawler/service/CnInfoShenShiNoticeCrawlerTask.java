package com.neeq.crawler.service;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;

/**
 * 巨潮深市公告
 * Created by kidbei on 16/7/5.
 */
public class CnInfoShenShiNoticeCrawlerTask extends BasicCnInfoNoticeCrawlerTask {

    public CnInfoShenShiNoticeCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis, fileUploader);
    }

    @Override
    public String getColumn() {
        return "szse";
    }

    @Override
    public String getColumnTitle() {
        return "深市公告";
    }

    @Override
    public String taskId() {
        return "巨潮深市公告";
    }
}

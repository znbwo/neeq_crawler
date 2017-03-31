package com.neeq.crawler.service;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;

/**
 * 巨潮香港股市通告
 * Created by kidbei on 16/7/5.
 */
public class CnInfoHkNoticeCrawlerTask extends BasicCnInfoNoticeCrawlerTask {
    public CnInfoHkNoticeCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis, fileUploader);
    }

    @Override
    public String getColumn() {
        return "hke";
    }

    @Override
    public String getColumnTitle() {
        return "香港市场";
    }

    @Override
    public String taskId() {
        return "巨潮香港股市通告";
    }
}

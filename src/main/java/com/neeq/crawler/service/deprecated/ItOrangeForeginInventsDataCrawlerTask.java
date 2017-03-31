package com.neeq.crawler.service.deprecated;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;

/**
 * IT桔子国外融资数据
 * Created by kidbei on 16/6/7.
 */
public class ItOrangeForeginInventsDataCrawlerTask extends BasicItOrangeInventsCrawlerTask {

    private final String url = "https://www.itjuzi.com/investevents/foreign?page=";

    public ItOrangeForeginInventsDataCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis,fileUploader);
    }

    @Override
    String getBaseUrl() {
        return url;
    }

    @Override
    public String taskId() {
        return "IT桔子国外融资数据";
    }
}

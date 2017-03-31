package com.neeq.crawler.service.deprecated;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;

/**
 *
 * IT桔子国内投资数据
 * Created by kidbei on 16/6/7.
 */
public class ItOrangeInvestEventsDataCrawlerTask extends BasicItOrangeInventsCrawlerTask {

    private final String url_pre = "https://www.itjuzi.com/investevents?page=";


    public ItOrangeInvestEventsDataCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis,fileUploader);
    }

    @Override
    String getBaseUrl() {
        return url_pre;
    }


    @Override
    public String taskId() {
        return "IT桔子国内融资数据";
    }
}

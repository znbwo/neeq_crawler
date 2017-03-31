package com.neeq.crawler.service.deprecated;

import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;

/**
 * IT桔子国外并购数据
 * Created by kidbei on 16/6/7.
 */
public class ItOrangeForeginMergeCrawlerTask extends BasicItOrangeMergeCrawlerTask {

    private final String url = "https://www.itjuzi.com/merger/foreign?page=";


    public ItOrangeForeginMergeCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis,fileUploader);
    }

    @Override
    String getBaseUrl() {
        return url;
    }

    @Override
    public String taskId() {
        return "IT桔子国外并购数据";
    }
}

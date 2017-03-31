package com.neeq.crawler.service.researchReport;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by bj on 16/6/30.
 */
public class HeXunIndustryReportCrawlerTask extends BasicHeXunYanBaoCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(HeXunIndustryReportCrawlerTask.class);
    private final String baseUrl = "http://yanbao.stock.hexun.com/listnews2_1.shtml";
    private final String redisKey = Constant.Redis.RESEARCH_REPORT_HEXUN_QUEUE;
    private final String kafkaTopic = Constant.Topic.INDUSTRY_REPORT_HEXUN_TOPIC;

    public HeXunIndustryReportCrawlerTask(PushQueue pushQueue, FileUploader fileUploader, CoopRedis redis) {
        super.pushQueue = pushQueue;
        super.fileUploader = fileUploader;
        super.redis = redis;
        super.log = log;
    }



    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }

    @Override
    public String taskId() {
        return "和讯行业研报";
    }


    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected String getRedisKey() {
        return redisKey;
    }

    @Override
    protected String getKafkaTopic() {
        return kafkaTopic;
    }
}

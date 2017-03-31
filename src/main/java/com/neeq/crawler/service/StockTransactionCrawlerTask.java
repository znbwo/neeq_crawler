package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.neeq.crawler.Constant;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * neeq系统首页市场总貌/挂牌公司统计数据
 * Created by kidbei on 16/5/25.
 */
public class StockTransactionCrawlerTask extends BasicClientCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(StockTransactionCrawlerTask.class);

    private final String url = "http://www.neeq.com.cn/nqxxController/getMarketData.do?callback=jQuery183015982955490814044_1464144792067";


    private PushQueue pushQueue;

    public StockTransactionCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }


    @Override
    public void next() {
        willStop = true;

        CloseableHttpResponse response = null;
        try{

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            String jsonStr = getForStringPage(get,0);
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));

            JSONArray result = JSON.parseArray(jsonStr);

            if (log.isDebugEnabled()) {
                log.debug("抓到统计数据:{}",result.toJSONString());
            }

            pushQueue.sendToQueue(Constant.Topic.STOCK_DIS_TOPIC,result.toJSONString().getBytes());

        } catch(Exception e) {
            log.error("抓取三板交易统计数据失败",e);
        } finally {
            HttpManager.close(response);
        }
    }



    @Override
    public String taskId() {
        return "三板统计数据";
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 24;
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 5).setTimeUnit(TimeUnit.MILLISECONDS);
    }
}

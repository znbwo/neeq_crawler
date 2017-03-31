package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.neeq.crawler.Constant;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kidbei on 16/5/19.
 */
public class DeletedCompanyCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(DeletedCompanyCrawlerTask.class);

    private final String url_pre = "http://www.neeq.com.cn/nqxxController/nqxx.do?callback=jQuery183006973334994210645_1463637527318&page=";
    private final String url_after = "&typejb=B&xxzqdm=&sortfield=xxzqdm&sorttype=asc&_=1463637527409";

    private PushQueue pushQueue;
    private int pageIndex = 0;
    private int totalPage = -1;


    public DeletedCompanyCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }



    @Override
    public void next() {
        log.info("抓取已退市的公司第{}页",pageIndex);

        errorCount = 0;

        String url = url_pre + pageIndex + url_after;
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);

        down(get);

        pageIndex ++;
        if (pageIndex == totalPage) {
            pageIndex = 0;
        }
    }

    @Override
    public String taskId() {
        return "已退市的公司";
    }



    int errorCount = 0;
    private void down(HttpGet get) {
        try{

            String str = getForStringPage(get,0);

            str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));

            if (totalPage == -1) {
                JSONArray result = JSON.parseArray(str);
                totalPage = result.getJSONObject(0).getInteger("totalPages");
            }

            if (log.isDebugEnabled()) {
                log.debug("抓取退市公司第{}页成功,{}",pageIndex,str);
            }

            pushQueue.sendToQueue(Constant.Topic.DELETE_COMPANY_TOPIC,str.getBytes());



        } catch(Exception e) {
            errorCount ++;
            log.error("抓取第" + pageIndex + "页,失败{}次",errorCount);
            if (errorCount < 3) {
                down(get);
            }
        }
    }
}

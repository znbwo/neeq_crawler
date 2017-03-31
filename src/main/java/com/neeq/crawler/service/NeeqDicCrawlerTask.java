package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.neeq.crawler.Constant;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 数据字典抓取
 * Created by kidbei on 16/5/19.
 */
public class NeeqDicCrawlerTask extends BasicClientCrawlerTask {
    final Logger log = LoggerFactory.getLogger(NeeqDicCrawlerTask.class);


    private PushQueue pushQueue;

    private final String industryUrl = "http://www.neeq.com.cn/listedDictionary/getDic.do?callback=jQuery1830022637747586487067_1463634842582&dictCode=profession_type&_=1463634842697";
    private final String placeUrl = "http://www.neeq.com.cn/listedDictionary/getXxssdq.do?callback=jQuery1830022637747586487067_1463634842583&_=1463634842700";
    private final String zbqsUrl = "http://www.neeq.com.cn/nqxxController/getXxzbqs.do?callback=jQuery1830022637747586487067_1463634842584&_=1463634842703";


    public NeeqDicCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }


    @Override
    public void next() {
        fetchIndustry();
        fetchPlace();
        fetchZbqs();
    }

    @Override
    public String taskId() {
        return "neeq系统数据字典";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 60 * 60 * 24).setTimeUnit(TimeUnit.MILLISECONDS);
    }



    private int industryError = 0;

    /**
     * 抓取行业种类
     */
    private void fetchIndustry() {

        HttpGet get = new HttpGet(industryUrl);
        HttpManager.config(get);

        try{

            String str = getForStringPage(get,0);
            str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));

            JSONArray result = JSON.parseArray(str);

            if (log.isDebugEnabled()) {
                log.debug("抓取到行业种类数据:\n{}",result.toJSONString());
            }
            pushQueue.sendToQueue(Constant.Topic.INDUSTRY_TOPIC,result.toJSONString().getBytes());

        } catch(Exception e) {
            log.error("抓取行业种类失败次数:" + industryError);
            industryError ++;
            if (industryError < 3) {
                fetchIndustry();
            }
        }
    }


    /**
     * 地区数据
     */
    int placeError = 0;
    private void fetchPlace() {

        HttpGet get = new HttpGet(placeUrl);
        HttpManager.config(get);

        try{

            String str = getForStringPage(get,0);
            str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));

            JSONArray result = JSON.parseArray(str);

            if (log.isDebugEnabled()) {
                log.debug("抓取到地区数据:\n{}",result.toJSONString());
            }
            pushQueue.sendToQueue(Constant.Topic.PLACE_TOPIC,result.toJSONString().getBytes());
        } catch(Exception e) {
            placeError ++;
            log.error("抓取地区数据失败:" + placeError);
            if (placeError <3) {
                fetchPlace();
            }
        }
    }


    /**
     * 主办券商数据
     */
    int zbqsError = 0;
    private void fetchZbqs() {
        HttpGet get = new HttpGet(zbqsUrl);
        HttpManager.config(get);

        try{

            String str = getForStringPage(get,0);
            str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));

            JSONArray result = JSON.parseArray(str);

            if (log.isDebugEnabled()) {
                log.debug("抓取到主办券商数据:\n{}",result.toJSONString());
            }
            pushQueue.sendToQueue(Constant.Topic.ZBQS_TOPIC,result.toJSONString().getBytes());
        } catch(Exception e) {
            zbqsError ++;
            log.error("抓取主办券商数据失败:" + zbqsError);
            if (zbqsError <3) {
                fetchZbqs();
            }
        }
    }
}

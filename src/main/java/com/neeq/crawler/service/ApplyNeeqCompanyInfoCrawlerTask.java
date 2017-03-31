package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *
 * 全国股份转让系统在审申请挂牌企业基本情况表
 * Created by kidbei on 16/6/2.
 */
public class ApplyNeeqCompanyInfoCrawlerTask extends BasicClientCrawlerTask {


    private final Logger log = LoggerFactory.getLogger(ApplyNeeqCompanyInfoCrawlerTask.class);


    final String URL = "http://www.neeq.com.cn/disclosureInfoController/infoResult.do?callback=jQuery18307193577939435725_1464863999825";
    final String DOMAIN = "http://www.neeq.com.cn";


    private PushQueue pushQueue;
    private FileUploader fileUploader;



    public ApplyNeeqCompanyInfoCrawlerTask(FileUploader fileUploader,PushQueue pushQueue) {
        this.fileUploader = fileUploader;
        this.pushQueue = pushQueue;
    }



    @Override
    public void next() {
        try{

            HttpPost post = new HttpPost(URL);
            List<NameValuePair> params = new ArrayList<NameValuePair>(){
                {
                    this.add(new BasicNameValuePair("disclosureType","9"));
                    this.add(new BasicNameValuePair("page","0"));
                }
            };
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpManager.config(post);

            String jsonStr = getForStringPage(post,0);
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1,jsonStr.lastIndexOf(")"));
            JSONArray result = JSONArray.parseArray(jsonStr);

            JSONObject info = result.getJSONObject(0).getJSONObject("disclosureInfo");
            String url = info.getString("destFilePath");
            url = DOMAIN + url;

            InputStream is = new URL(url).openStream();

            url = fileUploader.upload(is, UUID.randomUUID().toString()+".xls",0);

            String pubdate = info.getString("publishDate");

            JSONObject infoResult = new JSONObject();
            infoResult.put("fileUrl",url);
            infoResult.put("publishDate",pubdate);


            pushQueue.sendToQueue(Constant.Topic.APPLY_COMPANY_INFO_TOPIC,infoResult.toJSONString().getBytes());

            log.info("抓取全国股份转让系统在审申请挂牌企业基本情况表成功");

        } catch(Exception e) {
            log.error("抓取全国股份转让系统在审申请挂牌企业基本情况表失败",e);
        }
    }

    @Override
    public String taskId() {
        return "全国股份转让系统在审申请挂牌企业基本情况表";
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 20).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public long repeatAfterTime() {
        return  1000 * 60 * 60 * 24;
    }
}

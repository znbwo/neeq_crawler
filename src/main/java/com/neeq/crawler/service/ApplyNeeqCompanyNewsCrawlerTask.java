package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
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
import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 审查公开信息列表
 * Created by kidbei on 16/6/3.
 */
public class ApplyNeeqCompanyNewsCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ApplyNeeqCompanyNewsCrawlerTask.class);

    private final String FETCH_URL = "http://www.neeq.com.cn/disclosureInfoController/infoResult.do?callback=jQuery183049992579385869573_1464936617070";
    private final String DOAMIN = "http://www.neeq.com.cn";

    private int page = 1;
    private int totalPage = -1;
    private int repeatCount = 0;
    private boolean fullCrawed = false;



    private CoopRedis redis;
    private FileUploader fileUploader;
    private PushQueue pushQueue;


    public ApplyNeeqCompanyNewsCrawlerTask(PushQueue pushQueue,FileUploader fileUploader,CoopRedis redis){
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
        this.redis = redis;
    }


    @Override
    public void next() {
        try{

            if ((page >= totalPage && totalPage != -1) || (repeatCount > 10 && fullCrawed)) {
                willStop = true;
                fullCrawed = true;
                page = 1;
                totalPage = -1;
                return;
            }


            HttpPost post = new HttpPost(FETCH_URL);
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


            JSONArray list = result.getJSONObject(0).getJSONObject("listInfo").getJSONArray("content");
            if (totalPage == -1) {
                totalPage = result.getJSONObject(0).getJSONObject("listInfo").getInteger("totalPages");
            }


            if (list!= null && list.size() > 0) {

                for (int i = 0 ; i <  list.size(); i ++) {
                    JSONObject cp = list.getJSONObject(i);

                    String title = cp.getString("disclosureTitle");
                    String publishDate = cp.getString("publishDate");
                    String md5 = Md5Helper.getMd5(title + publishDate);

                    if (isExist(md5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("公告信息已经存在:{}",title);
                        }
                        repeatCount += 1;
                        continue;
                    }


                    if (title.indexOf("公司") == -1) {
                        throw new NullPointerException("非正规公司名开头");
                    } else {
                        String cpName = title.substring(0,title.indexOf("公司") + 2);
                        cp.put("companyName",cpName);

                        String fileUrl = DOAMIN + cp.getString("destFilePath");

                        InputStream is = new URL(fileUrl).openStream();
                        fileUrl = fileUploader.upload(is,md5 + ".pdf",0);

                        cp.put("disclosureTitle",fileUrl);

                        if (log.isDebugEnabled()) {
                            log.debug("抓取公司公告信息:{} 成功",title);
                        }

                        pushQueue.sendToQueue(Constant.Topic.APPLY_COMPANY_NEWS_TOPIC,cp.toJSONString().getBytes());
                        redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE,md5);
                    }
                }
            }


        } catch(Exception e) {
            log.error("抓取公开信息列表失败,page={}",page,e);
        }

        page += 1;
    }


    private boolean isExist(String key) {
        Jedis jedis = null;
        try{

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE,key);
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    @Override
    public String taskId() {
        return "审查公开信息列表";
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 6;
    }


}

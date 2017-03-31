package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
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
 *
 * 公司公告抓取
 * Created by kidbei on 16/5/23.
 */
public class NoticeCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(NoticeCrawlerTask.class);

    private final String url = "http://www.neeq.com.cn/disclosureInfoController/infoResult.do?callback=";


    private int page = 0;
    private int totalPage = -1;

    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;

    private final String CHECK_REPEAT_QUEUE_SET = "notice_repeat_check_set";



    public NoticeCrawlerTask(PushQueue pushQueue,CoopRedis redis,FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }




    @Override
    public void next() {
        if (page == totalPage) {
            page = 0;
            totalPage = -1;
            this.willStop = true;
        }

        try{
            HttpPost post = new HttpPost(url);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("disclosureType","5"));
            params.add(new BasicNameValuePair("page",page + ""));
            params.add(new BasicNameValuePair("companyCd",""));
            params.add(new BasicNameValuePair("isNewThree","1"));
            params.add(new BasicNameValuePair("startTime",""));
            params.add(new BasicNameValuePair("endTime",""));
            params.add(new BasicNameValuePair("keyword","关键字"));

            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

            down(post);

            page ++;

        } catch(Exception e) {
            e.printStackTrace();
        }
    }



    private boolean isExist(String key) {
        Jedis jedis = null;
        try{

            jedis = redis.getJedis();

            return jedis.sismember(CHECK_REPEAT_QUEUE_SET,key);
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }




    private void down(HttpPost post) {
        try{
            HttpManager.config(post);

            String jsonStr = getForStringPage(post,0);
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1,jsonStr.lastIndexOf(")"));


            JSONArray result = JSON.parseArray(jsonStr);
            if (totalPage == -1) {
                totalPage = result.getJSONObject(0).getJSONObject("listInfo").getInteger("totalPages");
            }


            JSONArray list = result.getJSONObject(0).getJSONObject("listInfo").getJSONArray("content");
            JSONArray newList = new JSONArray();
            if (list.size() > 0) {
                for (int i = 0 ; i <  list.size(); i ++) {
                    JSONObject info = list.getJSONObject(i);
                    String companyCode = info.getString("companyCd");
                    String title = info.getString("disclosureTitle");
                    String publishDate = info.getString("publishDate");
                    String md5 = Md5Helper.getMd5(title + publishDate);
                    info.put("titleMd5",md5);

                    if (isExist(md5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("notice {} is exists",title);
                        }
                        continue;
                    }

                    String fileUrl = info.getString("destFilePath");
                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        fileUrl = "http://www.neeq.com.cn/" + fileUrl;
                    } else {
                        log.warn("no file url,company code = {}",companyCode);
                        continue;
                    }

                    if ("pdf".equals(info.getString("fileExt"))) {
                        //下载文件
                        InputStream is = new URL(fileUrl).openStream();
                        String url = fileUploader.upload(is,md5 + ".pdf",0);
                        if (log.isDebugEnabled()) {
                            log.debug("down an save file success,url={},localUrl={}",fileUrl,url);
                        }
                        info.put("destFilePath",url);
                    } else {
                        log.warn("not support fileExt : {}",info.getString("fileExt"));
                        continue;
                    }

                    redis.insertToSet(CHECK_REPEAT_QUEUE_SET,md5);
                    newList.add(info);
                }

                if (newList.size() > 0) {
                    pushQueue.sendToQueue(Constant.Topic.NOTICE_TOPIC,newList.toJSONString().getBytes());
                }
            }

        } catch(Exception e) {
            log.warn("down error ",e);
        }
    }



    @Override
    public String taskId() {
        return "公司公告";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 5).setTimeUnit(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean repeat() {
        return true;
    }



    //12小时之后重新抓取
    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 24;
    }


}

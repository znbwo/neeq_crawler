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
import com.neeq.crawler.tool.RedisHelper;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *
 * 巨潮的公告
 * Created by kidbei on 16/7/5.
 */
public abstract class BasicCnInfoNoticeCrawlerTask extends BasicClientCrawlerTask{

    private final Logger log = LoggerFactory.getLogger(BasicCnInfoNoticeCrawlerTask.class);

    private final String POST_URL = "http://www.cninfo.com.cn/cninfo-new/disclosure/szse_latest";
    private final String BASE_URL = "http://www.cninfo.com.cn/";
    private final String REPEATE_KEY = "cninfo_notice_repeat";

    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;

    private boolean hasMore = true;
    private int page = 1;
    private int repeatCount = 0;
    private boolean fullCrawed = false;


    public BasicCnInfoNoticeCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }



    @Override
    public void next() {
        try{

            if (!hasMore || (repeatCount >= 10 && fullCrawed) ) {
                willStop = true;
                page = 1;
                hasMore = true;
                fullCrawed = true;
                repeatCount = 0;
                return;
            }

            HttpPost post = new HttpPost(POST_URL);
            HttpManager.config(post);


            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(){
                {
                    this.add(new BasicNameValuePair("stock",""));
                    this.add(new BasicNameValuePair("searchkey",""));
                    this.add(new BasicNameValuePair("plate",""));
                    this.add(new BasicNameValuePair("category",""));
                    this.add(new BasicNameValuePair("trade",""));
//                    this.add(new BasicNameValuePair("column","szse"));
                    this.add(new BasicNameValuePair("column",getColumn()));
//                    this.add(new BasicNameValuePair("columnTitle","深市公告"));
                    this.add(new BasicNameValuePair("columnTitle",getColumnTitle()));
                    this.add(new BasicNameValuePair("pageNum",page + ""));
                    this.add(new BasicNameValuePair("pageSize","30"));
                    this.add(new BasicNameValuePair("tabName","latest"));
                    this.add(new BasicNameValuePair("sortName",""));
                    this.add(new BasicNameValuePair("sortType",""));
                    this.add(new BasicNameValuePair("limit",""));
                    this.add(new BasicNameValuePair("showTitle",""));
                    this.add(new BasicNameValuePair("setDate","请选择日期"));
                }
            };
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

            String jsonStr = getForStringPage(post,0);

            JSONObject result = JSONObject.parseObject(jsonStr);

            hasMore = result.getBoolean("hasMore");

            JSONArray list = result.getJSONArray("classifiedAnnouncements");
            if (list != null && list.size() > 0) {
                for (int i = 0 ; i <  list.size(); i ++) {
                    JSONArray itemArr = list.getJSONArray(i);
                    for (int j = 0 ; j <  itemArr.size(); j ++) {
                        JSONObject item = itemArr.getJSONObject(j);

                        String fileUrl = item.getString("adjunctUrl");
                        if (fileUrl == null) {
                            continue;
                        }

                        String code = item.getString("secCode");
                        String title = item.getString("announcementTitle");
                        String md5 = Md5Helper.getMd5(code + title);

                        if (RedisHelper.existInSet(redis,REPEATE_KEY,md5)) {
                            if (log.isDebugEnabled()) {
                                log.debug("item repeat,code={},title={}",code,title);
                            }
                            repeatCount += 1;
                            continue;
                        }

                        try{
                            uploadFile(item, fileUrl);
                        } catch(Exception e) {
                            log.error("上传文件失败,url={}",fileUrl,e);
                        }

                        pushQueue.sendToQueue(Constant.Topic.CNINFO_NOTICE_TOPIC,item.toJSONString().getBytes());
                        redis.insertToSet(REPEATE_KEY,md5);
                        if (log.isDebugEnabled()) {
                            log.debug("抓取到公告:{}",item.toJSONString());
                        }
                    }


                }
            }


        } catch(Exception e) {
            log.error("获取列表失败,URL={}",POST_URL,e);
        }

        page += 1;
    }

    private void uploadFile(JSONObject item, String fileUrl) throws IOException {
        fileUrl = BASE_URL + fileUrl;
        InputStream is = new URL(fileUrl).openStream();
        fileUrl = fileUploader.upload(is, UUID.randomUUID().toString()+".pdf",0);
        item.put("adjunctUrl",fileUrl);

        if (log.isDebugEnabled()) {
            log.debug("抓取到附件:{}",fileUrl);
        }
    }


    public abstract String getColumn();

    public abstract String getColumnTitle();


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 20).setTimeUnit(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 24;
    }
}

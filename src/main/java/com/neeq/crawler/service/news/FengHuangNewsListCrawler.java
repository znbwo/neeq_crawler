package com.neeq.crawler.service.news;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * 凤凰新三板
 * Created by kidbei on 16/5/31.
 */
public class FengHuangNewsListCrawler extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(FengHuangNewsListCrawler.class);

    private final String url_pre = "http://finance.ifeng.com/cmppdyn/820/902/";
    private final String url_after = "/dynlist.html";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;
    private boolean hasNext = true;


    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;

    public FengHuangNewsListCrawler(PushQueue pushQueue,CoopRedis redis,FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
        this.redis = redis;
    }


    @Override
    public void next() {
        try{

            if ((repeatCount > 10 || !hasNext) && fullCrawed) {
                willStop = true;
                page = 1;
                repeatCount = 0;
                hasNext = true;
                return;
            }


            String url = url_pre + page + url_after;
            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0);



            List<Element> lis = new ArrayList<>();
            Elements uls = doc.select(".list03 ul");
            for (Element ul : uls) {
                lis.addAll(ul.children());
            }

            if (lis.size() < 40) {
                hasNext = false;
                fullCrawed = true;
            }

            for (Element li : lis) {
                try{
                    String title = li.child(0).child(0).text();
                    String titleMd5 = Md5Helper.getMd5(title);
                    if (isExist(titleMd5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("news {} is exist",title);
                        }
                        repeatCount += 1;
                        continue;
                    }

                    String cts = li.child(1).text();
                    String itemUrl = li.child(0).child(0).attr("href");

                    HttpGet itemGet = new HttpGet(itemUrl);
                    HttpManager.config(itemGet);

                    Document atDoc = getForDocPage(itemGet,0);
                    replaceImage(atDoc,fileUploader);
                    String content = atDoc.getElementById("main_content").html();
                    String source = atDoc.select("span[itemprop='publisher']").text();

                    JSONObject itemResult = new JSONObject();
                    itemResult.put("source", source);
                    itemResult.put("from", taskId());
                    itemResult.put("title",title);
                    itemResult.put("cts",cts);
                    itemResult.put("about", "");
                    itemResult.put("content",content);
                    itemResult.put("keywords",new JSONArray());
                    itemResult.put("localImgPath", "");
                    pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC,itemResult.toJSONString().getBytes());
                    redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE,titleMd5);

                    if (log.isDebugEnabled()) {
                        log.debug("saved news:{}",title);
                    }
                } catch(Exception e) {
                    log.warn("抓取详情失败",e);
                }
            }
        } catch(Exception e) {
            log.error("抓取凤凰新三板错误,page={}",page,e);
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
        return "凤凰新三板";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 6;
    }
}

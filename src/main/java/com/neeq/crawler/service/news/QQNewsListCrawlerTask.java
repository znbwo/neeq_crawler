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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯新三板资讯
 * Created by kidbei on 16/5/31.
 */
public class QQNewsListCrawlerTask extends BasicClientCrawlerTask {


    private final Logger log = LoggerFactory.getLogger(QQNewsListCrawlerTask.class);

    private final String url_pre = "http://finance.qq.com/c/xsbdt_";
    private final String domain = "http://finance.qq.com";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    private PushQueue pushQueue;
    private FileUploader fileUploader;
    private CoopRedis redis;


    public QQNewsListCrawlerTask(PushQueue pushQueue, FileUploader fileUploader, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
        this.redis = redis;
    }


    @Override
    public void next() {
        try {

            String url = url_pre + page + ".htm?0." + System.currentTimeMillis();
            HttpGet get = new HttpGet(url);
            HttpManager.config(get);


            Document doc = getForDocPage(get, 0, "gb2312");
            if (doc == null) {
                willStop = true;
                page = 1;
                fullCrawed = true;
                repeatCount = 0;
                return;
            }


            if (repeatCount > 10 && fullCrawed) {
                willStop = true;
                page = 1;
                fullCrawed = true;
                repeatCount = 0;
                return;
            }


            Elements list = doc.getElementsByClass("Q-tpWrap");
            for (Element item : list) {
                try {
                    String title = item.child(0).child(0).text();
                    String titleMd5 = Md5Helper.getMd5(title);
                    if (isExist(titleMd5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("news {} is exist", title);
                        }
                        repeatCount += 1;
                        continue;
                    }


                    String itemUrl = domain + item.child(0).child(0).attr("href");
                    HttpGet atGet = new HttpGet(itemUrl);
                    HttpManager.config(atGet);
                    Document atDoc = getForDocPage(atGet, 0, "gb2312");
                    replaceImage(atDoc, fileUploader);
                    String content = atDoc.getElementById("Cnt-Main-Article-QQ").html();
                    String source = atDoc.select("span.where.color-a-1").text();
                    String author = atDoc.select("span.auth.color-a-3").text();

                    String cts = atDoc.select(".pubTime.article-time").text();
                    JSONObject itemResult = new JSONObject();
                    itemResult.put("source", source);
                    itemResult.put("from", taskId());
                    itemResult.put("author", author);
                    itemResult.put("title", title);
                    itemResult.put("cts", cts);
                    itemResult.put("about", "");
                    itemResult.put("content", content);
                    itemResult.put("keywords", new JSONArray());
                    itemResult.put("localImgPath", "");

                    pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, itemResult.toJSONString().getBytes());
                    redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

                    if (log.isDebugEnabled()) {
                        log.debug("saved news:{}", title);
                    }
                } catch (Exception e) {
                    log.error("抓取qq三板资讯详情失败", e);
                }
            }


        } catch (Exception e) {
            log.error("抓取qq新三板资讯失败,page={}", page, e);
            willStop = true;
            page = 1;
            fullCrawed = true;
            repeatCount = 0;
        }

        page += 1;
    }


    private String formatCts(String ctsStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH: mm");
        Date date = sdf.parse(ctsStr);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sdf.format(date);
    }


    private boolean isExist(String key) {
        Jedis jedis = null;
        try {

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    @Override
    public String taskId() {
        return "腾讯新三板资讯";
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 6;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }
}

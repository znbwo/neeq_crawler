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

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 挖贝网资讯
 * Created by kidbei on 16/5/31.
 */
public class WaBeiNewsListCrawlerTask extends BasicClientCrawlerTask {


    private final Logger log = LoggerFactory.getLogger(WaBeiNewsListCrawlerTask.class);

    private final String url_pre = "http://www.wabei.cn/indus/";
    private final String url_after = ".html";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;

    public WaBeiNewsListCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }


    @Override
    public void next() {

        try {


            String url = url_pre + page + url_after;

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get, 0);

            Element a = doc.select("a.next.disable").first();
            if (a != null) {
                log.info("抓取挖贝资讯完成,page={}", page);
                willStop = true;
                page = 1;
                fullCrawed = true;
                repeatCount = 0;
            }


            if (repeatCount > 10 && fullCrawed) {
                willStop = true;
                page = 1;
                repeatCount = 0;
                return;
            }


            Elements list = doc.select(".section-news-list .item-subject");
            for (Element item : list) {
                try {
                    String title = item.select(".title a").text();
                    String titleMd5 = Md5Helper.getMd5(title);
                    if (isExist(titleMd5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("news {} is exist", title);
                        }
                        repeatCount += 1;
                        continue;
                    }

                    String createTimeStr = item.select(".attr-time").text();
                    String cts = formatCts(createTimeStr);

                    Elements tags = item.select(".tag");
                    String about = item.select("p.desc").text();
                    String imghref = item.select("a.img").attr("href");
                    String localImgPath = "";
                    if (!imghref.equals("")) {
                        localImgPath = fileUploader.upload(new URL(imghref).openStream(), titleMd5, 0);
                    }

                    String itemUrl = item.select(".title a").attr("href");
                    HttpGet atGet = new HttpGet(itemUrl);
                    HttpManager.config(atGet);
                    Document atItem = getForDocPage(atGet, 0);

                    replaceImage(atItem, fileUploader);

                    String content = atItem.getElementsByClass("subject-content").html();

                    String source = atItem.select("div.subject div.attr span.source").text();
                    String author = atItem.select("div.subject div.attr span.author").text();
                    JSONObject itemResult = new JSONObject();
                    itemResult.put("source", source);
                    itemResult.put("from", taskId());
                    itemResult.put("cts", cts);
                    itemResult.put("author", author);
                    itemResult.put("title", title);
                    itemResult.put("about", about);
                    itemResult.put("content", content);
                    itemResult.put("localImgPath", localImgPath);
                    if (tags != null && tags.size() > 0) {
                        JSONArray arr = tags.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                        itemResult.put("keywords", arr);
                    }

                    pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, itemResult.toJSONString().getBytes());
                    redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

                    if (log.isDebugEnabled()) {
                        log.debug("saved news:{}", title);
                    }
                } catch (Exception e) {
                    log.error("抓取挖贝资讯失败", e);
                }
            }

        } catch (Exception e) {
            log.error("抓取挖贝列表失败,page={}", page, e);
        }

        page += 1;
    }


    private String formatCts(String ctsStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
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
        return "挖贝网资讯";
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
        return 1000 * 60 * 60 * 12;
    }
}


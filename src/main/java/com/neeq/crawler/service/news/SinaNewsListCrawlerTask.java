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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 新浪三板要闻
 * Created by kidbei on 16/5/30.
 */
public class SinaNewsListCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(SinaNewsListCrawlerTask.class);

    private final String url_pre = "http://api.roll.news.sina.com.cn/zt_list?channel=finance&cat_1=zq1&cat_2=sbsc&show_ext=1&tag=1&callback=jQuery17205086987940968887_1464580837869&show_num=10&page=";
    private final String url_after = "&_=1464580846107";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;


    public SinaNewsListCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
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

            String jsonStr = getForStringPage(get, 0);
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));


            JSONObject result = JSONObject.parseObject(jsonStr);
            JSONArray list = result.getJSONObject("result").getJSONArray("data");

            if (list.size() == 0) {
                willStop = true;
                page = 1;
                repeatCount = 0;
                fullCrawed = true;
                return;
            }

            if (repeatCount > 5 && fullCrawed) {
                willStop = true;
                repeatCount = 0;
                page = 1;
                return;
            }


            for (int i = 0; i < list.size(); i++) {
                try {
                    JSONObject item = list.getJSONObject(i);

                    String title = item.getString("title");
                    String itemUrl = item.getString("url");

                    String titleMd5 = Md5Helper.getMd5(title);
                    if (isExist(titleMd5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("news {} is exist", title);
                        }
                        repeatCount += 1;
                        continue;
                    }


                    String createTime = item.getString("createtime");
                    String keywordsStr = item.getString("keywords");
                    String cts = format(createTime);
                    JSONArray keywords = formatKeywords(keywordsStr);

                    HttpGet articleGet = new HttpGet(itemUrl);
                    HttpManager.config(articleGet);

                    Document doc = getForDocPage(articleGet, 0);
                    replaceImage(doc, fileUploader);
                    String source = "";
                    String author = "";
                    String s = doc.select("div#artibody p").first().text();
                    if ((s.contains("来源：") || s.contains("记者") || s.contains("公众号")) && s.length() < 20) {
                        author = source = s.replaceAll("来源：", "");
                    } else {
                        source = doc.select("span.time-source").text().split(" ")[1];
                    }

                    String content = doc.getElementById("artibody").html();

                    JSONObject itemResult = new JSONObject();
                    itemResult.put("source", source);
                    itemResult.put("author", author);
                    itemResult.put("from", taskId());
                    itemResult.put("title", title);
                    itemResult.put("cts", cts);
                    itemResult.put("keywords", keywords);
                    itemResult.put("about", "");
                    itemResult.put("content", content);
                    itemResult.put("localImgPath", "");

                    pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, itemResult.toJSONString().getBytes());
                    redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

                    if (log.isDebugEnabled()) {
                        log.debug("saved news:{}", title);
                    }
                } catch (Exception e) {
                    log.error("craw page error", e);
                }
            }

        } catch (Exception e) {
            log.error("抓取新浪新三板新闻失败,page = {}", page, e);
        }

        page += 1;
    }


    private String format(String str) {
        Date date = new Date(Long.valueOf(str));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }


    private JSONArray formatKeywords(String keyStr) {
        if (keyStr == null || keyStr.equals("")) {
            return new JSONArray();
        }

        String[] a = keyStr.split(",");
        JSONArray list = new JSONArray();
        for (int i = 0; i < a.length; i++) {
            list.add(a[i]);
        }
        return list;
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
        return "新浪三板要闻";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 3).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 5;
    }
}

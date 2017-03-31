package com.neeq.crawler.service.news;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
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
 * 中金在线新三板要闻
 * Created by kidbei on 16/5/23.
 */
public class ZhongJinZaiXianNewsCrawlerTask extends BasicClientCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(ZhongJinZaiXianNewsCrawlerTask.class);

    final String BASE_URL = "http://sbsc.stock.cnfol.com/xsbyw/";
    String nextUrl = null;

    private boolean indexed = false;

    private int repeatCount = 0;

    private PushQueue pushQueue;
    private CoopRedis redis;


    public ZhongJinZaiXianNewsCrawlerTask(PushQueue pushQueue, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.redis = redis;
    }


    @Override
    public void next() {

        if (repeatCount > 10) {
            willStop = true;
            indexed = false;
            log.info("repeat bigger than {},will stop to craw", repeatCount);
            return;
        }

        if (!indexed) {
            HttpGet get = new HttpGet(BASE_URL);
            HttpManager.config(get);
            crawPage(get);
        } else {
            if (nextUrl != null) {
                HttpGet get = new HttpGet(nextUrl);
                HttpManager.config(get);
                crawPage(get);
            } else {
                willStop = true;
                indexed = false;
                return;
            }
        }
    }


    private void crawPage(HttpGet get) {

        if (log.isDebugEnabled()) {
            log.debug("抓取地址:{} 的列表", get.getURI());
        }

        try {

            String html = getForStringPage(get, 0);
            Document doc = Jsoup.parse(html);


            List<String> infoUrls = new ArrayList<>();
            Elements uls = doc.select(".NewsLstItem");
            if (uls != null && uls.size() > 0) {
                for (Element ul : uls) {
                    Elements lis = ul.getElementsByTag("li");
                    for (Element li : lis) {
                        String href = li.getElementsByTag("a").first().attr("href");
                        if (href != null) {
                            infoUrls.add(href);
                        }
                    }
                }
            }

            //获取下一页的地址
            Element nextTag = doc.select(".NewsLstPage .BtPA").first();
            if (nextTag != null) {
                nextUrl = BASE_URL + nextTag.attr("href");
                if (log.isDebugEnabled()) {
                    log.debug("next page url : {}", nextUrl);
                }
            } else {
                nextUrl = null;
            }


            if (!infoUrls.isEmpty()) {
                log.info("craw info page for urls:{}", infoUrls);

                for (String infoUrl : infoUrls) {
                    HttpGet infoGet = new HttpGet(infoUrl);
                    HttpManager.config(infoGet);
                    crawInfoPage(infoGet);
                }
            }

            indexed = true;

        } catch (Exception e) {
            log.warn("down error", e);
        }
    }


    private void crawInfoPage(HttpGet get) {

        try {
            String html = getForStringPage(get, 0);
            Document doc = Jsoup.parse(html);
            String source = doc.select("div.Subtitle span#source_baidu").text().replaceAll("作者：", "");
            String author = doc.select("div.Subtitle span#author_baidu").text().replaceAll("来源：", "");
            String title = doc.getElementById("Title").text();
            String cts = doc.getElementById("pubtime_baidu").text();
            String content = doc.getElementById("Content").html();
            String titleMd5 = Md5Helper.getMd5(title);
            if (isExist(titleMd5)) {
                if (log.isDebugEnabled()) {
                    log.debug("news {} is exist", title);
                }
                repeatCount += 1;
                return;
            }

            JSONObject result = new JSONObject();
            result.put("from", taskId());
            result.put("source", source);
            result.put("author", author);
            result.put("title", title);
            result.put("about", "");
            result.put("cts", cts);
            result.put("content", content);
            result.put("keywords", new JSONArray());
            result.put("localImgPath", "");

            pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, result.toJSONString().getBytes());
            redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

            if (log.isDebugEnabled()) {
                log.debug("saved news:{}", title);
            }
        } catch (Exception e) {
            log.warn("down error ", e);
        }
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
        return "中金在线";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 5).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }


    //2小时
    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 6;
    }


}

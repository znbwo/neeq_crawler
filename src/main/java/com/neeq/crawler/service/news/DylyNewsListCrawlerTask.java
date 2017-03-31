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
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 第一路演新三板资讯
 * Created by kidbei on 16/5/30.
 */
public class DylyNewsListCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(DylyNewsListCrawlerTask.class);

    private final String url = "http://news.dyly.com/getAppNewsList.do";
    private final String article_url_pre = "http://news.dyly.com/news/detail/";
    private final String article_url_after = ".html?t=p";

    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;

    private int page = 1;
    private int repeatCount = 0;
    private boolean fullCrawed = false;

    public DylyNewsListCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }


    @Override
    public void next() {

        if (repeatCount > 5 && fullCrawed) {
            willStop = true;
            page = 1;
            repeatCount = 0;
            return;
        }

        try {
            HttpPost post = new HttpPost(url);
            List<NameValuePair> params = new ArrayList<NameValuePair>() {
                {
                    this.add(new BasicNameValuePair("ajax", "ajax"));
                    this.add(new BasicNameValuePair("type", "news_primary"));
                    this.add(new BasicNameValuePair("loginMethod", "wap"));
                    this.add(new BasicNameValuePair("searchType", ""));
                    this.add(new BasicNameValuePair("pageNo", "" + page));
                }
            };
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpManager.config(post);

            String jsonStr = getForStringPage(post, 0);
            JSONObject result = JSONObject.parseObject(jsonStr);

            JSONArray list = result.getJSONArray("newsContent");
            if (list.size() == 0) {
                willStop = true;
                page = 1;
                repeatCount = 0;
                fullCrawed = true;
                return;
            }

            for (int i = 0; i < list.size(); i++) {
                try {
                    JSONObject item = list.getJSONObject(i);
                    String id = item.getString("objectId");
                    String itemUrl = article_url_pre + id + article_url_after;

                    HttpGet get = new HttpGet(itemUrl);
                    HttpManager.config(get);

                    Document doc = getForDocPage(get, 0);

                    String title = doc.select("header.sec-art-left-head").first().text();
                    String titleMd5 = Md5Helper.getMd5(title);
                    String cts = item.getString("showDate");
                    String source = item.getString("newsSource");

                    String content = doc.select(".sec-art-left").first().child(2).html();
                    String about = content.substring(0, content.indexOf("</p>\n"));
                    if (isExist(titleMd5)) {
                        if (log.isDebugEnabled()) {
                            log.debug("news {} is exist", title);
                        }
                        repeatCount += 1;
                        continue;
                    }

                    String bigPicPath_qn = item.getString("objectPicPath_qn");
                    int beginIndex = bigPicPath_qn.lastIndexOf(".");
                    String imageName = bigPicPath_qn.substring(beginIndex);
                    String localImgPath = fileUploader.upload(new URL(bigPicPath_qn).openStream(), titleMd5 + imageName);

                    replaceImage(doc, fileUploader);

                    JSONObject itemResult = new JSONObject();
                    itemResult.put("source", source);
                    itemResult.put("from", taskId());
                    itemResult.put("title", title);
                    itemResult.put("cts", cts);
                    itemResult.put("about", about);
                    itemResult.put("keywords", item.getJSONArray("keyWords"));
                    itemResult.put("localImgPath", localImgPath);
                    itemResult.put("content", content);

                    pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, itemResult.toJSONString().getBytes());
                    redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

                    if (log.isDebugEnabled()) {
                        log.debug("saved news:{}", title);
                    }
                } catch (Exception e) {
                    log.warn("fetch failed", e);
                }
            }

        } catch (Exception e) {
            log.error("抓取第一路演资讯失败", e);
        }

        page++;
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
        return "第一路演资讯";
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 3).setTimeUnit(TimeUnit.MILLISECONDS);
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 2;
    }
}

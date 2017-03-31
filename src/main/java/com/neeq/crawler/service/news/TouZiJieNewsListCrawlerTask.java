package com.neeq.crawler.service.news;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.mail.SendBy163Mail;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 投资界新闻列表
 * Created by kidbei on 16/6/6.
 */
public class TouZiJieNewsListCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(TouZiJieNewsListCrawlerTask.class);


    private int page = 1;

    private final String BASE_URL = "http://www.pedaily.cn/top/handlers/Handler.ashx?action=newslist-all&p=$page$&url=http://www.pedaily.cn/top/newslist.aspx?c=all/";
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;


    public TouZiJieNewsListCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }


    @Override
    public void next() {
        try {
            HttpGet post = new HttpGet(BASE_URL.replace("$page$", page + ""));
            HttpManager.config(post);
            Document doc = getForDocPage(post, 0);
            if (doc == null) {
                toStop();
                return;
            }

            Elements lis = doc.select("li");
            if (lis != null && !lis.isEmpty()) {
                String itemUrl = "";
                for (Element li : lis) {
                    try {
                        String title = li.getElementsByClass("title").first().text();
                        String titleMd5 = Md5Helper.getMd5(title);

                        if (isExist(titleMd5)) {
                            if (log.isDebugEnabled()) {
                                log.debug("news {} is exist", title);
                            }
                            repeatCount += 1;
                            continue;
                        }


                        String src = li.getElementsByTag("img").attr("src");
                        String localImgPath = fileUploader.upload(new URL(src).openStream(), titleMd5, 0);


                        itemUrl = li.select(".title").attr("href");
                        HttpGet itemGet = new HttpGet(itemUrl);
                        HttpManager.config(itemGet);

                        Document atDoc = getForDocPage(itemGet, 0);

                        replaceImage(atDoc, fileUploader);
                        String content = "";
                        String source = "";
                        String author = "";
                        if (itemUrl.matches("http://newseed\\.pedaily\\.cn/.*")) {
                            String sourceStr = "";
                            Elements allPage = atDoc.select("div.page.page-list a.all");
                            if (allPage != null && allPage.size() > 0) {
                                String href = allPage.attr("href");
                                Document allDoc = getForDocPage(href);
                                replaceImage(allDoc, fileUploader);
                                content = allDoc.select("div.news-content").html();
                                sourceStr = allDoc.select("div.info").first().child(1).ownText().replaceAll("　", "").trim();
                            } else {
                                content = atDoc.select("div.news-content").html();
                                sourceStr = atDoc.select("div.info").first().child(1).ownText().replaceAll("　", "").trim();
                            }
                            //　 投资界　 Echo　
                            String[] split = sourceStr.split(" ");
                            int length = split.length;
                            if (length > 1) {
                                source = split[0];
                                author = split[1];
                            } else if (length > 2) {
                                source = split[0];
                                //多个作者
                                author = source.replace(source, "").trim();
                            } else {
                                source = split[0];
                            }
                        } else if (itemUrl.matches("http://news\\.pedaily\\.cn/.*")
                                || itemUrl.matches("http://people\\.pedaily\\.cn/.*")
                                || itemUrl.matches("http://pe\\.pedaily\\.cn/.*")
                                || itemUrl.matches("http://if\\.pedaily\\.cn/.*")) {
                            String sourceStr = "";
                            Elements allPage = atDoc.select("div.page.page-list a.all");
                            if (allPage != null && allPage.size() > 0) {
                                String href = allPage.attr("href");
                                Document allDoc = getForDocPage(href);
                                replaceImage(allDoc, fileUploader);
                                content = allDoc.select("div.news-content").html();
                                sourceStr = allDoc.select("div.news-show div.info div.box-l").first().ownText().replaceAll("　", "").trim();
                            } else {
                                content = atDoc.select("div.news-content").html();
                                sourceStr = atDoc.select("div.news-show div.info div.box-l").first().ownText().replaceAll("　", "").trim();
                            }
                            //　 投资界　 Echo　
                            String[] split = sourceStr.split(" ");
                            int length = split.length;
                            if (length > 1) {
                                source = split[0];
                                author = split[1];
                            } else if (length > 2) {
                                source = split[0];
                                //多个作者
                                author = source.replace(source, "").trim();
                            } else {
                                source = split[0];
                            }

                        } else if (itemUrl.matches("http://dc\\.pedaily\\.cn/.*")) {
                            String sourceStr = "";
                            content = atDoc.select("div.body").html();
                            String ownText = atDoc.select("div.source").first().ownText();
                            sourceStr = ownText.replaceAll("来源：", "").replaceAll("作者：", "").trim();
                            //来源：每经网 作者：庞静涛
                            String[] split = sourceStr.split(" ");
                            int length = split.length;
                            if (length > 1) {
                                source = split[0];
                                author = split[1];
                            } else {
                                source = split[0];
                            }
                        } else {
                            SendBy163Mail.getInstance().sendMail(taskId() + "" + itemUrl);
                            System.out.println();
                        }

                        JSONObject itemResult = new JSONObject();
                        itemResult.put("from", taskId());
                        itemResult.put("source", source);
                        itemResult.put("author", author);
                        itemResult.put("title", title);
                        String cts = li.getElementsByClass("date").text();
                        itemResult.put("cts", cts);
                        itemResult.put("content", content);
                        String about = li.select("div.txt").text();
                        itemResult.put("about", about);
                        itemResult.put("localImgPath", localImgPath);
                        Elements tags = li.select(".tag a");
                        if (tags != null && !tags.isEmpty()) {
                            JSONArray tagArr = tags.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                            itemResult.put("keywords", tagArr);
                        }

                        pushQueue.sendToQueue(Constant.Topic.NEWS_TOPIC, itemResult.toJSONString().getBytes());
                        redis.insertToSet(Constant.Redis.NEWS_CHECK_REPEAT_QUEUE, titleMd5);

                        if (log.isDebugEnabled()) {
                            log.debug("saved news:{}", title);
                        }
                    } catch (Exception e) {
                        log.error("{} error in {}", taskId(), itemUrl, e);
                    }

                }
            }

        } catch (Exception e) {
            log.error("抓取投资界新闻列表失败,page={}", page, e);
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


    private void toStop() {
        willStop = true;
        page = 1;
        fullCrawed = true;
        repeatCount = 0;
    }


    @Override
    public String taskId() {
        return "投资界新闻列表";
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

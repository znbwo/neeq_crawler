package com.neeq.crawler.service.deprecated;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.service.deprecated.ItOrangeInvestEventsDataCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/6/7.
 */
public abstract class BasicItOrangeInventsCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ItOrangeInvestEventsDataCrawlerTask.class);


    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    protected PushQueue pushQueue;
    protected CoopRedis redis;
    private FileUploader fileUploader;


    public BasicItOrangeInventsCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }


    @Override
    public void next() {
        try {

            String url = getBaseUrl() + page;
            Document doc = getForDocPage(url);

            boolean hasNext = false;
            Elements as = doc.select(".ui-pagechange.for-sec-bottom a");
            for (Element a : as) {
                if (a.text().startsWith("下一页")) {
                    hasNext = true;
                }
            }

            if (!hasNext || repeatCount >= 10) {
                willStop = true;
                page = 1;
                fullCrawed = true;
                repeatCount = 0;
                return;
            }


            Element ul = doc.select(".list-main-eventset").last();

            Elements lis = ul.children();
            for (Element li : lis) {
                String date = li.child(0).text();       //时间
//                String cpName = li.select(".maincell .title a span").text();    //公司名称
                String cpName = fetchFullCpName(li);
                String md5 = Md5Helper.getMd5(cpName + date);   //去重

                if (isExist(md5)) {
                    if (log.isDebugEnabled()) {
                        log.debug("{} is exists", cpName);
                    }
                    repeatCount += 1;
                    continue;
                }

                String cpUrl = li.select(".maincell .title a").attr("href");

                String step = li.child(3).text();       //轮次
                String total = li.child(4).text();      //融资额
//                String investCp = li.child(5).text();   //投资方
                JSONArray inventsOrg = getInventJSONArray(li.child(5).child(0).getElementsByTag("a"), li.child(5).child(0).select("span:not(.investorset)")); //投资人数组

//                String industry = li.child(2).child(1).child(0).text(); //行业
//                String address = li.child(2).child(1).child(1).text();  //地区


                Document infoDoc = getForDocPage(cpUrl);
                Elements tds = infoDoc.select(".main .sec .block-inc-fina td");

                String logoUrl = tds.first().getElementsByTag("img").attr("src");
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    InputStream is = getFileStream(logoUrl, 0);
                    logoUrl = fileUploader.upload(is, md5 + ".png", 0);  //公司logo
                }

                String industry = tds.get(1).select("a").get(1).text() + "," + tds.get(1).select("span").get(0).text(); //大行业,小行业
                String address = tds.get(1).select("span").last().text().replace(" · ", ",");  //地区


                Elements divs = infoDoc.select(".main .sec .block div");
                Element div = divs.get(1);
                String desc = div.text();   //描述


                JSONObject result = new JSONObject();
                result.fluentPut("date", date)
                        .fluentPut("cpName", cpName)
                        .fluentPut("step", step)
                        .fluentPut("inventsOrg", inventsOrg)
                        .fluentPut("total", total)
                        .fluentPut("industry", industry)
                        .fluentPut("address", address)
                        .fluentPut("desc", desc)
                        .fluentPut("logoUrl", logoUrl);

                redis.insertToSet(Constant.Redis.INVEST_REPEAT_QUEUE, md5);
                pushQueue.sendToQueue(Constant.Topic.INVEST_DATA_TOPIC, result.toJSONString().getBytes());

                if (log.isDebugEnabled()) {
                    log.debug("抓取到投资数据:{}", result.toJSONString());
                }
            }


            page += 1;
        } catch (Exception e) {
            log.error("抓取IT桔子投资数据失败,page={}", page, e);
        }
    }


    private boolean isExist(String key) {
        Jedis jedis = null;
        try {

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.INVEST_REPEAT_QUEUE+"_"+taskId(), key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    private String fetchFullCpName(Element element) {
        String invUrl = element.select(".maincell .title a").attr("abs:href");//投资详情页绝对url
        Document doc = getForDocPage(invUrl);
        String cpUrl = doc.select(".block-inc-fina .incicon").attr("abs:href");//公司详情url
        doc = getForDocPage(cpUrl);
        String fullName = doc.select(".main .des-more div span").first().text();
        return fullName.replace("公司全称：", "").trim();
    }


    private JSONArray getInventJSONArray(Elements inventUrls, Elements inventSpans) {
        JSONArray jsonArray = new JSONArray();
        for (Element url : inventUrls) {
            jsonArray.add(url.text());
        }
        for (Element span : inventSpans) {
            jsonArray.add(span.text());
        }
        return jsonArray;
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


    abstract String getBaseUrl();
}

package com.neeq.crawler.service.deprecated;

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

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IT桔子投资机构数据
 * Created by kidbei on 16/6/7.
 */
public class ItOrangeInvestOrgCrawlerTask  extends BasicClientCrawlerTask {


    private final Logger log = LoggerFactory.getLogger(ItOrangeInvestOrgCrawlerTask.class);

    private final String base_url = "https://www.itjuzi.com/investfirm?page=";

    private int page = 1;
    private int repeatCount = 0;
    private boolean fullCrawed = false;



    private PushQueue pushQueue;
    private CoopRedis redis;
    private FileUploader fileUploader;


    public ItOrangeInvestOrgCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        this.pushQueue = pushQueue;
        this.redis = redis;
        this.fileUploader = fileUploader;
    }



    @Override
    public void next() {
        try{

            String url = base_url + page;
            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0);


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

            Elements lis = doc.select(".list-main-investset").last().children();
            for (Element li : lis) {
                String orgName = li.child(1).child(0).child(0).text();
                String orgUrl = li.child(1).child(0).child(0).attr("href");
                String orgMd5 = Md5Helper.getMd5(orgName);

                if (isExist(orgMd5)) {
                    if (log.isDebugEnabled()) {
                        log.debug("投资机构以及存在,{}",orgName);
                    }
                    repeatCount += 1;
                    continue;
                }

                HttpGet infoGet = new HttpGet(orgUrl);
                HttpManager.config(infoGet);
                Document infoDoc = getForDocPage(infoGet,0);

                String logoUrl = infoDoc.select(".infohead-group .pic img").attr("src");
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    InputStream is = getFileStream(logoUrl, 0);
                    logoUrl = fileUploader.upload(is, orgMd5 + ".png", 0);  //公司logo
                }
                String desc = infoDoc.select(".sec .block-inc-info .des").text();

                //投资领域
                Elements investRanges = infoDoc.select(".sec-notitleborder div").get(1).select(".list-tags a");
                //投资轮次
                Elements investSteps = infoDoc.select(".sec-notitleborder div").get(3).select(".list-tags a");


                JSONObject result = new JSONObject();
                result.fluentPut("orgName",orgName)
                        .fluentPut("orgUrl",orgUrl)
                        .fluentPut("desc",desc)
                        .fluentPut("logoUrl",logoUrl);

                if (investRanges != null && !investRanges.isEmpty()) {
                    JSONArray arr = investRanges.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                    result.put("investRanges",arr);
                }

                if (investSteps != null && !investSteps.isEmpty()) {
                    JSONArray arr = investSteps.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                    result.put("investSteps",arr);
                }

                redis.insertToSet(Constant.Redis.INVEST_ORG_REPEAT_QUEUE,orgMd5);
                pushQueue.sendToQueue(Constant.Topic.INVEST_ORG_TOPIC,result.toJSONString().getBytes());
                if (log.isDebugEnabled()) {
                    log.debug("抓取到投资机构数据:{}",result.toJSONString());
                }

            }

        } catch(Exception e) {
            log.error("抓取IT桔子投资机构数据失败,page={}",page,e);
        }

        page += 1;
    }


    private boolean isExist(String key) {
        Jedis jedis = null;
        try{

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.INVEST_ORG_REPEAT_QUEUE,key);
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
        return "IT桔子投资机构数据";
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 24;
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }
}

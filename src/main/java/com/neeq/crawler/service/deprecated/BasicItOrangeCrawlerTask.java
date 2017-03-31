package com.neeq.crawler.service.deprecated;

import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
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

import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/6/7.
 */
public abstract class BasicItOrangeCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(BasicItOrangeCrawlerTask.class);


    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;



    protected PushQueue pushQueue;
    protected CoopRedis redis;


    public BasicItOrangeCrawlerTask(PushQueue pushQueue, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.redis = redis;
    }




    @Override
    public void next() {
        try{

            String url = getBaseUrl() + page;
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


            Element ul = doc.select(".list-main-eventset").last();

            Elements lis = ul.children();
            for (Element li : lis) {
                String date = li.child(0).text();       //时间
                String cpName = li.select(".maincell .title a span").text();    //公司名称

                String md5 = Md5Helper.getMd5(cpName + date);   //去重

                if (isExist(md5)) {
                    if (log.isDebugEnabled()) {
                        log.debug("{} is exists",cpName);
                    }
                    repeatCount += 1;
                    continue;
                }

                String cpUrl = li.select(".maincell .title a").attr("href");

                String step = li.child(3).text();       //轮次
                String total = li.child(4).text();      //融资额
                String investCp = li.child(5).text();   //投资方

                String industry = li.child(2).child(1).child(0).text(); //行业
                String address = li.child(2).child(1).child(1).text();  //地区


                HttpGet infoGet = new HttpGet(cpUrl);
                HttpManager.config(infoGet);

                Document infoDoc = getForDocPage(infoGet,0);

                Elements divs = infoDoc.select(".main .sec .block div");
                Element div = divs.get(1);

                String desc = div.text();   //描述


                JSONObject result = new JSONObject();
                result.fluentPut("date",date)
                        .fluentPut("cpName",cpName)
                        .fluentPut("step",step)
                        .fluentPut("investCp",investCp)
                        .fluentPut("total",total)
                        .fluentPut("industry",industry)
                        .fluentPut("address",address)
                        .fluentPut("desc",desc);

                redis.insertToSet(Constant.Redis.INVEST_REPEAT_QUEUE,md5);
                pushQueue.sendToQueue(Constant.Topic.INVEST_DATA_TOPIC,result.toJSONString().getBytes());

                if (log.isDebugEnabled()) {
                    log.debug("抓取到投资数据:{}",result.toJSONString());
                }
            }


            page += 1;
        } catch(Exception e) {
            log.error("抓取IT桔子投资数据失败,page={}",page,e);
        }
    }




    private boolean isExist(String key) {
        Jedis jedis = null;
        try{

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.INVEST_REPEAT_QUEUE,key);
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
        return "IT桔子投资数据";
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

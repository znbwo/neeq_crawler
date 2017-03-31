package com.neeq.crawler.service.deprecated;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
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
import java.util.stream.Collectors;

/**
 * it桔子投资人数据
 * Created by kidbei on 16/6/8.
 */
public class ItOrangeInvestPersonCrawlerTask extends BasicClientCrawlerTask {


    private  final Logger log = LoggerFactory.getLogger(ItOrangeInvestPersonCrawlerTask.class);


    private final String base_url = "https://www.itjuzi.com/investor?page=";


    private PushQueue pushQueue;
    private CoopRedis redis;


    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;



    public ItOrangeInvestPersonCrawlerTask(PushQueue pushQueue, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.redis = redis;
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


            Elements lis = doc.select(".list-main-personset li");
            for (Element li : lis) {
                String name = li.child(1).select(".title .name").text();
                String userUrl = li.child(1).select(".title .name").attr("href");
                if (isExist(name)) {
                    if (log.isDebugEnabled()) {
                        log.debug("投资人数据已经存在:{}",name);
                    }
                    repeatCount += 1;
                    continue;
                }


                HttpGet userGet = new HttpGet(userUrl);
                HttpManager.config(userGet);

                Document userDoc = getForDocPage(userGet,0);

//                String orgName = userDoc.select(".titleset span a").text(); //所属机构
//                String role = userDoc.select(".titleset span").text();  //角色

                //所有机构 + 角色
                Elements belongsTos = userDoc.select(".titleset span");

                Elements divs = userDoc.select(".main .sec");


                //投资轮次
                Elements investSteps = divs.get(0).child(1).select(".list-tags-box").first().select(".list-tags .tag");

                //投资领域
                Elements investRanges = divs.get(0).child(1).select(".list-tags-box").last().select(".list-tags .tag");

                String desc = divs.get(0).child(1).child(2).text();

                //投资案例 //// TODO: 16/6/8 需要登录
//                String al = divs.get(1).child(1).child(0).child(0).text();


                JSONObject result = new JSONObject();
                if (belongsTos != null && !belongsTos.isEmpty()) {
                    JSONArray arr = belongsTos.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                    result.put("belongsTos",arr);
                }
                if (investSteps != null && !investSteps.isEmpty()) {
                    JSONArray arr = investSteps.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                    result.put("investSteps",arr);
                }
                if (investRanges != null && !investRanges.isEmpty()) {
                    JSONArray arr = investRanges.stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new));
                    result.put("investRanges",arr);
                }
                result.put("desc",desc);
                result.put("name",name);


                redis.insertToSet(Constant.Redis.INVEST_PERSON_REPEAT_QUEUE,name);
                pushQueue.sendToQueue(Constant.Topic.INVEST_PERSON_TOPIC,result.toJSONString().getBytes());

                if (log.isDebugEnabled()) {
                    log.debug("抓取到投资人数据:{}",result.toJSONString());
                }
            }



        } catch(Exception e) {
            log.error("抓取IT桔子投资人数据失败,page={}",page,e);
        }


        page += 1;
    }




    private boolean isExist(String key) {
        Jedis jedis = null;
        try{

            jedis = redis.getJedis();

            return jedis.sismember(Constant.Redis.INVEST_PERSON_REPEAT_QUEUE,key);
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
        return "it桔子投资人数据";
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

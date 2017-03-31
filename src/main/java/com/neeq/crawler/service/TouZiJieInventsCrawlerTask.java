package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.RedisHelper;
import com.neeq.crawler.tool.StringHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 投资界投资事件
 * znb 16/6/22
 */
public class TouZiJieInventsCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(TouZiJieInventsCrawlerTask.class);


    private final String BASE_URL = "http://zdb.pedaily.cn";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;



    private final String topic = Constant.Topic.INVEST_DATA_TOUZIJIE_TOPIC;
    private final String redisKey = Constant.Redis.INVEST_TOUZIJIE_REPEAT_QUEUE;

    public TouZiJieInventsCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(redis, pushQueue, fileUploader);
    }


    @Override
    public void next() {
        try {
            String url = BASE_URL + "/inv/" + page + "/";
            Document doc = getForDocPage(url);

            boolean hasNext = false;
            Elements as = doc.select(".content .main .page-list .next");
            for (Element a : as) {
                if (a.text().contains("下一页")) {
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


            Elements trs = doc.select(".main .box-content  .zdb-inv-table tr");
            if (trs != null && !trs.isEmpty()) {
                trs.remove(0);//去掉非数据tr行
                for (Element tr : trs) {
                    String infoUrl = BASE_URL + tr.select(".td6 a").attr("href");//拼接绝对路径
                    try {
                        Document infoDoc = getForDocPage(infoUrl);
                        Element div = infoDoc.select(".main .zdb-content").first();
                        String date = div.select("p").first().text().replace(div.select("p").first().child(0).text(), "");  //事件日期
                        String cpName = div.select("p").get(2).select("a").first().text(); //受资公司全名
                        String titleMd5 = Md5Helper.getMd5(date + cpName);

                        if (RedisHelper.existInSet(redis, redisKey, titleMd5)) {
                            if (log.isDebugEnabled()) {
                                log.debug("{} is exist", cpName);
                            }
                            repeatCount += 1;
                            continue;
                        }

                        String step = div.select("p").get(3).text().replace(div.select("p").get(3).child(0).text(), "");
                        String industry = StringHelper.formatElements(div.select("p").get(4).select("a"));
                        String total = div.select("p").get(5).text().replace(div.select("p").get(5).child(0).text(), "");  //投资额
                        String desc = div.select("p").get(7).text();   //事件描述


                        JSONObject result = new JSONObject();
                        result.fluentPut("date", date)
                                .fluentPut("cpName", cpName)//受资方
                                .fluentPut("step", step)//投资轮次
                                .fluentPut("total", total)//金额
                                .fluentPut("industry", industry)//行业
                                .fluentPut("desc", desc);
                        Elements inventNames = div.select("p").get(1).select("a");
                        if (inventNames != null && !inventNames.isEmpty()) {
                            result.put("inventNamesArr", StringHelper.formatElements(inventNames));  //投资方
                        }

                        sendMessage(topic, result, redisKey, titleMd5);

                        if (log.isDebugEnabled()) {
                            log.debug("抓取到投资数据:{}", result.toJSONString());
                        }
                    } catch (Exception e) {
                        log.error("抓取{}失败,url={}", taskId(), infoUrl, e);
                    }

                }
            }

        } catch (Exception e) {
            log.error("抓取投资界投资事件失败,page={}", page, e);
        }
        page += 1;
    }


    @Override
    public String taskId() {
        return "投资界投资事件";
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

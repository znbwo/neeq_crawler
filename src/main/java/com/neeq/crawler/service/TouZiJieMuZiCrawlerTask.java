package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.RedisHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 投资界募资事件
 * znb 16/7/6
 */
public class TouZiJieMuZiCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(TouZiJieMuZiCrawlerTask.class);


    private final String BASE_URL = "http://zdb.pedaily.cn";

    private int page = 1;
    private boolean fullCrawed = false;
    private int repeatCount = 0;


    private final String topic = Constant.Topic.FUNDRAISING_DATA_TOUZIJIE_TOPIC;
    private final String redisKey = Constant.Redis.INVEST_TOUZIJIE_REPEAT_QUEUE;


    public TouZiJieMuZiCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(redis, pushQueue, fileUploader);
    }


    @Override
    public void next() {
        try {
            String url = BASE_URL + "/pe/" + page + "/";
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


            Elements trs = doc.select(".main .box-content .zdb-pe-table tr");
            if (trs != null && !trs.isEmpty()) {
                trs.remove(0);//去掉非数据tr行
                for (Element tr : trs) {
                    String infoUrl = BASE_URL + tr.select(".td6 a").attr("href");//拼接绝对路径

                    try {
                        Document infoDoc = getForDocPage(infoUrl);
                        Element div = infoDoc.select(".main .zdb-content").first();
                        Elements ps = div.select("p");
                        Element p0 = ps.get(0);
                        String fundName = p0.text().replace(p0.select("b").text(), "");//基金名称
                        Element p1 = ps.get(1);
                        String startTime = p1.text().replace(p1.select("b").text(), "");//成立时间
                        Element p2 = ps.get(2);
                        String administration = p2.text().replace(p2.select("b").text(), "");//管理机构名称
                        Element p3 = ps.get(3);
                        String fundType = p3.text().replace(p3.select("b").text(), "");//资本类型
                        Element p4 = ps.get(4);
                        String moneyType = p4.text().replace(p4.select("b").text(), "");//币种
                        Element p5 = ps.get(5);
                        String state = p5.text().replace(p5.select("b").text(), "");//募集状态
                        Element p6 = ps.get(6);
                        String targetMoney = p6.text().replace(p6.select("b").text(), "");//目标规模
                        Element p7 = ps.get(7);
                        String money = p7.text().replace(p7.select("b").text(), "");//募集金额

                        String desc = ps.get(9).text();
                        String md5 = Md5Helper.getMd5(fundName + state + money);

                        if (RedisHelper.existInSet(redis, redisKey, md5)) {
                            if (log.isDebugEnabled()) {
                                log.debug("{} is exist", fundName);
                            }
                            repeatCount += 1;
                            continue;
                        }


                        JSONObject result = new JSONObject();
                        result.fluentPut("基金名称", fundName)
                                .fluentPut("成立时间", startTime)
                                .fluentPut("管理机构名称", administration)
                                .fluentPut("资本类型", fundType)
                                .fluentPut("币种", moneyType)
                                .fluentPut("募集状态", state)
                                .fluentPut("目标规模", targetMoney)
                                .fluentPut("募集金额", money)
                                .fluentPut("描述", desc);

                        sendMessage(topic, result, redisKey, md5);

                        if (log.isDebugEnabled()) {
                            log.debug("抓取到并购数据:{}", result.toJSONString());
                        }
                    } catch (Exception e) {
                        log.error("抓取{}失败,url={}", taskId(), infoUrl, e);
                    }


                }
            }

        } catch (Exception e) {
            log.error("抓取投资界募资事件失败,page={}", page, e);
        }
        page += 1;
    }


    @Override
    public String taskId() {
        return "投资界募资事件";
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

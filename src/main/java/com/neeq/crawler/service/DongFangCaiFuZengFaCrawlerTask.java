package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 *
 * 抓取东方财富三板增发列表
 * Created by kidbei on 16/5/25.
 */
public class DongFangCaiFuZengFaCrawlerTask  extends BasicClientCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(DongFangCaiFuZengFaCrawlerTask.class);

    private final String url_pre = "http://datainterface.eastmoney.com//EM_DataCenter/JS.aspx?type=SB&sty=SBZF&st=2&sr=-1&p=";
    private final String url_after  = "&ps=100&js=%7Bpages:(pc),data:[(x)]%7D&stat=0&rt=48805218";


    private PushQueue pushQueue;
    private CoopRedis redis;

    private int page = 0;
    private int totalPage = -1;

    public DongFangCaiFuZengFaCrawlerTask(PushQueue pushQueue,CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.redis = redis;
    }


    @Override
    public void next() {

        page ++ ;

        if (page == totalPage) {
            willStop = true;
            totalPage = -1;
            page = 0;
        }


        try{
            String url = url_pre + page + url_after;

            HttpGet get = new HttpGet(new URI(url));
            HttpManager.config(get);

            String jsonStr = getForStringPage(get,0);

            JSONObject result = JSON.parseObject(jsonStr);

            if (totalPage == -1) {
                totalPage = result.getInteger("pages");
            }

            JSONArray list = result.getJSONArray("data");
            JSONArray newList = new JSONArray();
            for (int i = 0 ; i <  list.size(); i ++) {
                String line = list.getString(i);
                String[] lines = line.split(",");

                //代码
                String code = lines[0];
                code = code.substring(0,code.lastIndexOf("."));

                String name = lines[1]; //公司名称
                String jd = lines[4];   //方案进度
                String zfsl = lines[5];//增发数量
                String zfjg = lines[6];//增发价格
                String syl = lines[7]; //市盈率(LYR)
                String zyjl = lines[8]; //折溢价率
                String fxfs = lines[9]; //发行方式
                String zxggr = lines[2]; //最新公告日
                String scggr = lines[3]; //首次公告日

                JSONObject obj = new JSONObject()
                        .fluentPut("code",code)
                        .fluentPut("name",name)
                        .fluentPut("jd",jd)
                        .fluentPut("zfsl",zfsl)
                        .fluentPut("zfjg",zfjg)
                        .fluentPut("syl",syl)
                        .fluentPut("zyjl",zyjl)
                        .fluentPut("fxfs",fxfs)
                        .fluentPut("zxggr",zxggr)
                        .fluentPut("scggr",scggr);
                newList.add(obj);
            }


            if (log.isDebugEnabled()) {
                log.debug("get data : {}",newList.toJSONString());
            }

            pushQueue.sendToQueue(Constant.Topic.ZENGFA_TOPIC,newList.toJSONString().getBytes());


        } catch(Exception e) {
            log.error("抓取东方财富三板增发失败",e);
        }

    }

    @Override
    public String taskId() {
        return "东方财富三板增发";
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
        return new TaskOptions().setPeriod(1000 * 3).setTimeUnit(TimeUnit.MILLISECONDS);
    }


}

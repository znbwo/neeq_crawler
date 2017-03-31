package com.neeq.crawler.service.proxy;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/24.
 */
public class ProxyIpTask01 extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ProxyIpTask01.class);

    private final String URL = "http://www.xicidaili.com/nt/";


    private CoopRedis redis;


    public ProxyIpTask01(CoopRedis redis) {
        this.redis = redis;
    }


    private int page = 1;
    private int totalPage = 10;


    @Override
    public void next() {
        try{

            if (page == totalPage) {
                willStop = true;
                page = 1;
            }

            String url = URL + page;

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0);
            Elements trs = doc.select("#ip_list tbody tr");
            trs.remove(0);

            for (Element tr : trs) {
                String host = tr.child(1).text();
                String portStr = tr.child(2).text();
                String schema = tr.child(5).text().toLowerCase();

                String hostName = host + ":" + portStr;
                if (log.isDebugEnabled()) {
                    log.debug("push {} to check queue",hostName);
                }

                if (schema.equals("http")) {
                    redis.insertToSet(Constant.Redis.PROXY_HTTP_CHECK_QUEUE,hostName);
                } else {
                    redis.insertToSet(Constant.Redis.PROXY_HTTPS_CHECK_QUEUE,hostName);
                }
            }

            page += 1;

        } catch(Exception e) {
            log.error("抓取IP失败",e);
        }
    }





    @Override
    public String taskId() {
        return "http://www.xicidaili.com/nt/";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 5).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 10;
    }
}

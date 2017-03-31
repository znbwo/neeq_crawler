package com.neeq.crawler.service.proxy;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kidbei on 16/5/24.
 */
public class ProxyIpTask02 extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ProxyIpTask02.class);

    private final String httpURL = "http://api.zdaye.com/?api=201607021010446529&ct=2000";
    private final String httpsURL = "http://api.zdaye.com/?api=201607021010446529&https=%D6%A7%B3%D6&ct=2000";


    private CoopRedis redis;

    public ProxyIpTask02(CoopRedis redis) {
        this.redis = redis;
    }


    @Override
    public void next() {
        try {

            fetchIps(httpURL, "http");
            fetchIps(httpsURL, "https");

        } catch (Exception e) {
            log.error("抓取IP失败", e);
        }
    }

    private void fetchIps(String url, String schema) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);

        String ipstr = getForStringPage(get, 0,"gbk");
        //来源网站认为抓取过于频繁
        String regEx = "[\\u4e00-\\u9fa5]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(ipstr);
        boolean b = m.find();
        if (ipstr.contains("<bad>") || b) {
            System.out.println("来源网站认为抓取过于频繁");
            return;
        }
        String[] ips = ipstr.split("\r\n");
        for (String ip : ips) {
            if (log.isDebugEnabled()) {
                log.debug("push {} to check " + schema + " queue", ip);
            }

            if (schema.equals("http")) {
                redis.insertToSet(Constant.Redis.PROXY_HTTP_CHECK_QUEUE, ip);
            } else {
                redis.insertToSet(Constant.Redis.PROXY_HTTPS_CHECK_QUEUE, ip);
            }
        }
    }


    @Override
    public String taskId() {
        return "http://api.zdaye.com/";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 60).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 5;
    }
}

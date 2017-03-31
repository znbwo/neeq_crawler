package com.neeq.crawler.service.proxy;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.pool.ThreadPool;
import com.neeq.crawler.task.CrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/24.
 * 初始检查
 */
public class ProxyCheckTask implements CrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ProxyCheckTask.class);

    private final String CHECK_HTTP_URL = "http://data.eastmoney.com/other/sb/zfts.html";
    private final String CHECK_HTTPS_URL = "https://www.itjuzi.com/";

    public CoopRedis redis;


    public ProxyCheckTask(CoopRedis redis) {
        this.redis = redis;
    }


    @Override
    public void preStart() {
        log.info("start proxy ip check task");
    }

    @Override
    public void preStop() {
        log.info("proxy ip check task is stopped");
    }

    @Override
    public void next() {
        Set<String> httpHostNames = redis.getSetMembers(Constant.Redis.PROXY_HTTP_CHECK_QUEUE);
        ThreadPool.getPool().execute(() -> {
            for (String hostName : httpHostNames) {
                if (hostName != null && !hostName.isEmpty() && !hostName.equals("nil")) {
                    checkHostName(hostName, "http");
                }
            }
        });
        Set<String> httpsHostNames = redis.getSetMembers(Constant.Redis.PROXY_HTTPS_CHECK_QUEUE);
        for (String hostName : httpsHostNames) {
            if (hostName != null && !hostName.isEmpty() && !hostName.equals("nil")) {
                checkHostName(hostName, "https");
            }
        }
    }


    private void checkHostName(String hostName, String schema) {
        String host = hostName.split(":")[0];
        int port = Integer.valueOf(hostName.split(":")[1]);

        ThreadPool.getPool().execute(() -> {
            CloseableHttpClient client = null;
            try {
                client = HttpManager.getProxyClient(host, port);

                HttpGet get = new HttpGet(schema.equals("http") ? CHECK_HTTP_URL : CHECK_HTTPS_URL);
                HttpManager.config(get, 1000 * 5);

                CloseableHttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() >= 400) {
                    throw new RuntimeException("status code:" + response.getStatusLine().getStatusCode());
                }
                redis.insertToSet(schema.equals("http") ? Constant.Redis.PROXY_HTTP_OK_QUEUE : Constant.Redis.PROXY_HTTPS_OK_QUEUE, hostName);
                if (log.isDebugEnabled()) {
                    log.debug("check {} success : {}", schema, hostName);
                }
            } catch (Exception e) {
                log.error("check proxy {}:{},schema={} failed", host, port, schema, e);
            } finally {
                HttpManager.close(client);
            }
        });
    }

    @Override
    public String taskId() {
        return "代理IP检查";
    }

    @Override
    public boolean willStop() {
        return false;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 2).setTimeUnit(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean repeat() {
        return false;
    }

    @Override
    public void taskAgain() {

    }

    @Override
    public long repeatAfterTime() {
        return 0;
    }

    @Override
    public boolean userProxy() {
        return false;
    }
}

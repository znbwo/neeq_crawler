package com.neeq.crawler.service.proxy;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.task.CrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * 可以使用的ip实时复查
 * Created by kidbei on 16/5/24.
 */
public class ProxyOkQueueCheckTask implements CrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ProxyOkQueueCheckTask.class);

    private final String CHECK_HTTP_URL = "http://data.eastmoney.com/other/sb/zfts.html";
    private final String CHECK_HTTPS_URL = "https://www.itjuzi.com/";

    public CoopRedis redis;


    public ProxyOkQueueCheckTask(CoopRedis redis) {
        this.redis = redis;
    }


    @Override
    public void preStart() {
        log.info("start proxy ip recheck task");
    }

    @Override
    public void preStop() {
        log.info("proxy ip recheck task is stopped");
    }


    @Override
    public void next() {

        Jedis jedis = redis.getJedis();
        Set<String> httpHostNames = jedis.smembers(Constant.Redis.PROXY_HTTP_OK_QUEUE);
        if (httpHostNames != null && httpHostNames.size() > 0) {
            for (String hostName : httpHostNames) {
                checkHostName(hostName,"http");
            }
        }
        Set<String> httpsHostNames = jedis.smembers(Constant.Redis.PROXY_HTTPS_OK_QUEUE);
        if (httpsHostNames != null && httpsHostNames.size() > 0) {
            for (String hostName : httpsHostNames) {
                checkHostName(hostName,"https");
            }
        }

    }




    private void checkHostName(String hostName,String schema) {
        String host = hostName.split(":")[0];
        int port = Integer.valueOf(hostName.split(":")[1]);

        CloseableHttpClient client = null;
        try{
            client = HttpManager.getProxyClient(host,port);

            HttpGet get = new HttpGet(schema.equals("http") ? CHECK_HTTP_URL : CHECK_HTTPS_URL);
            HttpManager.config(get,1000 * 5);

            CloseableHttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new RuntimeException("status code:" + response.getStatusLine().getStatusCode());
            }

            if (log.isDebugEnabled()) {
                log.debug("recheck {} success : {}",schema,hostName);
            }
        } catch(Exception e) {
            log.error("recheck {} proxy {}:{} failed",schema,host,port,e);
            Jedis jedis = null;
            try{
                jedis = redis.getJedis();
                jedis.srem(schema.equals("http")?Constant.Redis.PROXY_HTTP_OK_QUEUE:Constant.Redis.PROXY_HTTPS_OK_QUEUE,hostName);
            } catch(Exception e1) {
                e1.printStackTrace();
            } finally {
                jedis.close();
            }
        } finally {
            HttpManager.close(client);
        }
    }

    @Override
    public String taskId() {
        return "代理IP复查";
    }

    @Override
    public boolean willStop() {
        return false;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 1).setTimeUnit(TimeUnit.MILLISECONDS);
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
        return 1000 * 60 * 10;
    }

    @Override
    public boolean userProxy() {
        return false;
    }
}

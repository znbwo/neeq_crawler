package com.neeq.crawler.consumer;

import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import redis.clients.jedis.Jedis;

/**
 * Created by bj on 16/7/14.
 */
public class HttpHelper {
    private static CoopRedis redis = RedisHelper.redis;

    public static String getForStringPage(CloseableHttpClient client, HttpRequestBase request, int sleepTime, int errorIndex, String encoding) {
        CloseableHttpResponse response = null;
        try {
            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() >= 400) {
                return null;
            }
            Thread.sleep(1000 * sleepTime);
            return IOUtils.toString(response.getEntity().getContent(), encoding);
        } catch (Exception e) {
            if (errorIndex < 3) {
                int count = errorIndex + 1;
                return getForStringPage(client, request, sleepTime, count, encoding);
            }
        } finally {
            HttpManager.close(response);
        }
        return null;
    }

    public static HttpHost getProxy(String schema) {
        Jedis jedis = null;
        try {
            jedis = redis.getJedis();
            String hostName = jedis.srandmember(schema.equals("http") ? Constant.Redis.PROXY_HTTP_OK_QUEUE : Constant.Redis.PROXY_HTTPS_OK_QUEUE);
            String[] ss = hostName.split(":");
            if (ss.length > 1) {
                String ip = ss[0];
                int port = Integer.valueOf(ss[1]);
                return new HttpHost(ip, port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }
}

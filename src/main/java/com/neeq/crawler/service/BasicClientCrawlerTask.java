package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.consumer.HttpHelper;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.CrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/23.
 */
public abstract class BasicClientCrawlerTask implements CrawlerTask {
    private static final Logger log = LoggerFactory.getLogger(BasicClientCrawlerTask.class);

    private CloseableHttpClient client;
    protected boolean willStop = false;

    protected CoopRedis redis;
    protected PushQueue pushQueue;
    protected FileUploader fileUploader;

    public BasicClientCrawlerTask() {
    }

    public BasicClientCrawlerTask(CoopRedis redis, PushQueue pushQueue, FileUploader fileUploader) {
        this.redis = redis;
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
    }

    @Override
    public void preStart() {
        if (!userProxy()) {
            client = HttpManager.getClient();
        }
    }

    @Override
    public void preStop() {
        HttpManager.close(client);
    }

    @Override
    public boolean willStop() {
        return willStop;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions()
                .setPeriod(1000 * 60 * 60 * 24)
                .setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return false;
    }


    @Override
    public void taskAgain() {
        willStop = false;
    }

    @Override
    public long repeatAfterTime() {
        return 0;
    }


    @Override
    public boolean userProxy() {
        return false;
    }


    public CoopRedis getRedis() {
        return null;
    }


    public CloseableHttpClient getProxyClient(String schema) {
        HttpHost proxy = HttpHelper.getProxy(schema);
        if (proxy != null) {
            return HttpManager.getProxyClient(proxy.toHostString(), proxy.getPort());
        }
        throw new NullPointerException("no proxy ip found");
//        Jedis jedis = null;
//        try {
//            jedis = getRedis().getJedis();
//            if (jedis == null) {
//                throw new NullPointerException("please override getRedis method");
//            }
//
//            String hostName = jedis.srandmember(schema.equals("http") ? Constant.Redis.PROXY_HTTP_OK_QUEUE : Constant.Redis.PROXY_HTTPS_OK_QUEUE);
//            if (hostName == null || hostName.equals("nil")) {
//                throw new NullPointerException("no proxy ip found");
//            }
//
//            String[] ss = hostName.split(":");
//            String ip = ss[0];
//            int port = Integer.valueOf(ss[1]);
//
//            return HttpManager.getProxyClient(ip, port);
//
//        } catch (Exception e) {
//            throw e;
//        } finally {
//            if (jedis != null) {
//                jedis.close();
//            }
//        }
    }

    public CloseableHttpClient getClient(String schema) {
        if (userProxy()) {
            return getProxyClient(schema);
        }
        return client;
    }

    public String getForStringPage(HttpRequestBase request, int errorIndex) {
        return getForStringPage(request, errorIndex, "utf-8");
    }

    public String getForStringPage(HttpRequestBase request, int errorIndex, String encoding) {
        String schema = null;
        try {
            schema = request.getURI().toURL().getProtocol();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return HttpHelper.getForStringPage(getClient(schema), request, 0, errorIndex, encoding);
    }


    public Document getForDocPage(String url) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);
        return getForDocPage(get, 0);
    }


    public Document getForDocPage(HttpRequestBase request, int errorIndex) {
        return getForDocPage(request, errorIndex, "utf-8");
    }

    public Document getForDocPage(HttpRequestBase request, int errorIndex, String encoding) {
        String html = getForStringPage(request, errorIndex, encoding);
        return Jsoup.parse(html);
    }


    public InputStream getFileStream(String url, int errorIndex) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get, 1000 * 60);
        return getFileStream(get, errorIndex);

    }

    public InputStream getFileStream(HttpRequestBase requestBase, int errorIndex) {
        CloseableHttpResponse response = null;
        try {
            response = getClient(requestBase.getProtocolVersion().getProtocol()).execute(requestBase);
            return response.getEntity().getContent();

        } catch (Exception e) {
            int count = errorIndex + 1;
            if (count < 3) {
                return getFileStream(requestBase, count);
            } else {
                return null;
            }
        } finally {
//            HttpManager.close(response); //下载文件是长连接
        }
    }


    protected void replaceImage(Document doc, FileUploader fileUploader) {
        Elements imgs = doc.getElementsByAttribute("img");
        if (imgs != null && imgs.size() > 0) {
            for (Element img : imgs) {
                try {
                    String oldUrl = img.attr("src");
                    if (oldUrl != null) {
                        InputStream is = new URL(oldUrl).openStream();
                        String url = fileUploader.upload(is, UUID.randomUUID().toString(), 0);
                        img.attr("src", url);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Header[] getCookie(HttpRequestBase request, int errorIndex) {
        CloseableHttpResponse response = null;
        try {

            String schema = request.getURI().toURL().getProtocol();
            response = getClient(schema).execute(request);
            if (response.getStatusLine().getStatusCode() >= 400) {
                return null;
            }
            Header[] cookies = response.getHeaders("Set-Cookie");
            return cookies;

        } catch (Exception e) {
            log.warn("down error {}", errorIndex, e);

            if (errorIndex < 3) {
                int count = errorIndex + 1;
                return getCookie(request, count);
            }
        } finally {
            HttpManager.close(response);
        }
        return null;
    }

    public void sendMessage(String topic, JSONObject kafkaResult, String redisKey, String redisValue) {
        pushQueue.sendToQueue(topic, kafkaResult.toJSONString().getBytes());
        redis.insertToSet(redisKey, redisValue);
    }

}

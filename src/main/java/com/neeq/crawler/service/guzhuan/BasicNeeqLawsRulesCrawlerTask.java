package com.neeq.crawler.service.guzhuan;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Created by bj on 16/7/19.
 */
public abstract class BasicNeeqLawsRulesCrawlerTask extends BasicNeeqCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(BasicNeeqLawsRulesCrawlerTask.class);

    private static String url = "http://www.neeq.com.cn";
    static String queryurl = url + "/info/list.do?callback=mm";
    private static String topc = Constant.Topic.NEEQ_RULES;
    private static String redis_key = Constant.Redis.NEEQ_RULES;
    private int pageIndex;

    public BasicNeeqLawsRulesCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis, fileUploader);
    }

    @Override
    public void next() {
        run();

    }

    @Override
    public String taskId() {
        return null;
    }

    @Override
    public void run() {
        try {
            JSONObject data = JSONArray.parseArray(getResponseJsonStr()).getJSONObject(0).getJSONObject("data");
            Boolean lastPage = data.getBoolean("lastPage");
            if (lastPage) {
                willStop = true;
            }
            JSONArray jsonArray = data.getJSONArray("content");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String title = json.getString("title");
                String infoId = json.getString("infoId");
                String publishDate = json.getString("publishDate");
                String md5 = Md5Helper.getMd5(infoId + publishDate);
                if (RedisHelper.existInSet(redis, redis_key, md5)) {
                    log.info("crawl {} data {} is exist ", taskId(), title);
                    continue;
                }

                String content = "";
                String localPath = "";
                String linkUrl = json.getString("linkUrl");
                if (!linkUrl.equals("")) {
                    localPath = fileUploader.upload(new URL(url + linkUrl).openStream(), md5 + ".pdf", 0);
                    content = getPDFContent(localPath);
                } else {
                    Document document = getForDocPage(url + json.getString("htmlUrl"));
                    String pdfhref = document.select("div.newstext div.txt a").attr("href");
                    if (!pdfhref.equals("")) {
                        //另一页面pdf文件
                        localPath = fileUploader.upload(new URL(url + linkUrl).openStream(), md5 + ".pdf", 0);
                        content = getPDFContent(localPath);
                    } else {
                        //纯文字
                        content = document.select("div.txt").first().text();

                    }

                }

                JSONObject result = new JSONObject();
                result.fluentPut("标题", title).fluentPut("发布时间", publishDate).fluentPut("文件url", localPath).fluentPut("全文", content);
                sendMessage(topc, result, redis_key, md5);
                log.info("crawl {} data is : {} ", taskId(), result.toJSONString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("crawl {} error ,the page is {} ", taskId(), pageIndex, e);
        }
        pageIndex++;
    }

    @Override
    public String getResponseJsonStr() throws IOException {
//        System.out.println(this.getClass());
        HttpPost post = getHttpPost(pageIndex);
        CloseableHttpResponse response = HttpManager.getClient().execute(post);
        String responseStr = IOUtils.toString(response.getEntity().getContent());
        String jsonStr = responseStr.substring(responseStr.indexOf("mm(") + 3, responseStr.length() - 1);
        return jsonStr;
    }


    public abstract HttpPost getHttpPost(int pageIndex) throws UnsupportedEncodingException;


    public HttpGet getRulesHttpGet(String url) throws UnsupportedEncodingException {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);
        return get;
    }

}

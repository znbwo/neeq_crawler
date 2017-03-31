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
import org.apache.http.client.methods.HttpPost;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by bj on 16/7/23.
 */
public class NeeqSbczCrawlerTask extends BasicNeeqCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(NeeqSbczCrawlerTask.class);
    String queryurl = "http://www.neeq.com.cn/neeqController/getNeeqInfoList.do?callback=mm";
    static String topic = Constant.Topic.NEEQ_ZHISHU_ZIXUN;
    static String redis_key = Constant.Topic.NEEQ_ZHISHU_ZIXUN;
    private int pageIndex;

    public NeeqSbczCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(pushQueue, redis, fileUploader);
    }

    @Override
    public void run() {
        try {
            JSONObject data = JSONArray.parseArray(getResponseJsonStr()).getJSONObject(0);
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
                Document document = getForDocPage(url + json.getString("htmlUrl"));


                JSONObject result = new JSONObject();
                result.fluentPut("标题", title).fluentPut("发布时间", publishDate).fluentPut("全文", content);
                sendMessage(topic, result, redis_key, md5);
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
        HttpPost post = new HttpPost(queryurl);
        HttpManager.config(post);
        CloseableHttpResponse response = HttpManager.getClient().execute(post);
        String responseStr = IOUtils.toString(response.getEntity().getContent());
        String jsonStr = responseStr.substring(responseStr.indexOf("mm(") + 3, responseStr.length() - 1);
        return jsonStr;
    }


}

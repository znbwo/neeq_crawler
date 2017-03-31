package com.neeq.crawler.service.company_info;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.NewTopic;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 已挂牌公司抓取
 * 股转系统
 * Created by kidbei on 16/5/19.
 */
public class ListedCompanyCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ListedCompanyCrawlerTask.class);

    private PushQueue pushQueue;

    final String url_pre = "http://www.neeq.com.cn/nqxxController/nqxx.do?callback=jQuery18306412934425537645_1462853279314&page=";
    final String url_after = "&typejb=T&xxzqdm=&xxzrlx=&xxhyzl=&xxssdq=&sortfield=xxzqdm&sorttype=asc&dicXxzbqs=&_=1462853520661";

    private int pageIndex = 0;
    private int totalPage = -1;


    public ListedCompanyCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }


    @Override
    public void next() {
        String url = url_pre + pageIndex + url_after;
        pageIndex++;
        if (pageIndex == totalPage) {
            pageIndex = 0;
            totalPage = -1;
            willStop = true;
        }

        HttpGet get = new HttpGet(url);
        HttpManager.config(get);

        downPage(get);
    }

    @Override
    public String toString() {
        return taskId();
    }

    @Override
    public String taskId() {
        return "挂牌公司";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 3).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 12;
    }


    private void downPage(HttpGet get) {
        try {
            String str = getForStringPage(get, 0);

            str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));
            JSONArray result = JSONArray.parseArray(str);

            JSONArray beans = result.getJSONObject(0).getJSONArray("content");

            if (totalPage == -1) {
                totalPage = result.getJSONObject(0).getInteger("totalPages");
            }

            for (int j = 0; j < beans.size(); j++) {
                JSONObject bean = beans.getJSONObject(j);
                String code = bean.getString("xxzqdm");
                String industry = bean.getString("xxhyzl");
                try {
                    getInfo(code, industry);
                } catch (Exception e) {
                    log.error("抓取挂牌公司失败:code={}", code);
                }
            }

        } catch (Exception e) {
            log.error("下载网页失败", e);
        }
    }


    String info_url_pre = "http://www.neeq.com.cn/nqhqController/detailCompany.do?callback=jQuery18306568386539380399_1462859669059&zqdm=";
    String info_url_after = "&_=1462859669167";

    private void getInfo(String code, String industry) throws IOException {
        String url = info_url_pre + code + info_url_after;

        HttpGet get = new HttpGet(url);
        HttpManager.config(get);

        try {
            String jsonStr = getForStringPage(get, 0);

            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));


            JSONObject result = JSONObject.parseObject(jsonStr);

            String desc = getCompanyDescript(code);

            result.getJSONObject("baseinfo").put("industry", industry);
            result.getJSONObject("baseinfo").put("desc", desc);
            result.put("type", 1);
            String jsonString = result.toJSONString();
            pushQueue.sendToQueue(NewTopic.COMPANY_INFO, result.toJSONString().getBytes());
            RedisHelper.redis.insertToSet("neeq_company", result.getJSONObject("baseinfo").getString("name"));
            if (log.isDebugEnabled()) {
                log.debug("抓取到数据:{}", result.toJSONString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //http://x3b.memect.cn/api/companies
    /**
     * 从文因互联抓取公司简介
     *
     * @param code
     * @return
     */
    private final String DESC_URL = "http://x3b.memect.cn/api/companies";

    private String getCompanyDescript(String code) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(DESC_URL);
        HttpManager.config(post);

        List<NameValuePair> params = new ArrayList<NameValuePair>() {
            {
                this.add(new BasicNameValuePair("ids", code));
            }
        };
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

        String jsonStr = getForStringPage(post, 0);

        if (jsonStr == null) {
            return null;
        }

        JSONObject result = JSON.parseObject(jsonStr);
        JSONArray list = result.getJSONArray("success");
        JSONObject descObj = list.getJSONObject(0).getJSONObject("operate");
        if (descObj != null) {
            return descObj.getString("公司简介");
        }

        return null;
    }


}

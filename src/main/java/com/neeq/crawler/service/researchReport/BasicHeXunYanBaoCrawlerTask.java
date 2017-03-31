package com.neeq.crawler.service.researchReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 和讯研报
 * Created by znb on 16/6/23.
 */
public abstract class BasicHeXunYanBaoCrawlerTask extends BasicClientCrawlerTask {
    Logger log;
    private int page;
    private int totalPage;
    Record record = new Record();

    final String encoding = "gb2312";
    final String baseUrl = "http://yanbao.stock.hexun.com/";
    final String loginUrl = "https://reg.hexun.com/rest/ajaxlogin.aspx?callback=jQuery1820690605754731223_1467180994492&username=qqvtqen8pd&password=hexun123&gourl=http%253A%2F%2Fyanbao.stock.hexun.com%2F&fromhost=yanbao.stock.hexun.com&hiddenReferrer=http%253A%2F%2Fyanbao.stock.hexun.com%2F&loginstate=1&act=login&_="; //https

    void login() {
        long time = new Date().getTime();
        HttpPost post = HttpManager.getPost(loginUrl + time);
        String page = getForStringPage(post, 0);
        HttpGet get = HttpManager.getGet("http://utrack.hexun.com/UserTrack.aspx?time=" + time);
        String page1 = getForStringPage(get, 0);
        System.out.println();
    }


    @Override
    public void next() {

        try {
            if (page == 0) {
                login();
                page = 1;
                totalPage = getTotalPage();
            }
            if (page < totalPage) {
                crawPage();
            } else {
                willStop = true;
                page = 0;
            }

        } catch (Exception e) {
            log.error(taskId() + "信息抓取失败", e);
        }
    }

    private void crawPage() {
        if (record.trs == null || record.index >= record.trs.size()) {
            HttpGet get = HttpManager.getGet(getUrl());
            Document docPage = getForDocPage(get, 0, encoding);
            Elements trs = docPage.select("table.tab_cont tbody tr");
            trs.remove(0);//去掉标题行
            initRecord(trs);
        }
        Element tr = record.trs.remove(record.index);
        JSONObject result = new JSONObject();
        result.put("报告标题", tr.child(0).text());
        result.put("报告来源", tr.child(1).text());
        result.put("报告作者", tr.child(2).children().stream().map(Element::text).collect(Collectors.toCollection(JSONArray::new)));
        result.put("投资评级", tr.child(3).text());
        result.put("研报日期", tr.child(4).text());
        String qianming = result.getString("报告标题") + "_" + result.getString("报告作者") + "_" + result.getString("研报日期");
        String md5 = Md5Helper.getMd5(qianming);
        if (!RedisHelper.existInSet(redis, getRedisKey(), md5)) {

            String href = tr.child(5).select("a").attr("href");
            HttpGet infoGet = HttpManager.getGet(baseUrl + href);
            Document infoDocPage = getForDocPage(infoGet, 0, encoding);
            String text = infoDocPage.select("p.txt_02").text();
            result.put("摘要", text);

            String pdfUrl = infoDocPage.select("a.check-pdf").attr("href");
//            String page = getForStringPage(new HttpGet(pdfUrl), 0);
            HttpGet pdfGet = HttpManager.getGet(pdfUrl);
            pdfGet.setHeader("Referer", href);
            InputStream inputStream = getFileStream(pdfGet, 0);
            String localUrl = fileUploader.upload(inputStream, md5 + ".pdf", 0);
            result.put("pdf文件本地地址", localUrl);
            sendMessage(getKafkaTopic(), result, getRedisKey(), md5);
            System.out.println(result.get("pdf文件本地地址"));
            if (log.isInfoEnabled()) {
                log.info(taskId() + "{}", result.toJSONString());
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(taskId() + "{} is exist", qianming);
            }
        }
    }


    void initRecord(Elements trs) {
        record.trs = trs;
        record.index = 0;
        page++;
    }

    private String getUrl() {
        return getBaseUrl().replace("_1", "_" + page);
    }

    protected int getTotalPage() {
        HttpGet get = HttpManager.getGet(getUrl());
        Document docPage = getForDocPage(get, 0, encoding);
        return Integer.valueOf(docPage.select(".more + li").text().trim());
    }


    protected abstract String getBaseUrl();

    protected abstract String getRedisKey();

    protected abstract String getKafkaTopic();

    private class Record {
        protected Elements trs;
        protected int index;
    }
}



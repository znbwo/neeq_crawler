package com.neeq.crawler.service.guzhuan;

import com.alibaba.fastjson.JSONArray;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.service.BasicClientCrawlerTask;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by bj on 16/7/19.
 */
public abstract class BasicNeeqCrawlerTask extends BasicClientCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(BasicNeeqCrawlerTask.class);
    static String url = "http://www.neeq.com.cn";
    static String queryurl = "";

    public BasicNeeqCrawlerTask(PushQueue pushQueue, CoopRedis redis, FileUploader fileUploader) {
        super(redis, pushQueue, fileUploader);
    }

    @Override
    public void next() {
        run();

    }

    @Override
    public String taskId() {
        return null;
    }

    public abstract void run();


    public abstract String getResponseJsonStr() throws IOException;

    String getPDFContent(String localPath) throws IOException {
        PDDocument pdDocument = null;
        PDFTextStripper stripper = null;
        try {
            pdDocument = PDDocument.load(new FileInputStream(localPath));
            stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(pdDocument);
        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (pdDocument != null) {
                pdDocument.close();
            }
        }
        return null;
    }


    public HttpGet getRulesHttpGet(String url) throws UnsupportedEncodingException {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);
        return get;
    }

    private class JsonData {
        private Boolean lastPage;
        private JSONArray contentsJson;

        public Boolean getLastPage() {
            return lastPage;
        }

        public JSONArray getContentsJson() {
            return contentsJson;
        }

    }


}

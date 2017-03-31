package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.dependence.CoopRedis;
import com.neeq.crawler.dependence.Md5Helper;
import com.neeq.crawler.io.FileUploader;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import com.neeq.crawler.tool.RedisHelper;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 中国证券业协会信息抓取
 * Created by znb on 16/6/23.
 */
public class ZhongZhengXieCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(ZhongZhengXieCrawlerTask.class);

    private PushQueue pushQueue;
    private FileUploader fileUploader;
    private CoopRedis redis;


    private int index = 0;
    private URLCodec urlCodec;
    private List<Entity> pdfUrls;


    final String headUrl = "http://jg.sac.net.cn/pages/publicity/";


    public ZhongZhengXieCrawlerTask(PushQueue pushQueue, FileUploader fileUploader, CoopRedis redis) {
        this.pushQueue = pushQueue;
        this.fileUploader = fileUploader;
        this.redis = redis;
        this.urlCodec = new URLCodec();
    }


    @Override
    public void next() {
        try {
            if (pdfUrls == null) {
                initPdfUrls();//抓取年报名称及文档地址
            } else {
                if (index < pdfUrls.size()) {
                    try {
                        Entity entity = pdfUrls.get(index);
                        downFile(entity.report_year, entity.cpName, entity.pdfUrl);//下载文件,存储消息
                    } catch (Exception e) {
                        log.error("抓取{}失败,index={},pdfUrls.size={}", taskId(), index, pdfUrls.size(), e);

                    }
                    index++;
                } else {
                    willStop = true;
                    //本次抓取结束
                }

            }
        } catch (Exception e) {
            log.error("证券公司年报信息抓取失败", e);
        }


    }

    /**
     * 将json串 存 pdfUrls
     */
    private void initPdfUrls() throws UnsupportedEncodingException, EncoderException {
        pdfUrls = new ArrayList<>();

        HttpPost post = new HttpPost(headUrl + "resource!search.action");
        HttpManager.config(post);
        List<NameValuePair> params = new ArrayList<NameValuePair>() {
            {
                this.add(new BasicNameValuePair("sqlkey", "publicity"));
                this.add(new BasicNameValuePair("sqlval", "GET_ALL_DQBG_REPROT_YEAR"));
            }
        };
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        String page = getForStringPage(post, 0);
        JSONArray yearArray = changeToJSONArray(page);
        for (int i = 0; i < yearArray.size(); i++) {
            String report_year = yearArray.getJSONObject(i).getString("REPORT_YEAR");
            params.clear();
            params.add(new BasicNameValuePair("sqlkey", "publicity"));
            params.add(new BasicNameValuePair("sqlval", "GET_DQBG_BY_REPORT_YEAR"));
            params.add(new BasicNameValuePair("filter_LIKES_mdi_report_date", report_year));
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            page = getForStringPage(post, 0);
            JSONArray array = changeToJSONArray(page);
            for (int j = 0; j < array.size(); j++) {
                JSONObject json = array.getJSONObject(j);
                String cpName = json.getString("AOI_NAME");//爱建证券有限责任公司

                String mdai_filepath = json.getString("MDAI_FILEPATH");//2016-04-27/dqbg/201604271315380.pdf
                String mdai_filename = json.getString("MDAI_FILENAME");//爱建证券2015年度财务报告披露.pdf
                //http://jg.sac.net.cn/pages/publicity
                // /train-line-register!writeFile.action?inputPath=2016-04-27/dqbg/201604271315380.pdf
                // &fileName=%25E7%2588%25B1%25E5%25BB%25BA%25E8%25AF%2581%25E5%2588%25B82015%25E5%25B9%25B4%25E5%25BA%25A6%25E8%25B4%25A2%25E5%258A%25A1%25E6%258A%25A5%25E5%2591%258A%25E6%258A%25AB%25E9%259C%25B2.pdf
                String pdfUrl = headUrl + "train-line-register!writeFile.action?inputPath=" + mdai_filepath + "&fileName=" + urlCodec.encode(mdai_filename);
                pdfUrls.add(new Entity(report_year, cpName, pdfUrl));
            }
        }
        if (log.isDebugEnabled()) {
            if (pdfUrls.size() > 0) {
                log.debug(taskId() + ":{}", "信息初始化完成");
            } else {
                log.debug(taskId() + ":{}", "信息初始化失败");
            }

        }
    }

    private void downFile(String report_year, String cpName, String pdfUrl) {
        String md5 = Md5Helper.getMd5(report_year + cpName);
        if (RedisHelper.existInSet(redis, Constant.Redis.COMPANY_REPORT_CHECK_QUEUE, md5)) {
            if (log.isDebugEnabled()) {
                log.debug("report {} is exists", cpName + report_year);
            }
            return;
        }
        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            InputStream is = getFileStream(pdfUrl, 0);
            pdfUrl = fileUploader.upload(is, md5 + ".pdf", 0);  //公司logo
        }
        JSONObject result = new JSONObject();
        result.fluentPut("report_year", Integer.valueOf(report_year) - 1)//年报所描述年份
                .fluentPut("cpName", cpName)
                .fluentPut("pdfUrl", pdfUrl);

        redis.insertToSet(Constant.Redis.COMPANY_REPORT_CHECK_QUEUE, md5);
        pushQueue.sendToQueue(Constant.Topic.COMPANY_REPORT_TOPIC, result.toJSONString().getBytes());
        if (log.isDebugEnabled()) {
            log.debug("证券公司年报:{}", result.toJSONString());
        }
    }


    private JSONArray changeToJSONArray(String page) {
//        jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1,jsonStr.lastIndexOf(")"));
        JSONArray result = JSONArray.parseArray(page);
        return result;
    }

    @Override
    public String toString() {
        return taskId();
    }

    @Override
    public String taskId() {
        return "中国证券业协会年报信息";
    }


    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 6).setTimeUnit(TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean repeat() {
        return true;
    }


    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 12;
    }

    /**
     * 内部类
     */
    private class Entity {
        private String report_year;
        private String cpName;
        private String pdfUrl;

        public Entity(String report_year, String cpName, String pdfUrl) {
            this.report_year = report_year;
            this.cpName = cpName;
            this.pdfUrl = pdfUrl;
        }
    }


}

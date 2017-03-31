package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.pool.ThreadPool;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * 巨潮上市公司抓取
 * Created by kidbei on 16/7/6.
 */
public class CnInfoPublicCompanyCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(CnInfoPublicCompanyCrawlerTask.class);
    private final String LIST_URL = "http://www.cninfo.com.cn/cninfo-new/information/companylist";
    private final String INFO_URL = "http://www.cninfo.com.cn/information/";


    private PushQueue pushQueue;

    private boolean firstRun = true;
    private List<Company> companies = new ArrayList<>();
    private int companyIdx = 0;

    @Override
    public void next() {
        if (firstRun) {
            getALlCompanies();
            firstRun = false;
        }
        if (companyIdx  == companies.size()) {
            firstRun = false;
            willStop = true;
            companies.clear();
            companyIdx = 0;
            return;
        }
        Company company = companies.get(companyIdx);

        fetchCompanyInfo(company);

        companyIdx += 1;
    }



    private void getALlCompanies() {

        try{

            HttpGet get = new HttpGet(LIST_URL);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");

            Elements lis = new Elements();
            lis.addAll(doc.getElementById("con-a-1").select(".company-list li"));
            lis.addAll(doc.getElementById("con-a-2").select(".company-list li"));
            lis.addAll(doc.getElementById("con-a-3").select(".company-list li"));
            lis.addAll(doc.getElementById("con-a-4").select(".company-list li"));

            for (Element li : lis) {
                String url = li.child(0).attr("href");
                String text = li.child(0).text();
                String code = text.split(" ")[0];
                String name = text.split(" ")[1];

                companies.add(new Company(code,name,url));
            }

        } catch(Exception e) {
            log.error("抓取公司列表失败",e);
            firstRun = true;
        }
    }


    private void fetchCompanyInfo(Company company) {
        JSONObject result = new JSONObject();

        CompletableFuture<JSONObject> f1 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONObject r1 = 获取公司概况(company.code);
            f1.complete(r1);
        });

        CompletableFuture<JSONObject> f2 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONObject r2 = 获取发行筹资(company.code);
            f2.complete(r2);
        });

        CompletableFuture<JSONArray> f3 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONArray r3 = 获取高管人员(company.code);
            f3.complete(r3);
        });

        CompletableFuture<JSONArray> f4 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONArray r4 = 获取分红(company.code);
            f4.complete(r4);
        });

        CompletableFuture<JSONArray> f5 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONArray r5 = 获取配股(company.code);
            f5.complete(r5);
        });

        CompletableFuture<JSONObject> f6 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONObject r6 = 获取十大股东(company.code);
            f6.complete(r6);
        });

        CompletableFuture<JSONObject> f7 = new CompletableFuture<>();
        ThreadPool.getPool().execute(()->{
            JSONObject r7 = 获取流通股东(company.code);
            f7.complete(r7);
        });

        result.put("公司概况",safeGet(f1));
        result.put("发行筹资",safeGet(f2));
        result.put("高管",safeGet(f3));
        result.put("分红列表",safeGet(f4));
        result.put("配股列表",safeGet(f5));
        result.put("十大股东",safeGet(f6));
        result.put("流通股东",safeGet(f7));

        System.out.println(result);
    }


    private <T> T safeGet(CompletableFuture<T> future) {
        try{
            return future.get();
        } catch(Exception e) {
            return null;
        }
    }




    //抓取公司概况
    private JSONObject 获取公司概况(String code) {
        String url = INFO_URL + "brief/szmb" + code + ".html";
        try{

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");

            Elements trs = doc.select("body .clear2 table tbody tr");
            JSONObject result = new JSONObject()
                    .fluentPut("公司全称",trs.get(0).child(1).text())
                    .fluentPut("英文名称",trs.get(1).child(1).text())
                    .fluentPut("注册地址",trs.get(2).child(1).text())
                    .fluentPut("公司简称",trs.get(3).child(1).text())
                    .fluentPut("法定代表人",trs.get(4).child(1).text())
                    .fluentPut("公司董秘",trs.get(5).child(1).text())
                    .fluentPut("注册资本(万元)",trs.get(6).child(1).text())
                    .fluentPut("行业种类",trs.get(7).child(1).text())
                    .fluentPut("邮政编码",trs.get(8).child(1).text())
                    .fluentPut("公司电话",trs.get(9).child(1).text())
                    .fluentPut("公司传真",trs.get(10).child(1).text())
                    .fluentPut("公司网址",trs.get(11).child(1).text())
                    .fluentPut("上市时间",trs.get(12).child(1).text())
                    .fluentPut("招股时间",trs.get(13).child(1).text())
                    .fluentPut("发行数量(万股)",trs.get(14).child(1).text())
                    .fluentPut("发行价格(元)",trs.get(15).child(1).text())
                    .fluentPut("发行市盈率(倍)",trs.get(16).child(1).text())
                    .fluentPut("发行方式",trs.get(17).child(1).text())
                    .fluentPut("主承销商",trs.get(18).child(1).text())
                    .fluentPut("上市推荐人",trs.get(19).child(1).text())
                    .fluentPut("保荐机构",trs.get(20).child(1).text());

            return result;

        } catch(Exception e) {
            return new JSONObject();
        }
    }



    private JSONObject 获取发行筹资(String code) {
        String url = INFO_URL + "issue/szmb" + code + ".html";
        try{

            JSONObject result = new JSONObject();

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");

            Elements trs = doc.select("body .clear2 table tbody tr");

            result
                    .fluentPut("发行类型",trs.get(0).child(1).text())
                    .fluentPut("发行起始日",trs.get(1).child(1).text())
                    .fluentPut("发行性质",trs.get(2).child(1).text())
                    .fluentPut("发行股票种类",trs.get(3).child(1).text())
                    .fluentPut("发行方式",trs.get(4).child(1).text())
                    .fluentPut("发行公众股数量(万股)",trs.get(5).child(1).text())
                    .fluentPut("人民币发行价格(元)",trs.get(6).child(1).text())
                    .fluentPut("外币发行价格(元)",trs.get(7).child(1).text())
                    .fluentPut("实际募集资金(万元)",trs.get(8).child(1).text())
                    .fluentPut("实际发行费用(万元)",trs.get(9).child(1).text())
                    .fluentPut("发行市盈率(倍)",trs.get(10).child(1).text())
                    .fluentPut("上网定价中签率(%)",trs.get(11).child(1).text())
                    .fluentPut("二级配售中签率(%)",trs.get(12).child(1).text());

            return result;

        } catch(Exception e) {
            return new JSONObject();
        }
    }



    private JSONArray 获取高管人员(String code) {

        String url = INFO_URL + "management/szmb" + code +".html";
        try{

            JSONArray result = new JSONArray();

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");
            Elements trs = doc.select("body .clear2 table tbody tr");
            if (trs.size() == 1) return result;

            for (int i = 1 ; i <  trs.size(); i ++) {
                Element tr = trs.get(i);

                JSONObject item = new JSONObject();
                item.put("姓名",tr.child(0).text());
                item.put("职务",tr.child(1).text());
                item.put("出生年份",tr.child(2).text());
                item.put("性别",tr.child(3).text());
                item.put("学历",tr.child(4).text());

                result.add(item);
            }


            return result;

        } catch(Exception e) {
            return new JSONArray();
        }
    }



    private JSONArray 获取分红(String code) {

        String url = INFO_URL + "dividend/szmb" + code + ".html";
        JSONArray result = new JSONArray();
        try{

            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");
            Elements trs = doc.select("body .clear2 table tbody tr");

            if (trs.size() == 1) return result;

            for (int i = 1 ; i <  trs.size(); i ++) {
                JSONObject item = new JSONObject();
                item
                        .fluentPut("分红年度",trs.get(i).child(0).text())
                        .fluentPut("分红方案",trs.get(i).child(1).text())
                        .fluentPut("股权登记日",trs.get(i).child(2).text())
                        .fluentPut("除权基准日",trs.get(i).child(3).text())
                        .fluentPut("红股上市日",trs.get(i).child(4).text());

                result.add(item);
            }

            return result;
        } catch(Exception e) {
            return new JSONArray();
        }

    }



    private JSONArray 获取配股(String code) {

        String url = INFO_URL + "allotment/szmb" + code + ".html";

        JSONArray result = new JSONArray();
        try{
            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");
            Elements trs = doc.select("body .clear2 table tbody tr");

            if (trs.size() == 1) return result;
            for (int i = 1 ; i <  trs.size(); i ++) {
                JSONObject item = new JSONObject();
                item
                        .fluentPut("配股年度",trs.get(i).child(0).text())
                        .fluentPut("配股方案",trs.get(i).child(1).text())
                        .fluentPut("配股价",trs.get(i).child(2).text())
                        .fluentPut("股权登记日",trs.get(i).child(3).text())
                        .fluentPut("除权基准日",trs.get(i).child(4).text())
                        .fluentPut("配股交款起止日",trs.get(i).child(5).text())
                        .fluentPut("配股可流通部分上市日",trs.get(i).child(6).text());

                result.add(item);
            }

            return result;
        } catch(Exception e) {
            return new JSONArray();
        }

    }



    private JSONObject 获取股本结构(String code) {

        JSONObject result = new JSONObject();
        try{


            return result;
        } catch(Exception e) {
            return new JSONObject();
        }
    }



    private JSONObject 获取十大股东(String code) {

        String url = INFO_URL + "shareholders/" + code + ".html?www.cninfo.com.cn";

        return 获取股东数据(url);
    }



    private JSONObject 获取流通股东(String code) {
        String url = INFO_URL + "circulateshareholders/" + code + ".html";

        return 获取股东数据(url);
    }



    private JSONObject 获取股东数据(String url) {
        JSONObject result = new JSONObject();
        try{
            HttpGet get = new HttpGet(url);
            HttpManager.config(get);

            Document doc = getForDocPage(get,0,"gb2312");
            Elements trs = doc.select("body .clear2 table tbody tr");

            if (trs.size() == 1) return result;


            JSONArray list = null;
            for (int i = 1 ; i < trs.size() ; i ++) {
                if (trs.get(i).child(0).hasClass("zx_data")) {
                    list = new JSONArray();
                    result.put(trs.get(i).child(0).text(),list);
                    trs.get(i).children().remove(0);
                }

                JSONObject item = new JSONObject();
                item.put("股东名称",trs.get(i).child(0).text());
                item.put("持股数量(股)",trs.get(i).child(1).text());
                item.put("持股比例(%)",trs.get(i).child(2).text());
                item.put("股份性质",trs.get(i).child(3).text());
                list.add(item);
            }

            return result;
        } catch(Exception e) {
            return new JSONObject();
        }
    }


    class Company{
        public String code;
        public String name;
        public String url;

        public Company(String code, String name, String url) {
            this.code = code;
            this.name = name;
            this.url = url;
        }
    }




    @Override
    public String taskId() {
        return "巨潮上市公司";
    }


    @Override
    public boolean repeat() {
        return true;
    }

    @Override
    public long repeatAfterTime() {
        return 1000 * 60 * 60 * 24;
    }

    @Override
    public TaskOptions options() {
        return new TaskOptions().setPeriod(1000 * 20).setTimeUnit(TimeUnit.MILLISECONDS);
    }
}

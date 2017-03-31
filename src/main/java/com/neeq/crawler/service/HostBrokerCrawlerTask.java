package com.neeq.crawler.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 *
 * 主办券商信息抓取
 * Created by kidbei on 16/5/24.
 */
public class HostBrokerCrawlerTask extends BasicClientCrawlerTask {
    private final Logger log = LoggerFactory.getLogger(HostBrokerCrawlerTask.class);

    private final String indexUrl = "http://www.neeq.com.cn/agency/securities.html";
    private final String BASE_URL = "http://bpmweb.neeq.org.cn";


    //所有列表URL
    private final ArrayList<String> listUrls = new ArrayList<>();


    private PushQueue pushQueue;


    public HostBrokerCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }


    @Override
    public void next() {
        listUrls.clear();
        fetchAllListUrls();
        fetchListItems();

        willStop = true;
    }



    //抓取所有列表链接
    private void fetchAllListUrls() {

        listUrls.add(indexUrl);

        HttpGet get = new HttpGet(indexUrl);
        HttpManager.config(get);

        String html = getForStringPage(get,0);
        Document doc = Jsoup.parse(html);

        Elements as = doc.select("#cmsPage a");
        for (Element a : as) {
            String cll = a.attr("class");
            if (cll.equals("") && !a.attr("href").equals("")) {
                String url = a.attr("href");
                if (url.startsWith("//")) {
                    url = "http:" + url;
                }
                listUrls.add(url);
            }
        }

    }



    //抓取列表
    private void fetchListItems() {
        for (String listUrl : listUrls) {
            HttpGet get = new HttpGet(listUrl);
            HttpManager.config(get);

            String html = getForStringPage(get,0);
            Document doc = Jsoup.parse(html);
            Elements lis = doc.select(".modlist .orglist .fix li");
            for (Element li : lis) {
                String href = li.getElementsByTag("a").first().attr("href");
                if (href.startsWith("//")) href = "http:" + href;
                if (log.isDebugEnabled()) {
                    log.debug("fetch company : {}",href);
                }
                fetchCompanyInfo(href);
            }
        }
    }



    //公司信息
    private void fetchCompanyInfo(String url) {
        JSONObject result = new JSONObject();
        try{
            fetchCompanyBasicInfo(result,url);
        } catch(Exception e) {
            log.error("fetch company error,url = {}",url,e);
        }

        if (log.isDebugEnabled()) {
            log.debug("fetch company info : \n{}",result.toJSONString());
        }

        pushQueue.sendToQueue(Constant.Topic.ZBQS_INFO_TOPIC,result.toJSONString().getBytes());
    }



    //抓取公司概况
    private void fetchCompanyBasicInfo(JSONObject result,String url) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);
        Document doc = getForDocPage(get,0);

        Element table = doc.select(".box_right table.border1").first();
        Elements trs = table.select("tbody tr");

        //公司名称
        String companyName = trs.get(1).children().get(2).text().replace("\u00A0","").trim();
        //成立日期
        String cts = trs.get(2).child(1).text().replace("\u00A0","").trim();
        //法人代表
        String fr = trs.get(2).child(3).text().replace("\u00A0","").trim();
        //总经理
        String zjl = trs.get(2).child(5).text().replace("\u00A0","").trim();
        //注册资本
        String zczb = trs.get(3).child(1).text().replace("\u00A0","").trim();
        //净资产
        String jzc = trs.get(3).child(3).text().replace("\u00A0","").trim();
        //净资本
        String jzb = trs.get(3).child(5).text().replace("\u00A0","").trim();
        //注册地址
        String zcdz = trs.get(4).child(1).text().replace("\u00A0","").trim();
        //营业部家数
        String yybjs = trs.get(4).child(3).text().replace("\u00A0","").trim();
        //办公地址
        String bgdz = trs.get(5).child(1).text().replace("\u00A0","").trim();
        //邮编
        String yb = trs.get(5).child(3).text().replace("\u00A0","").trim();
        //公司网址
        String gswz = trs.get(6).child(1).getElementsByTag("a").text().replace("\u00A0","").trim();
        //电子邮箱
        String email = trs.get(6).child(3).text().replace("\u00A0","").trim();
        //经营证券业务许可证编码
        String xkzbm = trs.get(6).child(5).text().replace("\u00A0","").trim();
        //证监会批准从事的证券业务
        String zqyw = trs.get(8).child(1).text().replace("\u00A0","").trim();
        //在全国股份转让系统从事的业务种类
        String ywzl = trs.get(9).child(1).text().replace("\u00A0","").trim();
        //公司概况
        String desc = trs.get(10).child(1).text().replace("\u00A0","").trim();

        result.fluentPut("companyName",companyName).fluentPut("cts",cts)
                .fluentPut("fr",fr).fluentPut("zjl",zjl).fluentPut("zczb",zczb)
                .fluentPut("jzc",jzc).fluentPut("jzb",jzb).fluentPut("zcdz",zcdz)
                .fluentPut("yybjs",yybjs).fluentPut("bgdz",bgdz).fluentPut("yb",yb)
                .fluentPut("gswz",gswz).fluentPut("email",email)
                .fluentPut("xkzbm",xkzbm).fluentPut("zqyw",zqyw).fluentPut("ywzl",ywzl)
                .fluentPut("desc",desc);

        Elements tds = doc.select("#secTable tbody tr td");
        try{
            String dynamicLogicUrl = BASE_URL + tds.get(1).child(0).attr("href");
            fetchDynamicLogic(result,dynamicLogicUrl);
        } catch(Exception e) {
            log.error("fetch dynamic logic error",e);
        }
    }





    //抓取业务动态
    private void fetchDynamicLogic(JSONObject result,String url) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);
        Document doc = getForDocPage(get,0);

        Elements tables = doc.select(".box_right table.border1");
        //推荐挂牌情况
        Element tb1 = tables.get(0);
        Elements trs = tb1.select("tr");
        //已被终止挂牌公司家数
        String zzgp_num = trs.get(trs.size() - 1).child(1).text().replace("\u00A0","").trim();
        //已上市公司家数
        String ss_num = trs.get(trs.size() - 2).child(1).text().replace("\u00A0","").trim();
        //正在挂牌公司家数
        String gping_num = trs.get(trs.size() - 3).child(1).text().replace("\u00A0","").trim();
        //撤回材料及申请被否公司家数
        String chsq_num = trs.get(trs.size() - 4).child(1).text().replace("\u00A0","").trim();
        //目前已推荐挂牌公司家数
        String tjgs_num = trs.get(trs.size() - 5).child(1).text().replace("\u00A0","").trim();


        JSONArray tj = new JSONArray();
        if (trs.size() == 7) {//没有推荐的公司

        } else {
            for (int i = 2 ; i <  trs.size() - 5; i ++) {
                //股份代码
                String code = trs.get(i).child(1).text().replace("\u00A0","").trim();
                //公司名称
                String cpName = trs.get(i).child(2).text().replace("\u00A0","").trim();
                //挂牌日期
                String gprq = trs.get(i).child(3).text().replace("\u00A0","").trim();
                //公司状态
                String gszt = trs.get(i).child(4).text().replace("\u00A0","").trim();

                JSONObject t = new JSONObject()
                        .fluentPut("code",code)
                        .fluentPut("cpName",cpName)
                        .fluentPut("gprq",gprq)
                        .fluentPut("gszt",gszt);
                tj.add(t);
            }
        }

        JSONObject tjgp_info = new JSONObject()
                .fluentPut("zzgp_num",zzgp_num)
                .fluentPut("ss_num",ss_num)
                .fluentPut("gping_num",gping_num)
                .fluentPut("chsq_num",chsq_num)
                .fluentPut("tjgs_num",tjgs_num)
                .fluentPut("tjs",tj);

        result.put("tjgp_info",tjgp_info);


        //推荐定向发行情况

        tb1 = tables.get(1);
        trs = tb1.select("tr");

        //推荐定向发行失败次数
        String tjdxsb = trs.get(trs.size() - 1).child(1).text();
        //推荐定向发行成功次数
        String tjdxcg = trs.get(trs.size() - 2).child(1).text();
        //推荐定向发行次数
        String tjdxcs = trs.get(trs.size() - 3).child(1).text();

        JSONArray tjfxgs = new JSONArray();
        if (trs.size() == 5) {

        } else {
            for (int i = 2 ; i <  trs.size() - 3; i ++) {
                //股份代码
                String code = trs.get(i).child(1).text().replace("\u00A0","").trim();
                //公司名称
                String cpName = trs.get(i).child(2).text().replace("\u00A0","").trim();
                //发行日期
                String fxrq = trs.get(i).child(3).text().replace("\u00A0","").trim();
                //公司状态
                String gszt = trs.get(i).child(4).text().replace("\u00A0","").trim();

                JSONObject txgs = new JSONObject()
                        .fluentPut("code",code)
                        .fluentPut("cpName",cpName)
                        .fluentPut("fxrq",fxrq)
                        .fluentPut("gszt",gszt);
                tjfxgs.add(txgs);
            }
        }

        JSONObject tjdxgs = new JSONObject()
                .fluentPut("tjdxsb",tjdxsb)
                .fluentPut("tjdxcg",tjdxcg)
                .fluentPut("tjdxcs",tjdxcs)
                .fluentPut("tjfxgs",tjfxgs);
        result.put("tjdxgs",tjdxgs);

        //推荐原代办股份转让系统的两网公司及退市公司挂牌情况
        //// TODO: 16/5/24
    }




    @Override
    public String taskId() {
        return "主办券商信息";
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
        return new TaskOptions().setPeriod(1000 * 10).setTimeUnit(TimeUnit.MILLISECONDS);
    }

}

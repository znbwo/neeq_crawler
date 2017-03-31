package com.neeq.crawler.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.Constant;
import com.neeq.crawler.push.PushQueue;
import com.neeq.crawler.task.TaskOptions;
import com.neeq.crawler.tool.HttpManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 私募基金管理人综合查询
 * http://gs.amac.org.cn/amac-infodisc/res/pof/manager/index.html
 * Created by kidbei on 16/6/3.
 */
public class PrivateFundManagerPersonListCrawlerTask extends BasicClientCrawlerTask {

    private final Logger log = LoggerFactory.getLogger(PrivateFundManagerPersonListCrawlerTask.class);

    private final String BASE_URL = "http://gs.amac.org.cn/amac-infodisc/api/pof/manager?";

    private final String url_pre = "http://gs.amac.org.cn/amac-infodisc/res/pof/manager/";
    private final String url_after = ".html";

    private int page = 0;
    private int totalPage = -1;
    private boolean fullCrawed = false;


    private PushQueue pushQueue;


    public PrivateFundManagerPersonListCrawlerTask(PushQueue pushQueue) {
        this.pushQueue = pushQueue;
    }



    @Override
    public void next() {
        try{

            String url = BASE_URL + "rand=0." + System.currentTimeMillis() + "&page=" + page + "&size=100";
            HttpPost post = new HttpPost(url);


            JSONObject param = new JSONObject()
                    .fluentPut("rand","0." + System.currentTimeMillis())
                    .fluentPut("page",page+"")
                    .fluentPut("size","100");

            StringEntity entity = new StringEntity(param.toJSONString(), Charset.forName("UTF-8"));
            entity.setContentType("application/json");
            post.setEntity(entity);

            HttpManager.config(post);

            if (log.isDebugEnabled()) {
                log.debug("抓取url:{}",url);
            }

            String jsonStr = getForStringPage(post,0);
            if (jsonStr == null) {
                log.warn("request failed for url:{}",url);
                return;
            }

            JSONObject result = JSON.parseObject(jsonStr);

            if (totalPage == -1) {
                totalPage = result.getInteger("totalPages");
            }

            if(page >= totalPage) {
                page = 0;
                totalPage = -1;
                fullCrawed = true;
                willStop = true;
            }


//            if(page == 0) {
//                page = 0;
//                totalPage = -1;
//                fullCrawed = true;
//                willStop = true;
//            }

            JSONArray list = result.getJSONArray("content");
            for (int i = 0 ; i < list.size() ; i ++) {
                try{
                    fetchItem(list, i);
                } catch(Exception e) {
                    log.error("抓取数据失败:{}",list.get(i),e);
                }
            }

        } catch(Exception e) {
            log.error("抓取私募基金管理人列表失败,page={}",page,e);
        }

//        page -= 1;
        page += 1;
    }

    private void fetchItem(JSONArray list, int i) {
        JSONObject cp = list.getJSONObject(i);
        String id = cp.getString("id");
        String itemUrl = url_pre + id + url_after;

        HttpGet get = new HttpGet(itemUrl);
        HttpManager.config(get);

        //详情页
        Document infoDoc = getForDocPage(get,0);

        Elements trs = infoDoc.select(".m-manager-list.m-list-details .table-info tbody").first().children();

        JSONObject info = new JSONObject();

        //基金管理人全称(中文)
        String cnName = infoDoc.getElementById("complaint1").text();
        //基金管理人全称(英文)
        String enName = getSafeText(trs,3,1);
        //登记编号
        String code = getSafeText(trs,4,1);
        //组织机构代码
        String orgCode = getSafeText(trs,5,1);
        //登记时间
        String reDate = getSafeText(trs,6,1);
        //注册地址
        String reAddress = getSafeText(trs,7,1);
        //办公地址
        String workAddress = getSafeText(trs,8,1);
        //注册资本(万元)
        String reMoney = getSafeText(trs,9,1);
        //实缴资本(万元)
        String realMoney = getSafeText(trs,9,3);
        //企业性质
        String cpType = getSafeText(trs,10,1);
        //注册资本实缴比例
        String zczbsjbl = getSafeText(trs,10,3);
        //管理基金主要类别
        String gljjlb = getSafeText(trs,11,1);
        //申请的其他业务类型
        String sqqtywlx = getSafeText(trs,11,3);
        //员工人数
        String memberNum = getSafeText(trs,12,1);
        //机构网址
        String website = getSafeText(trs,12,3);
        //法律意见书状态
        String flyjszt = getSafeText(trs,14,1);

        //法定代表人/执行事务合伙人(委派代表)姓名
        String ggname = getSafeText(trs,16,1);
        //是否有从业资格
        String sfycyzg = getSafeText(trs,17,1);
        //资格取得方式
        String zgqdfs = getSafeText(trs,17,3);

        info.fluentPut("cnName",cnName)
                .fluentPut("enName",enName)
                .fluentPut("code",code)
                .fluentPut("orgCode",orgCode)
                .fluentPut("reDate",reDate)
                .fluentPut("reAddress",reAddress)
                .fluentPut("workAddress",workAddress)
                .fluentPut("reMoney",reMoney)
                .fluentPut("realMoney",realMoney)
                .fluentPut("cpType",cpType)
                .fluentPut("zczbsjbl",zczbsjbl)
                .fluentPut("gljjlb",gljjlb)
                .fluentPut("sqqtywlx",sqqtywlx)
                .fluentPut("memberNum",memberNum)
                .fluentPut("website",website)
                .fluentPut("flyjszt",flyjszt)
                .fluentPut("ggname",ggname)
                .fluentPut("sfycyzg",sfycyzg)
                .fluentPut("zgqdfs",zgqdfs);


        //法定代表人/执行事务合伙人(委派代表)工作履历
        JSONArray hhrExps = new JSONArray();
        try{
            Elements llTrs = trs.get(18).child(1).child(0).child(2).children();
            if (llTrs != null && !llTrs.isEmpty()) {
                for (Element llTr : llTrs) {

                    String date = llTr.child(0).text();//时间
                    String hhrCompany = llTr.child(1).text();//任职单位
                    String hhrPost = llTr.child(2).text();//职务
                    JSONObject item = new JSONObject()
                            .fluentPut("date",date)
                            .fluentPut("hhrCompany",hhrCompany)
                            .fluentPut("hhrPost",hhrPost);
                    hhrExps.add(item);
                }
            }
        } catch(Exception e) {
        }
        info.put("hhrExps",hhrExps);

        //高管情况
        JSONArray ggInfos = new JSONArray();
        try{
            Elements ggInfoTrs = trs.get(19).child(1).child(0).child(2).children();
            if (ggInfoTrs != null && !ggInfoTrs.isEmpty()) {
                for (Element ggInfoTr : ggInfoTrs) {
                    JSONObject ggInfo = new JSONObject();

                    String ggName = ggInfoTr.child(0).text();//高管姓名
                    String ggPost = ggInfoTr.child(1).text();//职务
                    String hasCyzg = ggInfoTr.child(2).text();//是否具有基金从业资格

                    ggInfo.fluentPut("ggName",ggName)
                            .fluentPut("ggPost",ggPost)
                            .fluentPut("hasCyzg",hasCyzg);
                    ggInfos.add(ggInfo);
                }
                info.put("ggInfos",ggInfos);
            }

        } catch(Exception e) {
        }

        //暂行办法实施前成立的基金
        try{
            Elements zxbfqJJPs = trs.get(21).child(1).children();
            if (zxbfqJJPs != null && !zxbfqJJPs.isEmpty()) {
                JSONArray zxbfqJJs = new JSONArray();

                for (Element zxbfqJJP : zxbfqJJPs) {
                    String zbfqJJUrl = zxbfqJJP.child(0).attr("href");
                    if (zbfqJJUrl == null || zbfqJJUrl.indexOf("/") == -1) break;
                    zbfqJJUrl = zbfqJJUrl.substring(zbfqJJUrl.indexOf("/") , zbfqJJUrl.length());
                    zbfqJJUrl = "http://gs.amac.org.cn/amac-infodisc/res/pof" + zbfqJJUrl;

                    JSONObject zxbfqJJ = getJJInfo(zbfqJJUrl);
                    zxbfqJJs.add(zxbfqJJ);
                }

                info.put("zxbfqJJs",zxbfqJJs); //暂行办法实施前成立的基金
            }
        } catch(Exception e) {

        }

        //暂行办法实施后成立的基金
        try{
            Elements zxbfhJJPs = trs.get(22).child(1).children();
            if (zxbfhJJPs != null && !zxbfhJJPs.isEmpty()) {
                JSONArray zxbfhJJs = new JSONArray();

                for (Element zxbfqJJP : zxbfhJJPs) {
                    String zbfqJJUrl = zxbfqJJP.child(0).attr("href");
                    if (zbfqJJUrl == null || zbfqJJUrl.indexOf("/") == -1) break;
                    zbfqJJUrl = zbfqJJUrl.substring(zbfqJJUrl.indexOf("/") , zbfqJJUrl.length());
                    zbfqJJUrl = "http://gs.amac.org.cn/amac-infodisc/res/pof" + zbfqJJUrl;

                    JSONObject zxbfqJJ = getJJInfo(zbfqJJUrl);
                    zxbfhJJs.add(zxbfqJJ);
                }

                info.put("zxbfhJJs",zxbfhJJs); //暂行办法实施后成立的基金
            }
        } catch(Exception e) {

        }


        pushQueue.sendToQueue(Constant.Topic.SIMUJIJINGUANLIREN__TOPIC,info.toJSONString().getBytes());
        if (log.isDebugEnabled()) {
            log.debug("抓取到私募基金管理人数据:\n {}",info.toJSONString());
        }
    }


    /**
     * 防止TD缺失,单个字段不影响整条数据
     * @param trs
     * @param parent
     * @param child
     * @return
     */
    private String getSafeText(Elements trs,int parent,int child) {
        try{
            return trs.get(parent).child(child).text();
        } catch(Exception e) {
            return null;
        }
    }




    private JSONObject getJJInfo(String url) {
        HttpGet get = new HttpGet(url);
        HttpManager.config(get);

        Document doc = getForDocPage(get,0);

        try{
            Elements trs = doc.select(".m-manager-list.m-list-details .table-info tbody tr");
            if (trs != null && !trs.isEmpty()) {
                JSONObject obj = new JSONObject();
                //基金名称
                String jjName = trs.get(0).child(1).text();
                //基金编号
                String jjCode = trs.get(1).child(1).text();
                //成立时间
                String clsj = trs.get(2).child(1).text();
                //备案时间
                String basj = trs.get(3).child(1).text();
                //基金备案阶段
                String jjbajd = trs.get(4).child(1).text();
                //基金类型
                String jjlx = trs.get(5).child(1).text();
                //币种
                String bz = trs.get(6).child(1).text();
                //基金管理人名称
                String jjglrmc = trs.get(7).child(1).text();
                //管理类型
                String gllx = trs.get(8).child(1).text();
                //是否托管
                String sftg = trs.get(9).child(1).text();
                //托管人名称
                String tgrmc = trs.get(10).child(1).text();
                //主要投资领域
                String zytzly = trs.get(11).child(1).text();
                //运作状态
                String yzzt = trs.get(12).child(1).text();
                //基金信息最后报告时间
                String zhbgsj = trs.get(13).child(1).text();
                //基金协会特别提示（针对基金）
                String tbts = trs.get(14).child(1).text();
                obj.fluentPut("jjName",jjName)
                        .fluentPut("jjCode",jjCode)
                        .fluentPut("clsj",clsj)
                        .fluentPut("basj",basj)
                        .fluentPut("jjbajd",jjbajd)
                        .fluentPut("jjlx",jjlx)
                        .fluentPut("bz",bz)
                        .fluentPut("jjglrmc",jjglrmc)
                        .fluentPut("gllx",gllx)
                        .fluentPut("sftg",sftg)
                        .fluentPut("tgrmc",tgrmc)
                        .fluentPut("zytzly",zytzly)
                        .fluentPut("yzzt",yzzt)
                        .fluentPut("zhbgsj",zhbgsj)
                        .fluentPut("tbts",tbts);

                return obj;
            }
        } catch(Exception e) {

        }

        return null;
    }



    @Override
    public String taskId() {
        return "私募基金管理人综合查询";
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
